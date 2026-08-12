package com.mybeaufortviewproject.mybeaufortview_backend.like;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);
    Optional<Like> findByUser_IdAndPost_Id(Long userId, Long postId);
    long countByPost_Id(Long postId);
    void deleteByUser_IdAndPost_Id(Long userId, Long postId);
}
