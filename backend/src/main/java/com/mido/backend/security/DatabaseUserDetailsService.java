package com.mido.backend.security;

import com.mido.backend.user.entity.User;
import com.mido.backend.user.service.UserService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userService.findByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(Collections.emptyList())
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}

