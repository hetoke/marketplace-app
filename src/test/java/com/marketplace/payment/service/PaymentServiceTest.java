package com.marketplace.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.payment.config.SePayProperties;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.RefundPaymentRequest;
import com.marketplace.payment.dto.SePayCheckoutResponse;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.model.Payment.PaymentStatus;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SePayService sePayService;

    @InjectMocks
    private PaymentService paymentService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().when(sePayService.buildSuccessUrl(anyString())).thenReturn("http://localhost:3000/orders/" + ORDER_ID + "?payment=success");
        lenient().when(sePayService.buildErrorUrl(anyString())).thenReturn("http://localhost:3000/orders/" + ORDER_ID + "?payment=error");
        lenient().when(sePayService.buildCancelUrl(anyString())).thenReturn("http://localhost:3000/orders/" + ORDER_ID + "?payment=cancelled");
        lenient().when(sePayService.createCheckoutFields(anyString(), any(BigDecimal.class), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new SePayCheckoutResponse("https://pay-sandbox.sepay.vn/v1/checkout/init", Map.of("merchant", "test", "signature", "abc")));
    }

    // ==================== INITIATE PAYMENT ====================

    @Test
    void initiatePayment_success() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });

        SePayCheckoutResponse response = paymentService.initiatePayment(USER_ID.toString(), ORDER_ID, "BANK_TRANSFER");

        assertThat(response.checkoutUrl()).isEqualTo("https://pay-sandbox.sepay.vn/v1/checkout/init");
        assertThat(response.formFields()).containsKey("signature");
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void initiatePayment_alreadyPaid_throwsBusinessException() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PAID);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID.toString(), ORDER_ID, "BANK_TRANSFER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been paid");
    }

    @Test
    void initiatePayment_orderNotFound_throwsResourceNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID.toString(), ORDER_ID, "BANK_TRANSFER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void initiatePayment_notOrderOwner_throwsAccessDenied() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment(UUID.randomUUID().toString(), ORDER_ID, "BANK_TRANSFER"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void initiatePayment_cancelledOrder_throwsBusinessException() {
        Order order = createOrder(OrderStatus.CANCELLED, Order.PaymentStatus.PENDING);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID.toString(), ORDER_ID, "BANK_TRANSFER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot process payment");
    }

    @Test
    void initiatePayment_refundRequested_throwsBusinessException() {
        Order order = createOrder(OrderStatus.RETURN_REQUESTED, Order.PaymentStatus.REFUND_REQUESTED);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID.toString(), ORDER_ID, "BANK_TRANSFER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending refund");
    }

    // ==================== IPN NOTIFICATION ====================

    @Test
    void handleIpnNotification_approvedPayment_marksOrderPaid() throws Exception {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);
        Payment payment = createPayment(PaymentStatus.PENDING);
        payment.setInvoiceNumber("ORD" + ORDER_ID.toString().replace("-", "").substring(0, 8).toUpperCase());

        String invoiceNumber = payment.getInvoiceNumber();
        String rawBody = "{\"timestamp\":1757058220,\"notification_type\":\"ORDER_PAID\"," +
                "\"order\":{\"id\":\"e2c195be\",\"order_id\":\"ORD-123\",\"order_status\":\"CAPTURED\"," +
                "\"order_currency\":\"VND\",\"order_amount\":\"100000.00\"," +
                "\"order_invoice_number\":\"" + invoiceNumber + "\"," +
                "\"custom_data\":[],\"user_agent\":\"Mozilla/5.0\",\"ip_address\":\"14.186.39.212\"," +
                "\"order_description\":\"Test payment\"}," +
                "\"transaction\":{\"id\":\"384c66dd\",\"payment_method\":\"BANK_TRANSFER\"," +
                "\"transaction_id\":\"68da43da2d9de\",\"transaction_type\":\"PAYMENT\"," +
                "\"transaction_date\":\"2025-09-29 15:31:22\",\"transaction_status\":\"APPROVED\"," +
                "\"transaction_amount\":\"100000\",\"transaction_currency\":\"VND\"," +
                "\"authentication_status\":\"AUTHENTICATION_SUCCESSFUL\"," +
                "\"card_number\":null,\"card_holder_name\":null,\"card_expiry\":null," +
                "\"card_funding_method\":null,\"card_brand\":null}," +
                "\"customer\":null,\"agreement\":null}";

        when(sePayService.verifyIpnSecretKey("test-secret")).thenReturn(true);
        when(paymentRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        paymentService.handleIpnNotification(rawBody, "test-secret");

        verify(paymentRepository).save(paymentCaptor.capture());
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(orderCaptor.getValue().getPaymentStatus()).isEqualTo(Order.PaymentStatus.PAID);
    }

    @Test
    void handleIpnNotification_invalidSecretKey_throwsBusinessException() {
        String rawBody = "{\"timestamp\":1757058220,\"notification_type\":\"ORDER_PAID\"}";

        when(sePayService.verifyIpnSecretKey("wrong-key")).thenReturn(false);

        assertThatThrownBy(() -> paymentService.handleIpnNotification(rawBody, "wrong-key"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid IPN secret key");
    }

    @Test
    void handleIpnNotification_alreadyCompleted_idempotent() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);
        payment.setInvoiceNumber("ORDTEST1234");

        String rawBody = "{\"timestamp\":1757058220,\"notification_type\":\"ORDER_PAID\"," +
                "\"order\":{\"id\":\"e2c195be\",\"order_id\":\"ORD-123\",\"order_status\":\"CAPTURED\"," +
                "\"order_currency\":\"VND\",\"order_amount\":\"100000.00\"," +
                "\"order_invoice_number\":\"ORDTEST1234\"," +
                "\"custom_data\":[],\"user_agent\":\"Mozilla/5.0\",\"ip_address\":\"14.186.39.212\"," +
                "\"order_description\":\"Test payment\"}," +
                "\"transaction\":{\"id\":\"384c66dd\",\"payment_method\":\"BANK_TRANSFER\"," +
                "\"transaction_id\":\"68da43da2d9de\",\"transaction_type\":\"PAYMENT\"," +
                "\"transaction_date\":\"2025-09-29 15:31:22\",\"transaction_status\":\"APPROVED\"," +
                "\"transaction_amount\":\"100000\",\"transaction_currency\":\"VND\"," +
                "\"authentication_status\":\"AUTHENTICATION_SUCCESSFUL\"," +
                "\"card_number\":null,\"card_holder_name\":null,\"card_expiry\":null," +
                "\"card_funding_method\":null,\"card_brand\":null}," +
                "\"customer\":null,\"agreement\":null}";

        when(sePayService.verifyIpnSecretKey("test-secret")).thenReturn(true);
        when(paymentRepository.findByInvoiceNumber("ORDTEST1234")).thenReturn(Optional.of(payment));

        paymentService.handleIpnNotification(rawBody, "test-secret");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // ==================== REQUEST REFUND ====================

    @Test
    void requestRefund_success() {
        Order order = createOrder(OrderStatus.RETURN_REQUESTED, Order.PaymentStatus.PAID);
        Payment payment = createPayment(PaymentStatus.COMPLETED);
        RefundPaymentRequest request = new RefundPaymentRequest("Changed my mind");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.requestRefund(USER_ID.toString(), PAYMENT_ID, request);

        assertThat(response.status()).isEqualTo("REFUND_REQUESTED");
        assertThat(response.refundedAt()).isNull();
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.REFUND_REQUESTED);
    }

    @Test
    void requestRefund_notCompletedPayment_throwsBusinessException() {
        Order order = createOrder(OrderStatus.RETURN_REQUESTED, Order.PaymentStatus.PENDING);
        Payment payment = createPayment(PaymentStatus.PENDING);
        RefundPaymentRequest request = new RefundPaymentRequest("Changed my mind");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.requestRefund(USER_ID.toString(), PAYMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only completed payments");
    }

    @Test
    void requestRefund_wrongOrderStatus_throwsBusinessException() {
        Order order = createOrder(OrderStatus.DELIVERED, Order.PaymentStatus.PAID);
        Payment payment = createPayment(PaymentStatus.COMPLETED);
        RefundPaymentRequest request = new RefundPaymentRequest("Changed my mind");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.requestRefund(USER_ID.toString(), PAYMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RETURN_REQUESTED, RETURNED, or CANCELLED");
    }

    @Test
    void requestRefund_notOrderOwner_throwsAccessDenied() {
        Order order = createOrder(OrderStatus.RETURN_REQUESTED, Order.PaymentStatus.PAID);
        Payment payment = createPayment(PaymentStatus.COMPLETED);
        RefundPaymentRequest request = new RefundPaymentRequest("Changed my mind");

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.requestRefund(UUID.randomUUID().toString(), PAYMENT_ID, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong");
    }

    // ==================== APPROVE REFUND ====================

    @Test
    void approveRefund_success() {
        Order order = createOrder(OrderStatus.RETURN_REQUESTED, Order.PaymentStatus.REFUND_REQUESTED);
        Payment payment = createPayment(PaymentStatus.REFUND_REQUESTED);

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.approveRefund(PAYMENT_ID);

        assertThat(response.status()).isEqualTo("REFUNDED");
        assertThat(response.refundedAt()).isNotNull();
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.REFUNDED);
    }

    @Test
    void approveRefund_notRefundRequested_throwsBusinessException() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.approveRefund(PAYMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending refund request");
    }

    @Test
    void approveRefund_paymentNotFound_throwsResourceNotFound() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.approveRefund(PAYMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment");
    }

    // ==================== HELPERS ====================

    private Order createOrder(OrderStatus status, Order.PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setBuyerId(USER_ID);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setCurrency("VND");
        return order;
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setOrderId(ORDER_ID);
        payment.setAmount(new BigDecimal("99.99"));
        payment.setCurrency("VND");
        payment.setMethod(Payment.PaymentMethod.SEPAY);
        payment.setStatus(status);
        return payment;
    }
}
