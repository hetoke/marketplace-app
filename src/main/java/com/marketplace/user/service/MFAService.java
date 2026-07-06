package com.marketplace.user.service;

import com.marketplace.email.EmailService;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.security.JwtTokenProvider;
import com.marketplace.shared.security.TokenType;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.MFAChallenge;
import com.marketplace.user.model.RecoveryCode;
import com.marketplace.user.model.RefreshToken;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.MFAChallengeRepository;
import com.marketplace.user.repository.RecoveryCodeRepository;
import com.marketplace.user.repository.RefreshTokenRepository;
import com.marketplace.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MFAService {

    private static final Logger log = LoggerFactory.getLogger(MFAService.class);
    private static final int OTP_LENGTH = 6;
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final int RECOVERY_CODE_LENGTH = 8;
    private static final int CHALLENGE_EXPIRY_MINUTES = 5;
    private static final SecureRandom secureRandom = new SecureRandom();

    private final MFAChallengeRepository mfaChallengeRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public MFAService(MFAChallengeRepository mfaChallengeRepository,
                      RecoveryCodeRepository recoveryCodeRepository,
                      UserRepository userRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider,
                      EmailService emailService) {
        this.mfaChallengeRepository = mfaChallengeRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    @Transactional
    public String setupMFA(User user) {
        if (user.isMfaEnabled()) {
            throw new BusinessException("MFA is already enabled");
        }

        String otp = generateOtp();
        String codeHash = passwordEncoder.encode(otp);

        mfaChallengeRepository.deleteByUserIdAndType(user.getId(), MFAChallenge.ChallengeType.SETUP);
        MFAChallenge challenge = new MFAChallenge(user, codeHash, MFAChallenge.ChallengeType.SETUP,
                Instant.now().plus(CHALLENGE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        mfaChallengeRepository.save(challenge);

        emailService.sendMfaOtpEmail(user.getEmail(), otp);
        return otp;
    }

    @Transactional
    public AuthResponse verifySetup(User user, String otp) {
        if (user.isMfaEnabled()) {
            throw new BusinessException("MFA is already enabled");
        }

        MFAChallenge challenge = findValidChallenge(user, MFAChallenge.ChallengeType.SETUP, otp);

        user.setMfaEnabled(true);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        mfaChallengeRepository.delete(challenge);

        List<String> recoveryCodes = generateRecoveryCodes(user);
        log.info("MFA enabled for {}, recovery codes: {}", user.getEmail(), recoveryCodes);

        return new AuthResponse(null, null, UserResponse.from(user));
    }

    @Transactional
    public void disableMFA(User user, String otp) {
        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA is not enabled");
        }

        findValidChallenge(user, MFAChallenge.ChallengeType.DISABLE, otp);

        user.setMfaEnabled(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        mfaChallengeRepository.deleteByUserIdAndType(user.getId(), MFAChallenge.ChallengeType.DISABLE);
        recoveryCodeRepository.findByUserIdAndUsedFalse(user.getId())
                .forEach(rc -> {
                    rc.setUsed(true);
                    rc.setUsedAt(Instant.now());
                });
    }

    @Transactional
    public String sendDisableOTP(User user) {
        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA is not enabled");
        }

        String otp = generateOtp();
        String codeHash = passwordEncoder.encode(otp);

        mfaChallengeRepository.deleteByUserIdAndType(user.getId(), MFAChallenge.ChallengeType.DISABLE);
        MFAChallenge challenge = new MFAChallenge(user, codeHash, MFAChallenge.ChallengeType.DISABLE,
                Instant.now().plus(CHALLENGE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        mfaChallengeRepository.save(challenge);

        emailService.sendMfaOtpEmail(user.getEmail(), otp);
        return otp;
    }

    @Transactional
    public String sendLoginOTP(User user) {
        String otp = generateOtp();
        String codeHash = passwordEncoder.encode(otp);

        mfaChallengeRepository.deleteByUserIdAndType(user.getId(), MFAChallenge.ChallengeType.LOGIN);
        MFAChallenge challenge = new MFAChallenge(user, codeHash, MFAChallenge.ChallengeType.LOGIN,
                Instant.now().plus(CHALLENGE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        mfaChallengeRepository.save(challenge);

        emailService.sendMfaOtpEmail(user.getEmail(), otp);
        return otp;
    }

    @Transactional
    public AuthResponse verifyLoginOTP(String mfaToken, String otp) {
        String userId = jwtTokenProvider.getUserIdFromToken(mfaToken, TokenType.MFA);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA is not enabled for this user");
        }

        findValidChallenge(user, MFAChallenge.ChallengeType.LOGIN, otp);
        mfaChallengeRepository.deleteByUserIdAndType(user.getId(), MFAChallenge.ChallengeType.LOGIN);

        return generateTokenPair(user);
    }

    @Transactional
    public AuthResponse verifyRecoveryCode(String mfaToken, String code) {
        String userId = jwtTokenProvider.getUserIdFromToken(mfaToken, TokenType.MFA);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.isMfaEnabled()) {
            throw new BusinessException("MFA is not enabled for this user");
        }

        List<RecoveryCode> unusedCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(user.getId());
        for (RecoveryCode rc : unusedCodes) {
            if (passwordEncoder.matches(code, rc.getCodeHash())) {
                rc.setUsed(true);
                rc.setUsedAt(Instant.now());
                recoveryCodeRepository.save(rc);

                mfaChallengeRepository.deleteByUserIdAndType(user.getId(), MFAChallenge.ChallengeType.LOGIN);
                return generateTokenPair(user);
            }
        }

        throw new BusinessException("Invalid recovery code");
    }

    public long getRecoveryCodesRemaining(User user) {
        return recoveryCodeRepository.countByUserIdAndUsedFalse(user.getId());
    }

    private MFAChallenge findValidChallenge(User user, MFAChallenge.ChallengeType type, String otp) {
        List<MFAChallenge> challenges = mfaChallengeRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), type);

        for (MFAChallenge challenge : challenges) {
            if (challenge.isExpired()) {
                continue;
            }
            if (passwordEncoder.matches(otp, challenge.getCodeHash())) {
                return challenge;
            }
        }

        throw new BusinessException("Invalid or expired OTP");
    }

    private List<String> generateRecoveryCodes(User user) {
        List<String> plainCodes = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = generateRandomCode(RECOVERY_CODE_LENGTH);
            plainCodes.add(code);
            String hash = passwordEncoder.encode(code);
            recoveryCodeRepository.save(new RecoveryCode(user, hash));
        }
        return plainCodes;
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

    private String generateOtp() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
