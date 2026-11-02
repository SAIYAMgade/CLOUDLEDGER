package com.cloudledger.identity;

import com.cloudledger.common.exceptions.BusinessException;
import com.cloudledger.identity.dto.LoginRequest;
import com.cloudledger.identity.dto.LoginResponse;
import com.cloudledger.identity.dto.RefreshTokenRequest;
import com.cloudledger.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), roles);
        String refreshToken = jwtUtils.generateRefreshToken(user.getUsername());

        return new LoginResponse(accessToken, refreshToken, user.getUserId(), user.getUsername(), user.getEmail(), roles);
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        if (!jwtUtils.validateToken(request.getRefreshToken())) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "Provided refresh token is invalid or expired");
        }

        String username = jwtUtils.getUsernameFromToken(request.getRefreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleId)
                .collect(Collectors.toList());

        String newAccessToken = jwtUtils.generateAccessToken(user.getUsername(), roles);
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getUsername());

        return new LoginResponse(newAccessToken, newRefreshToken, user.getUserId(), user.getUsername(), user.getEmail(), roles);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
    }
}
