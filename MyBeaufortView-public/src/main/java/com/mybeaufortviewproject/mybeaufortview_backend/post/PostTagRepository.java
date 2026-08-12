package com.mybeaufortviewproject.mybeaufortview_backend.post;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    List<PostTag> findByPost_Id(Long postId);

    List<PostTag> findByPost_IdIn(List<Long> postIds);

    @Modifying
    @Query("DELETE FROM PostTag pt WHERE pt.post.id = :postId")
    public void deleteAllByPostId(@Param("postId") Long postId);
}
