package com.stellarix.hse.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.stellarix.hse.entity.Hse;

public class UserInfoDetails implements UserDetails {

    private static final long serialVersionUID = -2388600789544794553L;

    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;
    private final boolean accountNonLocked;
    private final boolean enabled;

    public UserInfoDetails(Hse user) {
        this.username       = user.getEmail();
        this.password       = user.getPassword();
        this.enabled        = user.isActive();
        this.accountNonLocked = user.getLockedUntil() == null
                || user.getLockedUntil().isBefore(LocalDateTime.now());

        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("HSE"));
        if (user.isAdmin()) auths.add(new SimpleGrantedAuthority("HSE_ADMIN"));
        this.authorities = List.copyOf(auths);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                     { return password; }
    @Override public String getUsername()                                     { return username; }
    @Override public boolean isAccountNonExpired()                            { return true; }
    @Override public boolean isAccountNonLocked()                             { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired()                        { return true; }
    @Override public boolean isEnabled()                                      { return enabled; }
}
