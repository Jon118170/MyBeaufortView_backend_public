package com.mybeaufortviewproject.mybeaufortview_backend.location;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.LocationNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.location.dto.LocationResponse;
import com.mybeaufortviewproject.mybeaufortview_backend.post.PostRepository;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    private final PostRepository postRepository;

    public LocationService(LocationRepository locationRepository, PostRepository postRepository) {
        this.locationRepository = locationRepository;
        this.postRepository = postRepository;
    }

    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toLocationResponse)
                .toList();
    }

    public LocationResponse getLocationBySlug(String slug) {
        Location location = getLocationEntityBySlug(slug);
        return toLocationResponse(location);
    }

    public Location getLocationEntityBySlug(String slug) {
        return locationRepository.findBySlug(slug)
                .orElseThrow(() -> new LocationNotFoundException(slug));
    }

    public Location getLocationEntityById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
    }

    private LocationResponse toLocationResponse(Location location) {
        long postCount = postRepository.countByLocation_Id(location.getId());

        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getSlug(),
                location.getLatitude(),
                location.getLongitude(),
                location.getDescription(),
                postCount
        );
    }

}
