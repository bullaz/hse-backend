package com.stellarix.hse.service;

import java.time.LocalDateTime;
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

    private static final int MAX_ATTEMPTS  = 5;
    private static final int LOCK_MINUTES  = 15;

    private final HseRepository hseRepository;
    private final PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return hseRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .map(UserInfoDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
    }

    public Hse findByUsernameOrEmail(String value) {
        return hseRepository.findByUsernameOrEmail(value, value)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + value));
    }

    public Optional<Hse> findByUsernameOrEmailOptional(String value) {
        return hseRepository.findByUsernameOrEmail(value, value);
    }

    public List<Hse> findAllNonAdmin() {
        return hseRepository.findByIsAdminFalseOrderByNomAscPrenomAsc();
    }

    public boolean isAccountLocked(Hse user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    public long minutesUntilUnlock(Hse user) {
        if (user.getLockedUntil() == null) return 0;
        long seconds = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    public void recordFailedAttempt(Hse user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            log.warn("Account locked after {} failed attempts: {}", attempts, user.getEmail());
        }
        hseRepository.save(user);
    }

    public void resetFailedAttempts(Hse user) {
        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            hseRepository.save(user);
        }
    }

    public void addUser(Hse hseUser) {
        validatePassword(hseUser.getPassword());
        hseUser.setPassword(encoder.encode(hseUser.getPassword()));
        hseRepository.save(hseUser);
    }

    public void changePassword(Hse user, String currentPassword, String newPassword) {
        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        validatePassword(newPassword);
        user.setPassword(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        hseRepository.save(user);
    }

    public void disableUser(Hse user) {
        user.setActive(false);
        hseRepository.save(user);
    }

    public void enableUser(Hse user) {
        user.setActive(true);
        hseRepository.save(user);
    }

    public void resetTotp(Hse user) {
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        hseRepository.save(user);
    }

    public void saveUser(Hse user) {
        hseRepository.save(user);
    }

    public boolean emailExists(String email) {
        return hseRepository.existsByEmail(email);
    }

    public boolean usernameExists(String username) {
        return hseRepository.existsByUsername(username);
    }

    public Hse findById(Integer id) {
        return hseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 12)
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 12 caractères");
        if (!password.matches(".*[A-Z].*"))
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins une majuscule");
        if (!password.matches(".*[0-9].*"))
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins un chiffre");
        if (!password.matches(".*[^a-zA-Z0-9].*"))
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins un caractère spécial");
    }
}
