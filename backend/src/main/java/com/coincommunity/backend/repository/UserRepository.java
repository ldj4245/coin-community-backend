package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 사용자명으로 사용자를 찾습니다.
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 이메일로 사용자를 찾습니다.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 사용자명이나 이메일로 사용자를 찾습니다.
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * 사용자명으로 사용자 존재 여부를 확인합니다.
     */
    boolean existsByUsername(String username);
    
    /**
     * 이메일로 사용자 존재 여부를 확인합니다.
     */
    boolean existsByEmail(String email);
    
    /**
     * 사용자명이나 이메일로 사용자가 존재하는지 확인합니다.
     */
    boolean existsByUsernameOrEmail(String username, String email);
}
