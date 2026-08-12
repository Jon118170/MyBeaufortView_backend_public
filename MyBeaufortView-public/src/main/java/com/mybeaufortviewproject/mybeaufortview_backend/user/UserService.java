package com.mybeaufortviewproject.mybeaufortview_backend.user;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.BadRequestException;
import com.mybeaufortviewproject.mybeaufortview_backend.common.exception.UserNotFoundException;
import com.mybeaufortviewproject.mybeaufortview_backend.user.dto.UserUpdateRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(UserUpdateRequest request) {
        if (request.getRole() == null) {
            throw new BadRequestException("User role must be provided");
        }

        User user = mapToEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(request.getUsername());
            user.setName(request.getName());
            user.setEmail(request.getEmail());

            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }

            return userRepository.save(user);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }

        userRepository.deleteById(id);
    }

    private User mapToEntity(UserUpdateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());

        return user;
    }
}
