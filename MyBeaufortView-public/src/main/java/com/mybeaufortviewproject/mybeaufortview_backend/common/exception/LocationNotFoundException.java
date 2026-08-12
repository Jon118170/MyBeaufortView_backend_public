package com.mybeaufortviewproject.mybeaufortview_backend.common.exception;

import org.springframework.http.HttpStatus;

public class LocationNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public LocationNotFoundException(Long id) {
        super(
              ErrorCode.LOCATION_NOT_FOUND,
              HttpStatus.NOT_FOUND,
              "Location not found with id: " + id
        );
    }

    public LocationNotFoundException(String slug) {
        super(
                ErrorCode.LOCATION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Location not found with slug: " + slug
        );
    }
}
