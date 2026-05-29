package com.devmatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devmatch.common.exception.BusinessException;
import com.devmatch.dto.LoginRequest;
import com.devmatch.dto.RegisterRequest;
import com.devmatch.entity.User;
import com.devmatch.entity.Wallet;
import com.devmatch.mapper.UserMapper;
import com.devmatch.mapper.WalletMapper;
import com.devmatch.security.JwtUtil;
import com.devmatch.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;

    /** 演示用固定验证码，无需接入真实短信服务 */
    private static final String DEMO_SMS_CODE = "123456";

    private static final String SMS_CODE_KEY = "sms:code:";
    private static final long SMS_CODE_EXPIRE = 5; // 分钟

    @Transactional
    public Map<String, Object> login(LoginRequest req) {
        User user;
        if ("SMS".equals(req.getLoginType())) {
            user = verifyAndGetUserBySms(req.getUsername(), req.getSmsCode());
        } else {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            user = ((SecurityUser) auth.getPrincipal()).getUser();
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "user", buildUserInfo(user)
        );
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest req) {
        verifySmsCode(req.getPhone(), req.getCode());

        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone())) > 0) {
            throw new BusinessException("该手机号已注册");
        }

        User user = new User();
        user.setUsername(req.getPhone());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setKycStatus("NONE");
        user.setStatus("ACTIVE");
        user.setCreditScore(100);

        String nickname = req.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = "用户" + req.getPhone().substring(7);
        }
        user.setNickname(nickname);

        if ("ENTERPRISE".equals(req.getRole())) {
            user.setCompanyName(req.getCompanyName());
        }
        userMapper.insert(user);

        // 自动创建钱包
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozen(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        wallet.setTotalExpense(BigDecimal.ZERO);
        walletMapper.insert(wallet);

        // 发送欢迎通知
        notificationService.send(user.getId(), "SYSTEM", "欢迎加入DevMatch！", "/");

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "user", buildUserInfo(user)
        );
    }

    public void sendSmsCode(String phone) {
        redisTemplate.opsForValue().set(SMS_CODE_KEY + phone, DEMO_SMS_CODE, SMS_CODE_EXPIRE, TimeUnit.MINUTES);
        log.info("[演示] 短信验证码: {} -> {}", phone, DEMO_SMS_CODE);
    }

    public void verifySmsCode(String phone, String code) {
        Object cached = redisTemplate.opsForValue().get(SMS_CODE_KEY + phone);
        if (cached == null || !cached.toString().equals(code)) {
            throw new BusinessException("验证码错误或已过期");
        }
        redisTemplate.delete(SMS_CODE_KEY + phone);
    }

    @Transactional
    public void resetPassword(String phone, String smsCode, String newPassword) {
        verifySmsCode(phone, smsCode);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) throw new BusinessException("用户不存在");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(401, "刷新Token无效");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        return Map.of("accessToken", newAccessToken);
    }

    private User verifyAndGetUserBySms(String phone, String code) {
        verifySmsCode(phone, code);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) throw new BusinessException("手机号未注册");
        return user;
    }

    private Map<String, Object> buildUserInfo(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "role", user.getRole(),
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "kycStatus", user.getKycStatus(),
                "creditScore", user.getCreditScore()
        );
    }
}
