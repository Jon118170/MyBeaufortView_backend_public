package com.mybeaufortviewproject.mybeaufortview_backend.post;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByUser_IdAndDescription(Long userId, String description);

    Optional<Post> findByDescription (String description);

    Optional<Post> findByImageUrl(String imageUrl);

    List<Post> findByDescriptionContainingIgnoreCase(String keyword, Sort sort);

    List<Post> findByUser_Id(Long userId);

    @Query("""
            SELECT DISTINCT p
            FROM Post p
            LEFT JOIN PostTag pt ON pt.post = p
            WHERE LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(pt.tag) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Post> searchByDescriptionOrTag(@Param("keyword") String keyword, Sort sort);

    @Query("""
            SELECT pt2.post
            FROM PostTag pt1
            JOIN PostTag pt2 ON pt1.tag = pt2.tag
            WHERE pt1.post.id = :postId
                AND pt2.post.id <> :postId
            GROUP BY pt2.post
            ORDER BY COUNT(pt2.tag) DESC
            """)
    List<Post> findSimilarPosts(@Param("postId") Long postId);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Post> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Post> findByLocation_Slug(String slug, Pageable pageable);

    long countByUser_Id(Long userId);

    long countByLocation_Id(Long locationId);
}
