package com.omnicharge.user.repository;

import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    void deleteByUser(User user);

    void deleteByToken(String token);

    long countByUser(User user);

    /** Ordered oldest-first for FIFO eviction */
    List<RefreshToken> findByUserOrderByExpiryDateAsc(User user);

    /** Cleanup expired tokens */
    void deleteByExpiryDateBefore(Instant cutoff);
}

