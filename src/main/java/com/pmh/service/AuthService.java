package com.pmh.service;

import com.pmh.model.dto.AuthResponse;
import com.pmh.model.dto.LoginRequest;
import com.pmh.model.entity.RefreshToken;
import com.pmh.model.entity.Users;
import com.pmh.repository.RefreshTokenRepository;
import com.pmh.repository.UsersRepository;
import com.pmh.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UsersRepository usersRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        Users user = usersRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        String accessToken = jwtUtils.generateAccessToken(user);

        // Xóa refresh token cũ (nếu muốn 1 user chỉ 1 token)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token đã hết hạn");
        }

        String accessToken = jwtUtils.generateAccessToken(token.getUser());
        return new AuthResponse(accessToken, refreshTokenStr);
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.deleteByToken(refreshTokenStr);
    }
}

