package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.UserDto;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.entity.UserRole;
import com.coincommunity.backend.entity.UserStatus;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.UserRepository;
import com.coincommunity.backend.repository.PostRepository;
import com.coincommunity.backend.repository.CommentRepository;
import com.coincommunity.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * 사용자 ID로 사용자를 조회합니다.
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 사용자명으로 사용자를 조회합니다.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 이메일로 사용자를 조회합니다.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * 사용자명 또는 이메일로 사용자를 조회합니다.
     */
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }
    
    /**
     * 회원가입
     */
    @Transactional
    public UserDto.ProfileResponse register(UserDto.RegisterRequest request) {
        // 중복 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        
        // 사용자 생성
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        
        User savedUser = userRepository.save(user);
        return UserDto.ProfileResponse.from(savedUser);
    }
    
    /**
     * 로그인
     */
    public UserDto.LoginResponse login(UserDto.LoginRequest request) {
        Optional<User> userOpt = findByUsernameOrEmail(request.getUsernameOrEmail());
        
        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("사용자를 찾을 수 없습니다.");
        }
        
        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("비활성화된 계정입니다.");
        }
        
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        
        return new UserDto.LoginResponse(accessToken, "Bearer", UserDto.UserResponse.from(user));
    }
    
    /**
     * 사용자 프로필 조회
     */
    public UserDto.ProfileResponse getUserProfile(Long userId) {
        User user = findById(userId);
        return UserDto.ProfileResponse.from(user);
    }
    
    /**
     * 사용자 게시글 목록 조회
     */
    public Page<UserDto.PostSummaryResponse> getUserPosts(Long userId, Pageable pageable) {
        findById(userId); // 사용자 존재 확인
        return postRepository.findByUserId(userId, pageable)
                .map(post -> UserDto.PostSummaryResponse.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .category(post.getCategory().name())
                        .categoryDisplayName(post.getCategory().getDisplayName())
                        .viewCount(post.getViewCount())
                        .likeCount(post.getLikeCount())
                        .commentCount(post.getCommentCount())
                        .createdAt(post.getCreatedAt())
                        .build());
    }
    
    /**
     * 사용자 댓글 목록 조회
     */
    public Page<UserDto.CommentSummaryResponse> getUserComments(Long userId, Pageable pageable) {
        findById(userId); // 사용자 존재 확인
        return commentRepository.findByUserId(userId, pageable)
                .map(comment -> UserDto.CommentSummaryResponse.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .postId(comment.getPost().getId())
                        .postTitle(comment.getPost().getTitle())
                        .build());
    }
    
    /**
     * 프로필 업데이트
     */
    @Transactional
    public UserDto.ProfileResponse updateProfile(Long userId, UserDto.UpdateRequest request) {
        User user = findById(userId);
        user.updateProfile(request.getNickname(), request.getAvatarUrl());
        User updatedUser = userRepository.save(user);
        return UserDto.ProfileResponse.from(updatedUser);
    }
    
    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, UserDto.PasswordChangeRequest request) {
        User user = findById(userId);
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    /**
     * 사용자 상태 변경
     */
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = findById(userId);
        user.setStatus(status);
        userRepository.save(user);
    }
}
