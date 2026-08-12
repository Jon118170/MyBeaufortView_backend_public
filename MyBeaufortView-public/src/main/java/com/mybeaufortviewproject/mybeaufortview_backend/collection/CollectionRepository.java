package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;

public interface CollectionRepository extends JpaRepository<Collection, Long> {

    Page<Collection> findByUser_Id(Long userId, Pageable pageable);
    Page<Collection> findByVisibility(CollectionVisibility visibility, Pageable pageable);

    Page<Collection> findByUser_IdAndVisibility(Long userId, CollectionVisibility visibility, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page <Post> findByUser_Id(Long userId, CollectionVisibility visibility, Pageable pageable);
    long countByUser_Id(Long userId);
}
