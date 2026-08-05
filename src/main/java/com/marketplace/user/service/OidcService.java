package com.marketplace.user.service;

import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.security.JwtTokenProvider;
import com.marketplace.user.config.OidcProperties;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.RefreshToken;
import com.marketplace.user.model.User;
import com.marketplace.user.model.UserIdentity;
import com.marketplace.user.repository.RefreshTokenRepository;
import com.marketplace.user.repository.UserIdentityRepository;
import com.marketplace.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class OidcService {

    private static final Logger log = LoggerFactory.getLogger(OidcService.class);
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String OIDC_NO_PASSWORD = "OIDC_NO_PASSWORD_" + UUID.randomUUID().toString();

    private final OidcProperties oidcProperties;
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // One-time code store: code -> AuthResponse
    private final ConcurrentHashMap<String, AuthResponse> oneTimeCodes = new ConcurrentHashMap<>();
    // State store: state -> timestamp (for CSRF protection)
    private final ConcurrentHashMap<String, Instant> stateStore = new ConcurrentHashMap<>();

    public OidcService(OidcProperties oidcProperties,
                       UserRepository userRepository,
                       UserIdentityRepository userIdentityRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       ObjectMapper objectMapper) {
        this.oidcProperties = oidcProperties;
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public String getAuthorizationUrl() {
        OidcProperties.Google google = oidcProperties.getGoogle();
        if (google.getClientId() == null || google.getClientId().isBlank()) {
            throw new BusinessException("Google OAuth is not configured. Set GOOGLE_CLIENT_ID environment variable.");
        }
        String state = UUID.randomUUID().toString();
        stateStore.put(state, Instant.now());

        String encodedRedirectUri = URLEncoder.encode(google.getRedirectUri(), StandardCharsets.UTF_8);
        String encodedScopes = URLEncoder.encode(google.getScopes(), StandardCharsets.UTF_8);

        return GOOGLE_AUTH_URL
                + "?client_id=" + google.getClientId()
                + "&redirect_uri=" + encodedRedirectUri
                + "&response_type=code"
                + "&scope=" + encodedScopes
                + "&state=" + state
                + "&access_type=offline"
                + "&prompt=consent";
    }

    @Transactional
    public String handleCallback(String code, String state) {
        // Validate state (CSRF protection)
        if (state == null || !stateStore.containsKey(state)) {
            throw new BusinessException("Invalid or expired state parameter");
        }
        stateStore.remove(state);

        // Exchange authorization code for tokens
        JsonNode tokenResponse = exchangeCodeForTokens(code);
        log.info("Token response: {}", tokenResponse);
        String idToken = tokenResponse.has("id_token") ? tokenResponse.get("id_token").asText() : null;
        if (idToken == null) {
            throw new BusinessException("No ID token received from Google");
        }

        // Parse ID token to extract user info
        JsonNode claims = parseIdToken(idToken);
        String googleSub = claims.get("sub").asText();
        String email = claims.has("email") ? claims.get("email").asText() : null;
        String name = claims.has("name") ? claims.get("name").asText() : null;
        String picture = claims.has("picture") ? claims.get("picture").asText() : null;

        if (email == null) {
            throw new BusinessException("No email received from Google");
        }

        // Find or create user
        User user = findOrCreateUser(email, name, picture, googleSub);

        // Generate JWT tokens (same as regular login)
        AuthResponse authResponse = generateTokenPair(user);

        // Store in one-time code map
        String oneTimeCode = UUID.randomUUID().toString();
        oneTimeCodes.put(oneTimeCode, authResponse);

        // Schedule cleanup after 60 seconds
        scheduleCleanup(oneTimeCode);

        log.info("OIDC login successful for user {} via Google", user.getEmail());
        return oneTimeCode;
    }

    public AuthResponse exchangeOneTimeCode(String code) {
        AuthResponse response = oneTimeCodes.remove(code);
        if (response == null) {
            throw new BusinessException("Invalid or expired authorization code");
        }
        return response;
    }

    private User findOrCreateUser(String email, String name, String picture, String providerUserId) {
        // 1. Check if identity already linked
        UserIdentity existingIdentity = userIdentityRepository
                .findByProviderAndProviderUserId(UserIdentity.Provider.GOOGLE, providerUserId)
                .orElse(null);
        if (existingIdentity != null) {
            return existingIdentity.getUser();
        }

        // 2. Check if user with same email exists (account linking)
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            // Link Google identity to existing user
            UserIdentity identity = new UserIdentity(existingUser, UserIdentity.Provider.GOOGLE,
                    providerUserId, email, name, picture);
            userIdentityRepository.save(identity);

            // Update profile picture if not set
            if (existingUser.getProfilePictureUrl() == null && picture != null) {
                existingUser.setProfilePictureUrl(picture);
                existingUser.setUpdatedAt(Instant.now());
                userRepository.save(existingUser);
            }

            log.info("Linked Google identity to existing user {}", email);
            return existingUser;
        }

        // 3. Create new user
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(OIDC_NO_PASSWORD));
        newUser.setDisplayName(name != null ? name : email.substring(0, email.indexOf('@')));
        newUser.setRole(User.Role.BUYER);
        newUser.setVerified(true); // Google already verified the email
        newUser.setAuthenticationType(User.AuthenticationType.OIDC);
        newUser.setProfilePictureUrl(picture);
        newUser = userRepository.save(newUser);

        // Create identity link
        UserIdentity identity = new UserIdentity(newUser, UserIdentity.Provider.GOOGLE,
                providerUserId, email, name, picture);
        userIdentityRepository.save(identity);

        log.info("Created new user {} via Google OAuth", email);
        return newUser;
    }

    private AuthResponse generateTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        RefreshToken refreshToken = new RefreshToken(user, refreshTokenValue,
                Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenValue, UserResponse.from(user));
    }

    private JsonNode exchangeCodeForTokens(String code) {
        OidcProperties.Google google = oidcProperties.getGoogle();

        org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", google.getClientId());
        body.add("client_secret", google.getClientSecret());
        body.add("redirect_uri", google.getRedirectUri());
        body.add("grant_type", "authorization_code");

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request =
                new org.springframework.http.HttpEntity<>(body, headers);

        try {
            org.springframework.http.HttpEntity<String> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Failed to exchange authorization code for tokens", e);
            throw new BusinessException("Failed to exchange authorization code with Google");
        }
    }

    private JsonNode parseIdToken(String idToken) {
        try {
            // ID token is a JWT: header.payload.signature
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException("Invalid ID token format");
            }

            // Decode payload
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return objectMapper.readTree(payload);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse ID token", e);
            throw new BusinessException("Invalid ID token");
        }
    }

    private void scheduleCleanup(String code) {
        Thread.ofVirtual().name("oidc-cleanup-" + code).start(() -> {
            try {
                Thread.sleep(60_000); // 60 seconds
                oneTimeCodes.remove(code);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
