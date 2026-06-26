package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public String firebaseLogin(String email, String name, AuthProvider provider, Role role) {
        Optional<User> existing = userRepository.findByEmail(email);

        User user;
        if (existing.isEmpty()) {
            user = new User();
            user.setEmail(email);
            user.setName(name != null && !name.isBlank() ? name : email.split("@")[0]);
            user.setProvider(provider);
            user.setRole(role);
            user = userRepository.save(user);
        } else {
            user = existing.get();

            if ((user.getName() == null || user.getName().isBlank())
                    && name != null
                    && !name.isBlank()) {
                user.setName(name);
            }

            if (provider == AuthProvider.GOOGLE && user.getProvider() != AuthProvider.GOOGLE) {
                user.setProvider(AuthProvider.GOOGLE);
            }

            if (user.getRole() == null) {
                user.setRole(role);
            }

            user = userRepository.save(user);
        }

        return jwtUtil.generateToken(user.getEmail());
    }
}