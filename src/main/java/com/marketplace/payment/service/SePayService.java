package com.marketplace.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.payment.config.SePayProperties;
import com.marketplace.payment.dto.SePayCheckoutResponse;
import com.marketplace.payment.dto.SePayRefundResponse;
import com.marketplace.shared.exception.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SePayService {

    private static final Logger log = LoggerFactory.getLogger(SePayService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private static final Set<String> SIGNED_FIELDS_ALLOWLIST = Set.of(
        "merchant", "env", "operation", "payment_method",
        "order_amount", "currency", "order_invoice_number",
        "order_description", "customer_id", "agreement_id",
        "agreement_name", "agreement_type", "agreement_payment_frequency",
        "agreement_amount_per_payment", "success_url", "error_url",
        "cancel_url", "order_id"
    );

    private final SePayProperties props;

    public SePayService(SePayProperties props) {
        this.props = props;
    }

    public SePayCheckoutResponse createCheckoutFields(String invoiceNumber, BigDecimal amount,
                                                       String currency, String description,
                                                       String successUrl, String errorUrl,
                                                       String cancelUrl) {
        if (props.merchantId() == null || props.merchantId().isBlank()
                || props.secretKey() == null || props.secretKey().isBlank()) {
            throw new BusinessException("SEPay credentials are not configured. Set SEPAY_MERCHANT_ID and SEPAY_SECRET_KEY.");
        }

        String amountStr = formatAmount(amount, currency);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("order_amount", amountStr);
        fields.put("merchant", props.merchantId());
        fields.put("currency", currency);
        fields.put("operation", "PURCHASE");
        fields.put("order_description", description != null ? description : "");
        fields.put("order_invoice_number", invoiceNumber);
        fields.put("success_url", successUrl);
        fields.put("error_url", errorUrl);
        fields.put("cancel_url", cancelUrl);

        String signatureInput = buildSignatureInput(fields);
        String signature = hmacSha256Base64(signatureInput, props.secretKey());
        fields.put("signature", signature);

        log.info("SEPay checkout: invoice={}, amount={}, currency={}", invoiceNumber, amountStr, currency);
        log.info("SEPay signature input: {}", signatureInput);
        log.info("SEPay signature: {}", signature);
        log.info("SEPay form fields: {}", fields);

        return new SePayCheckoutResponse(props.getCheckoutUrl(), fields);
    }

    public SePayRefundResponse refundTransaction(String orderInvoiceNumber) {
        if (props.isSandbox()) {
            log.info("[SANDBOX] Simulating SePay refund: invoice={}", orderInvoiceNumber);
            return SePayRefundResponse.success(orderInvoiceNumber);
        }

        if (props.merchantId() == null || props.merchantId().isBlank()
                || props.secretKey() == null || props.secretKey().isBlank()) {
            throw new BusinessException("SEPay credentials are not configured");
        }

        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (props.merchantId() + ":" + props.secretKey()).getBytes(StandardCharsets.UTF_8)
            );

            String requestBody = "{\"order_invoice_number\":\"" + orderInvoiceNumber + "\"}";

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getRefundApiUrl()))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String message = json.has("message") ? json.get("message").asText() : "Refund processed";
                log.info("SePay refund success: invoice={}, status={}", orderInvoiceNumber, response.statusCode());
                return new SePayRefundResponse(true, message, orderInvoiceNumber);
            } else {
                String errorMessage = json.has("message") ? json.get("message").asText() : "HTTP " + response.statusCode();
                log.error("SePay refund failed: invoice={}, status={}, response={}", orderInvoiceNumber, response.statusCode(), response.body());
                return SePayRefundResponse.failure(errorMessage);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SePay refund interrupted: invoice={}", orderInvoiceNumber, e);
            return SePayRefundResponse.failure("Refund request interrupted");
        } catch (Exception e) {
            log.error("SePay refund error: invoice={}", orderInvoiceNumber, e);
            return SePayRefundResponse.failure("Refund request failed: " + e.getMessage());
        }
    }

    private String buildSignatureInput(Map<String, String> fields) {
        return fields.keySet().stream()
                .filter(SIGNED_FIELDS_ALLOWLIST::contains)
                .filter(f -> fields.get(f) != null)
                .map(f -> f + "=" + fields.get(f))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    public boolean verifyIpnSecretKey(String receivedSecretKey) {
        String expected = props.secretKey();
        if (expected == null || expected.isBlank()) {
            log.warn("SEPAY_SECRET_KEY is not configured — skipping IPN verification");
            return true;
        }
        if (receivedSecretKey == null || receivedSecretKey.isBlank()) {
            log.warn("X-Secret-Key header is missing — rejecting IPN");
            return false;
        }
        String cleaned = receivedSecretKey.trim();
        if (cleaned.toLowerCase().startsWith("secretkey=")) {
            cleaned = cleaned.substring("secretkey=".length());
        }
        boolean valid = expected.trim().equals(cleaned);
        if (!valid) {
            log.warn("IPN X-Secret-Key mismatch: expected=[{}], received=[{}]", expected.trim(), cleaned);
        }
        return valid;
    }

    public String buildSuccessUrl(String orderId) {
        return props.successUrl().replace("{orderId}", orderId);
    }

    public String buildErrorUrl(String orderId) {
        return props.errorUrl().replace("{orderId}", orderId);
    }

    public String buildCancelUrl(String orderId) {
        return props.cancelUrl().replace("{orderId}", orderId);
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if ("VND".equalsIgnoreCase(currency)) {
            return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return amount.toPlainString();
    }

    private String hmacSha256Base64(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }
}
