package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String principal = email == null ? "" : email.trim();

        User u = repo.findByEmail(principal)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role = u.getRole();
        if (role != null && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        boolean accountNonLocked =
                u.getLockedUntil() == null || u.getLockedUntil().isBefore(OffsetDateTime.now());

        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getPasswordHash(),
                Boolean.TRUE.equals(u.getIsEnabled()),
                true,
                true,
                accountNonLocked,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}