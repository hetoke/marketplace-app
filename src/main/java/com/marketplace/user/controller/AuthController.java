package com.marketplace.user.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.ForgotPasswordRequest;
import com.marketplace.user.dto.LoginRequest;
import com.marketplace.user.dto.RefreshTokenRequest;
import com.marketplace.user.dto.RegisterRequest;
import com.marketplace.user.dto.ResendVerificationTokenRequest;
import com.marketplace.user.dto.ResetPasswordRequest;
import com.marketplace.user.dto.TokenResponse;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.dto.VerifyEmailRequest;
import com.marketplace.user.service.AuthService;
import com.marketplace.user.service.MFAService;
import com.marketplace.user.service.OidcService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final OidcService oidcService;
	private final MFAService mfaService;

	public AuthController(AuthService authService, OidcService oidcService, MFAService mfaService) {
		this.authService = authService;
		this.oidcService = oidcService;
		this.mfaService = mfaService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse user = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Registration successful", user));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse auth = authService.login(request);
		return ResponseEntity.ok(ApiResponse.ok("Login successful", auth));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		TokenResponse auth = authService.refreshToken(request);
		return ResponseEntity.ok(ApiResponse.ok("Token refreshed", auth));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout() {
		var auth = org.springframework.security.core.context.SecurityContextHolder
				.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
			return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
		}
		authService.logout(auth.getName());
		return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
	}

	@PostMapping("/verify-email")
	public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request);
		return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", null));
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<ApiResponse<Void>> resendVerificationToken(
			@Valid @RequestBody ResendVerificationTokenRequest request) {
		authService.resendVerificationToken(request);
		return ResponseEntity.ok(ApiResponse.ok("If the email exists and is unverified, a new verification token has been sent", null));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
		return ResponseEntity.ok(ApiResponse.ok("If the email exists, a reset link has been sent", null));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
	}

	@GetMapping("/oidc/login")
	public RedirectView oidcLogin(@RequestParam(defaultValue = "google") String provider) {
		String authorizationUrl = oidcService.getAuthorizationUrl();
		return new RedirectView(authorizationUrl);
	}

	@GetMapping("/oidc/callback")
	public RedirectView oidcCallback(@RequestParam String code, @RequestParam String state) {
		String oneTimeCode = oidcService.handleCallback(code, state);
		return new RedirectView("http://localhost:3000/auth/callback?code=" + oneTimeCode);
	}

	@PostMapping("/oidc/token")
	public ResponseEntity<ApiResponse<AuthResponse>> oidcToken(@RequestBody Map<String, String> body) {
		String code = body.get("code");
		if (code == null || code.isBlank()) {
			return ResponseEntity.badRequest().body(ApiResponse.ok("Authorization code is required", null));
		}
		AuthResponse auth = oidcService.exchangeOneTimeCode(code);
		return ResponseEntity.ok(ApiResponse.ok("OIDC login successful", auth));
	}

	@PostMapping("/mfa/verify")
	public ResponseEntity<ApiResponse<AuthResponse>> verifyMfaLogin(@RequestBody Map<String, String> body) {
		String mfaToken = body.get("mfaToken");
		String otp = body.get("otp");
		if (mfaToken == null || otp == null) {
			return ResponseEntity.badRequest().body(ApiResponse.ok("mfaToken and otp are required", null));
		}
		AuthResponse auth = mfaService.verifyLoginOTP(mfaToken, otp);
		return ResponseEntity.ok(ApiResponse.ok("Login successful", auth));
	}

	@PostMapping("/mfa/recovery")
	public ResponseEntity<ApiResponse<AuthResponse>> verifyRecoveryCode(@RequestBody Map<String, String> body) {
		String mfaToken = body.get("mfaToken");
		String recoveryCode = body.get("recoveryCode");
		if (mfaToken == null || recoveryCode == null) {
			return ResponseEntity.badRequest().body(ApiResponse.ok("mfaToken and recoveryCode are required", null));
		}
		AuthResponse auth = mfaService.verifyRecoveryCode(mfaToken, recoveryCode);
		return ResponseEntity.ok(ApiResponse.ok("Login successful", auth));
	}
}
