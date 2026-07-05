package com.marketplace.payment.service;

import com.marketplace.notification.model.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.payment.dto.IpnNotification;
import com.marketplace.payment.dto.PaymentCallbackRequest;
import com.marketplace.payment.dto.PaymentHistoryResponse;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.RefundPaymentRequest;
import com.marketplace.payment.dto.SePayCheckoutResponse;
import com.marketplace.payment.dto.SePayRefundResponse;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.model.Payment.PaymentMethod;
import com.marketplace.payment.model.Payment.PaymentStatus;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.shared.exception.AccessDeniedException;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SePayService sePayService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                          SePayService sePayService, ObjectMapper objectMapper,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.sePayService = sePayService;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public SePayCheckoutResponse initiatePayment(String userId, UUID orderId, String paymentMethod) {
        UUID buyerId = UUID.fromString(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Order does not belong to this user");
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RETURNED) {
            throw new BusinessException("Cannot process payment for this order");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BusinessException("Order has already been paid");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.REFUNDED) {
            throw new BusinessException("Order has been refunded");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.REFUND_REQUESTED) {
            throw new BusinessException("Order has a pending refund");
        }

        paymentRepository.deleteByOrderIdAndStatus(orderId, PaymentStatus.PENDING);

        String invoiceNumber = "ORD" + orderId.toString().replace("-", "").substring(0, 8).toUpperCase();

        PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
        Payment payment = new Payment(order.getId(), order.getTotalAmount(), order.getCurrency(), method);
        payment.setInvoiceNumber(invoiceNumber);
        paymentRepository.save(payment);

        orderRepository.updatePaymentStatus(orderId, Order.PaymentStatus.PENDING, Instant.now());

        String successUrl = sePayService.buildSuccessUrl(orderId.toString());
        String errorUrl = sePayService.buildErrorUrl(orderId.toString());
        String cancelUrl = sePayService.buildCancelUrl(orderId.toString());

        SePayCheckoutResponse response = sePayService.createCheckoutFields(
                invoiceNumber,
                order.getTotalAmount(),
                order.getCurrency(),
                "Thanh toan don hang " + invoiceNumber,
                successUrl,
                errorUrl,
                cancelUrl
        );

        log.info("SEPay payment initiated: invoice={}, orderId={}, amount={} {}",
                invoiceNumber, orderId, order.getTotalAmount(), order.getCurrency());

        return response;
    }

    @Transactional
    public void handleIpnNotification(String rawBody, String secretKey) {
        if (!sePayService.verifyIpnSecretKey(secretKey)) {
            log.warn("IPN secret key verification failed");
            throw new BusinessException("Invalid IPN secret key");
        }

        IpnNotification notification;
        try {
            notification = objectMapper.readValue(rawBody, IpnNotification.class);
        } catch (Exception e) {
            log.error("Failed to deserialize IPN payload: {}", e.getMessage());
            throw new BusinessException("Invalid IPN payload");
        }

        log.info("SEPay IPN received: notificationType={}, orderId={}, invoice={}, status={}",
                notification.notificationType(),
                notification.order() != null ? notification.order().orderId() : null,
                notification.order() != null ? notification.order().orderInvoiceNumber() : null,
                notification.transaction() != null ? notification.transaction().transactionStatus() : null);

        if (!"ORDER_PAID".equals(notification.notificationType())) {
            log.info("IPN ignored: notificationType={}, not ORDER_PAID", notification.notificationType());
            return;
        }

        if (notification.order() == null || notification.order().orderInvoiceNumber() == null
                || notification.order().orderInvoiceNumber().isBlank()) {
            log.warn("IPN ignored: no order invoice number");
            return;
        }

        String invoiceNumber = notification.order().orderInvoiceNumber();

        Payment payment = paymentRepository.findFirstByInvoiceNumber(invoiceNumber)
                .orElse(null);

        if (payment == null) {
            log.warn("IPN: no payment found for invoice={}", invoiceNumber);
            return;
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment already completed, skipping IPN: invoice={}", invoiceNumber);
            return;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProviderRef(notification.order().id());
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        orderRepository.confirmOnPayment(payment.getOrderId(), Order.PaymentStatus.PAID, OrderStatus.CONFIRMED, Instant.now());

        Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
        if (order != null) {
            notificationService.createNotification(
                    order.getBuyerId(),
                    NotificationType.PAYMENT_UPDATE,
                    "Payment Successful",
                    "Your payment for order #" + order.getId().toString().substring(0, 8).toUpperCase() + " has been completed.",
                    order.getId(),
                    "ORDER"
            );
        }

        log.info("Payment completed via IPN: paymentId={}, orderId={}, invoice={}",
                payment.getId(), payment.getOrderId(), invoiceNumber);
    }

    public void logPaymentCallback(PaymentCallbackRequest request) {
        UUID orderId = UUID.fromString(request.orderId());
        log.warn("SEPay callback: orderId={}, status={}, params={}", orderId, request.paymentStatus(), request.params());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String userId, UUID paymentId) {
        UUID buyerId = UUID.fromString(userId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Payment does not belong to this user");
        }

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse requestRefund(String userId, UUID paymentId, RefundPaymentRequest request) {
        UUID buyerId = UUID.fromString(userId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        UUID orderId = payment.getOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Payment does not belong to this user");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Only completed payments can be refunded");
        }

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED
                && order.getStatus() != OrderStatus.RETURNED
                && order.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessException("Refund requires order to be RETURN_REQUESTED, RETURNED, or CANCELLED");
        }

        payment.setStatus(PaymentStatus.REFUND_REQUESTED);
        payment.setUpdatedAt(Instant.now());

        order.setPaymentStatus(Order.PaymentStatus.REFUND_REQUESTED);
        order.setUpdatedAt(Instant.now());

        payment = paymentRepository.save(payment);
        orderRepository.save(order);

        notificationService.createNotification(
                buyerId,
                NotificationType.PAYMENT_UPDATE,
                "Refund Requested",
                "Your refund request for order #" + order.getId().toString().substring(0, 8).toUpperCase() + " has been submitted.",
                order.getId(),
                "ORDER"
        );

        log.info("Refund requested: paymentId={}, orderId={}", payment.getId(), order.getId());

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse approveRefund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new BusinessException("Payment does not have a pending refund request");
        }

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));

        SePayRefundResponse refundResponse = sePayService.refundTransaction(payment.getInvoiceNumber());

        if (!refundResponse.success()) {
            payment.setFailureReason("SePay refund failed: " + refundResponse.message());
            payment.setUpdatedAt(Instant.now());
            paymentRepository.save(payment);
            log.error("SePay refund rejected: paymentId={}, invoice={}, reason={}",
                    paymentId, payment.getInvoiceNumber(), refundResponse.message());
            throw new BusinessException("SePay refund failed: " + refundResponse.message());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        order.setUpdatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getBuyerId(),
                NotificationType.PAYMENT_UPDATE,
                "Refund Approved",
                "Your refund for order #" + order.getId().toString().substring(0, 8).toUpperCase() + " has been approved.",
                order.getId(),
                "ORDER"
        );

        log.info("Refund approved: paymentId={}, orderId={}, invoice={}", savedPayment.getId(), order.getId(), payment.getInvoiceNumber());

        return PaymentResponse.from(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(String userId) {
        UUID buyerId = UUID.fromString(userId);
        return paymentRepository.findByBuyerId(buyerId).stream()
                .map(PaymentHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getSellerPaymentHistory(String userId) {
        UUID sellerId = UUID.fromString(userId);
        return paymentRepository.findCompletedBySellerId(sellerId).stream()
                .map(PaymentHistoryResponse::from)
                .toList();
    }
}
