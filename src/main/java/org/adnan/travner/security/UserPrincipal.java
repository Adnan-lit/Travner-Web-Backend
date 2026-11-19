package org.adnan.travner.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authenticated identity used by both JWT and the interim Basic flow.
 * The subject is the user's database ID, never the username.
 */
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String username;
    private final String password;
    private final String email;
    private final List<String> roles;
    private final boolean active;

    public UserPrincipal(String id, String username, String password, String email,
                         List<String> roles, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles == null || roles.isEmpty() ? List.of("USER") : List.copyOf(roles);
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
