package com.marketplace.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.ProcessPaymentRequest;
import com.marketplace.payment.dto.RefundPaymentRequest;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.model.Payment.PaymentStatus;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();

    // ==================== PROCESS PAYMENT ====================

    @Test
    void processPayment_success() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(USER_ID.toString(), ORDER_ID, request);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.cardLastFour()).isEqualTo("4242");
        assertThat(response.cardBrand()).isEqualTo("VISA");
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PAID);
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void processPayment_declinedCard_fails() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4000000000000002", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });

        PaymentResponse response = paymentService.processPayment(USER_ID.toString(), ORDER_ID, request);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.failureReason()).isEqualTo("Insufficient funds");
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PENDING);
    }

    @Test
    void processPayment_alreadyPaid_throwsBusinessException() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PAID);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(USER_ID.toString(), ORDER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been paid");
    }

    @Test
    void processPayment_orderNotFound_throwsResourceNotFound() {
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(USER_ID.toString(), ORDER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void processPayment_notOrderOwner_throwsBusinessException() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(UUID.randomUUID().toString(), ORDER_ID, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void processPayment_cancelledOrder_throwsBusinessException() {
        Order order = createOrder(OrderStatus.CANCELLED, Order.PaymentStatus.PENDING);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(USER_ID.toString(), ORDER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot process payment");
    }

    @Test
    void processPayment_alreadyCompletedPayment_throwsBusinessException() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdAndStatus(ORDER_ID, PaymentStatus.COMPLETED))
                .thenReturn(Optional.of(new Payment()));

        assertThatThrownBy(() -> paymentService.processPayment(USER_ID.toString(), ORDER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment already exists");
    }

    @Test
    void processPayment_refundRequested_throwsBusinessException() {
        Order order = createOrder(OrderStatus.RETURN_REQUESTED, Order.PaymentStatus.REFUND_REQUESTED);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(USER_ID.toString(), ORDER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending refund");
    }

    @Test
    void processPayment_invalidCardNumber_throwsBusinessException() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "123", "John Doe", 12, 2026, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(USER_ID.toString(), ORDER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid card number");
    }

    @Test
    void processPayment_expiredCard_throwsBusinessException() {
        Order order = createOrder(OrderStatus.PENDING, Order.PaymentStatus.PENDING);
        ProcessPaymentRequest request = new ProcessPaymentRequest(
                "4242424242424242", "John Doe", 12, 2020, "123");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(USER_ID.toString(), ORDER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
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
    void requestRefund_notOrderOwner_throwsBusinessException() {
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
        order.setCurrency("USD");
        return order;
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setOrderId(ORDER_ID);
        payment.setAmount(new BigDecimal("99.99"));
        payment.setCurrency("USD");
        payment.setMethod(Payment.PaymentMethod.CREDIT_CARD);
        payment.setStatus(status);
        payment.setCardLastFour("4242");
        payment.setCardBrand("VISA");
        payment.setProviderRef("MOCK-ABC12345");
        return payment;
    }
}
