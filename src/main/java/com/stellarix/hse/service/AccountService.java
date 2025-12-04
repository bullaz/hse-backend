package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.repository.HseRepository;

import lombok.AllArgsConstructor;

@Service
public class AccountService implements UserDetailsService{
	
    private final PasswordEncoder encoder;
	private final HseRepository hseRepository;
	
	@Autowired
    public AccountService(HseRepository repository, PasswordEncoder encoder) {
        this.hseRepository = repository;
        this.encoder = encoder;
    }

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
		List<String> roles = new ArrayList<>();
		roles.add("hse");
		Optional<Hse> account = hseRepository.findByUsernameOrEmail(usernameOrEmail);
		
		if (account.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
        }
        
        Hse user = account.get();
        return new UserInfoDetails(user);
	}
	
	
    public String addUser(Hse hseUser) throws Exception{
        hseUser.setPassword(encoder.encode(hseUser.getPassword())); 
        hseRepository.save(hseUser);
        return "User added successfully!";
    }

}
