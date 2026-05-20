package com.stellarix.hse.security;

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

    public UserInfoDetails(Hse userInfo) {
        this.username = userInfo.getEmail();
        this.password = userInfo.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority("HSE"));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                     { return password; }
    @Override public String getUsername()                                     { return username; }
    @Override public boolean isAccountNonExpired()                            { return true; }
    @Override public boolean isAccountNonLocked()                             { return true; }
    @Override public boolean isCredentialsNonExpired()                        { return true; }
    @Override public boolean isEnabled()                                      { return true; }
}
