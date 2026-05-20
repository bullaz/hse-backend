package com.stellarix.hse.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.repository.HseRepository;
import com.stellarix.hse.security.UserInfoDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements UserDetailsService {

    private final HseRepository hseRepository;
    private final PasswordEncoder encoder;

	@Override
	public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
		List<String> roles = new ArrayList<>();
		roles.add("HSE");
		Optional<Hse> account = hseRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
		
		if (account.isEmpty()) {
			//log.info("no user with that username");
            throw new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
        }
        
        Hse user = account.get();
        
        //log.info("hereeeee");
        
        return new UserInfoDetails(user);
	}
	
	
    public Hse findByUsernameOrEmail(String value) {
        return hseRepository.findByUsernameOrEmail(value, value)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + value));
    }

    public String addUser(Hse hseUser) throws Exception{
        hseUser.setPassword(encoder.encode(hseUser.getPassword())); 
        hseRepository.save(hseUser);
        return "User added successfully!";
    }

}
