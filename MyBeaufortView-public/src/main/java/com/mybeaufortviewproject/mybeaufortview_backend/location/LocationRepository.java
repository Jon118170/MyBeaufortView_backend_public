package com.mybeaufortviewproject.mybeaufortview_backend.location;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findBySlug(String slug);

    List<Location> findAllByOrderByNameAsc();

    boolean existsBySlug(String slug);
}
