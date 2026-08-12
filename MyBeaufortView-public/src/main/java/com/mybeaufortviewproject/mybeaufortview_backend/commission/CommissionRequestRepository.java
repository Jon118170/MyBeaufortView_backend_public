package com.mybeaufortviewproject.mybeaufortview_backend.commission;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRequestRepository extends JpaRepository<CommissionRequest, Long> {

    List<CommissionRequest> findByPhotographerIdOrderByCreatedAtDesc(Long photographerId);

    List<CommissionRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<CommissionRequest> findByIdAndPhotographerId(Long id, Long photographerId);

    Optional<CommissionRequest> findByIdAndRequesterId(Long id, Long requesterId);
}
