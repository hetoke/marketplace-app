package com.marketplace.payment.service;

import com.marketplace.order.model.Order;
import com.marketplace.order.model.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.payment.dto.PaymentHistoryResponse;
import com.marketplace.payment.dto.PaymentResponse;
import com.marketplace.payment.dto.ProcessPaymentRequest;
import com.marketplace.payment.dto.RefundPaymentRequest;
import com.marketplace.payment.model.Payment;
import com.marketplace.payment.model.Payment.PaymentMethod;
import com.marketplace.payment.model.Payment.PaymentStatus;
import com.marketplace.payment.repository.PaymentRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
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

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public PaymentResponse processPayment(String userId, ProcessPaymentRequest request) {
        UUID buyerId = UUID.fromString(userId);
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.orderId()));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException("Order does not belong to this user");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BusinessException("Order has already been paid");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.REFUNDED) {
            throw new BusinessException("Order has been refunded");
        }

        validateCard(request.cardNumber(), request.expiryMonth(), request.expiryYear(), request.cvv());

        String cardLastFour = request.cardNumber().replaceAll("\\s", "").substring(
                request.cardNumber().replaceAll("\\s", "").length() - 4);
        String cardBrand = detectCardBrand(request.cardNumber());
        String providerRef = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payment payment = new Payment(
                order.getId(),
                order.getTotalAmount(),
                order.getCurrency(),
                PaymentMethod.CREDIT_CARD,
                cardLastFour,
                cardBrand,
                providerRef
        );

        boolean approved = mockAuthorize(request.cardNumber(), order.getTotalAmount());

        if (approved) {
            payment.setStatus(PaymentStatus.COMPLETED);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient funds");
            log.warn("Payment declined: orderId={}, card={}", order.getId(), cardLastFour);
        }

        payment = paymentRepository.save(payment);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        if (approved) {
            log.info("Payment completed: id={}, orderId={}, amount={} {}",
                    payment.getId(), order.getId(), order.getTotalAmount(), order.getCurrency());
        }

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String userId, UUID paymentId) {
        UUID buyerId = UUID.fromString(userId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", payment.getOrderId()));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException("Payment does not belong to this user");
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
            throw new BusinessException("Payment does not belong to this user");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Only completed payments can be refunded");
        }

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED
                && order.getStatus() != OrderStatus.RETURNED
                && order.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessException("Refund requires order to be RETURN_REQUESTED, RETURNED, or CANCELLED");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());

        order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        order.setUpdatedAt(Instant.now());

        payment = paymentRepository.save(payment);
        orderRepository.save(order);

        log.info("Refund processed: paymentId={}, orderId={}", payment.getId(), order.getId());

        return PaymentResponse.from(payment);
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

    private void validateCard(String cardNumber, Integer expiryMonth, Integer expiryYear, String cvv) {
        String digits = cardNumber.replaceAll("\\s", "");
        if (!digits.matches("\\d{13,19}")) {
            throw new BusinessException("Invalid card number format");
        }
        if (expiryMonth < 1 || expiryMonth > 12) {
            throw new BusinessException("Invalid expiry month");
        }
        if (expiryYear < 2024) {
            throw new BusinessException("Card has expired");
        }
        if (expiryYear == 2024 && expiryMonth < Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate().getMonthValue()) {
            throw new BusinessException("Card has expired");
        }
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new BusinessException("Invalid CVV");
        }
    }

    private boolean mockAuthorize(String cardNumber, java.math.BigDecimal amount) {
        String digits = cardNumber.replaceAll("\\s", "");
        if ("4000000000000002".equals(digits)) {
            return false;
        }
        return true;
    }

    private String detectCardBrand(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s", "");
        if (digits.startsWith("4")) return "VISA";
        if (digits.startsWith("5") || digits.startsWith("2")) return "MASTERCARD";
        if (digits.startsWith("37")) return "AMEX";
        return "UNKNOWN";
    }
}
