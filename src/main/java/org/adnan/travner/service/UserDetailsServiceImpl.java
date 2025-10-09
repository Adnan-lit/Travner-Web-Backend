package org.adnan.travner.service;

import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntry user = userRepository.findByuserName(username);
        if (user != null && user.isActive()) {
            // Update last login time (async to not slow down authentication)
            try {
                userService.updateLastLogin(username);
            } catch (Exception e) {
                // Don't fail authentication if logging fails
            }

            // Fix: Handle null roles list to prevent runtime NullPointerException
            String[] roles = user.getRoles() != null ?
                user.getRoles().toArray(new String[0]) :
                new String[]{"USER"};

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUserName())
                    .password(user.getPassword())
                    .roles(roles)
                    .disabled(!user.isActive())
                    .build();
            return userDetails;
        }
        throw new UsernameNotFoundException("User Not Found With UserName: " + username);
    }
}
