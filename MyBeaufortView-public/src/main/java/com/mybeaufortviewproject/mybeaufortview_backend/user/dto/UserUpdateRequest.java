package com.mybeaufortviewproject.mybeaufortview_backend.user.dto;

import com.mybeaufortviewproject.mybeaufortview_backend.user.Role;

public class UserUpdateRequest {

    private String username;
    private String name;
    private String email;
    private String password; // optional
    private Role role;       // optional (admin use)

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
