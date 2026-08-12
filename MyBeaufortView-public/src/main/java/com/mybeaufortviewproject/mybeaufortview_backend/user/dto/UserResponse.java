package com.mybeaufortviewproject.mybeaufortview_backend.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;

public class UserResponse {

    private Long id;
    private String username;
    private String name;
    private String email;

    @JsonProperty("role")
    private Role role;

    //Constructors
    public UserResponse() {}

    public UserResponse(Long id, String username, String name, String email, Role role) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }



}
