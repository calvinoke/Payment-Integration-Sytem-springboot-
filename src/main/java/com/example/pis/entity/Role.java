package com.example.pis.entity;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Enumeration of user roles in the system.
 * Each role can return its Spring Security authorities.
 */
public enum Role {
    USER,
    ADMIN;

    /**
     * Returns the authorities associated with this role.
     * For example, Role.USER → ["ROLE_USER"].
     */
    public List<GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.name()));
    }
}
