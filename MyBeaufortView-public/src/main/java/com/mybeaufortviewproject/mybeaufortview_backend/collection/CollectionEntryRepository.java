package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;



public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, Long> {

    long countByCollection_Id(Long collectionId);

    Page<CollectionEntry> findByCollection_IdOrderByAddedAtDesc(Long collectionId, Pageable pageable);

    Optional<CollectionEntry> findFirstByCollection_IdOrderByAddedAtDesc(Long collectionId);

    boolean existsByCollection_IdAndPost_Id(Long collectionId, Long postId);

    @Modifying
    @Transactional
    void deleteByCollection_IdAndPost_Id(Long collectionId, Long postId);

    @Modifying
    @Transactional
    void deleteByCollection_Id(Long collectionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CollectionEntry ce WHERE ce.post.id = :postId")
    void deleteByPost_Id(@Param("postId") Long postId);

    // -- Bulk: counts per collection ---
    @Query("""
            SELECT ce.collection.id, count(ce)
            FROM CollectionEntry ce
            WHERE ce.collection.id in :collectionIds
            GROUP BY ce.collection.id
            """)
    List<Object[]> countByCollectionIds(@Param("collectionIds") List<Long> collectionIds);

    // --- Bulk: cover image per collection (latest addedAt) ---
    @Query("""
            SELECT e.collection.id, e.post.imageUrl
            FROM CollectionEntry e
            WHERE e.collection.id IN :collectionIds
                AND e.id = (
                    SELECT MAX(e2.id)
                    FROM CollectionEntry e2
                    WHERE e2.collection.id = e.collection.id
                    AND e2.addedAt = (
                         SELECT MAX(e3.addedAt)
                         FROM CollectionEntry e3
                         WHERE e3.collection.id = e.collection.id
                     )
                )
            """)
    List<Object[]> coversByCollectionIds(@Param("collectionIds") List<Long> collectionIds);

    // Note: addedAt is preferred over id because it expresses the meaning of "when it was added to a collection"
}
