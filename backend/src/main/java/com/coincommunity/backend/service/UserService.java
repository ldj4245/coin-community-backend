package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.UserDto;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.exception.BusinessException;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.UserRepository;
import com.coincommunity.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 사용자 등록
     */
    @Transactional
    public UserDto.UserResponse register(UserDto.RegisterRequest request) {
        // 사용자명 중복 검사
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("이미 존재하는 사용자명입니다: " + request.getUsername());
        }

        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 존재하는 이메일입니다: " + request.getEmail());
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 사용자 생성 및 저장
        User user = request.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);

        log.info("새 사용자가 등록되었습니다: {}", savedUser.getUsername());
        return UserDto.UserResponse.from(savedUser);
    }

    /**
     * 사용자 로그인
     */
    public UserDto.LoginResponse login(UserDto.LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다: " + request.getUsername()));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("잘못된 비밀번호입니다");
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("사용자 로그인: {}", user.getUsername());
        return UserDto.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.UserResponse.from(user))
                .build();
    }

    /**
     * 사용자 ID로 조회
     */
    public UserDto.UserResponse findById(Long userId) {
        User user = getUserById(userId);
        return UserDto.UserResponse.from(user);
    }

    /**
     * 사용자명으로 조회
     */
    public UserDto.UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return UserDto.UserResponse.from(user);
    }

    /**
     * 프로필 업데이트
     */
    @Transactional
    public UserDto.UserResponse updateProfile(Long userId, UserDto.UpdateProfileRequest request) {
        User user = getUserById(userId);
        user.updateProfile(request.getNickname(), request.getProfileImageUrl());
        User updatedUser = userRepository.save(user);
        
        log.info("사용자 프로필이 업데이트되었습니다: {}", user.getUsername());
        return UserDto.UserResponse.from(updatedUser);
    }

    /**
     * 사용자 ID로 User 엔티티 조회 (내부 사용)
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 사용자명으로 User 엔티티 조회 (내부 사용)
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }
}