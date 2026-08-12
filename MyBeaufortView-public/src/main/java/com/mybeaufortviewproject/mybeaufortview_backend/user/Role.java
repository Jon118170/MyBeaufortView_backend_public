package com.mybeaufortviewproject.mybeaufortview_backend.user;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Role {
    ADMIN,
    PRIVILEGED_USER;

    @JsonCreator
     public static Role fromString(String role) {
        if (role == null) {
            return null;
        }
        return Role.valueOf(role.toUpperCase()); // ensures "privileged_user" or "PRIVILEGED_USER" works
        }
    }
