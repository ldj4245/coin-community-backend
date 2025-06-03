package com.coincommunity.backend.security;

import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * 사용자 인증 정보를 로드하는 서비스
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            // ID로 사용자 조회
            Long userId = Long.parseLong(username);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
            
            return createUserDetails(user);
        } catch (NumberFormatException e) {
            // 이메일로 사용자 조회
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
            
            return createUserDetails(user);
        }
    }
    
    /**
     * User 엔티티를 스프링 시큐리티의 UserDetails로 변환
     */
    private UserDetails createUserDetails(User user) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        
        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),  // 사용자 ID를 username으로 사용
                user.getPassword(),
                Collections.singleton(authority)
        );
    }
}
