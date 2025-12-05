package com.stellarix.hse.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.stellarix.hse.entity.Hse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class UserInfoDetails implements UserDetails {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2388600789544794553L;
	
	private String username;
    private String password;
    private List<GrantedAuthority> authorities;

    public UserInfoDetails(Hse userInfo) {
        this.username = userInfo.getEmail();
        this.password = userInfo.getPassword();
        List<String> roles = new ArrayList<String>();
        
        //
        roles.add("hse");
        this.authorities = roles
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
        return true;
    }

	@Override
	public String getPassword() {
		return this.password;
	}
}