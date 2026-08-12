package com.mybeaufortviewproject.mybeaufortview_backend.media;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MediaJobRepository extends JpaRepository<MediaJob, Long> {

    List<MediaJob> findByPostId(Long postId);

    List<MediaJob> findByPostIdOrderByCreatedAtDesc(Long postId);

    Optional<MediaJob> findTopByPostIdAndJobTypeOrderByIdDesc(Long postId, MediaJobType jobType);

    @Modifying
    @Transactional
    @Query("DELETE FROM MediaJob j WHERE j.post.id = :postId")
    void deleteByPost_Id(@Param("postId") Long postId);

    // Eagerly fetch post + user in one query so processJob can access them without an open session
    @Query("SELECT j FROM MediaJob j JOIN FETCH j.post p JOIN FETCH p.user WHERE j.status = :status ORDER BY j.createdAt ASC")
    List<MediaJob> findPendingWithPost(@Param("status") MediaJobStatus status);
}
