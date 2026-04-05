package com.finance.dashboard.dto.response;

import com.finance.dashboard.enums.Role;

public class AuthResponse {

    private String token;
    private String tokenType;
    private String email;
    private String name;
    private Role role;
    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, String email, String name, Role role) {
        this.token = token;
        this.tokenType = tokenType;
        this.email = email;
        this.name = name;
        this.role = role;
    }



    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}