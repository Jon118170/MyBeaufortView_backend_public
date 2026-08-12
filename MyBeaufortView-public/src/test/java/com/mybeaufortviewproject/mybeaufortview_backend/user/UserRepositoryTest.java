package com.mybeaufortviewproject.mybeaufortview_backend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindById() {
        // Arrange: create a valid user with all required fields
         User user = new User("johndoe", "John Doe", "john@example.com", "password123", Role.ADMIN);
            user = userRepository.save(user);

        // Act: retrieve the user by ID
        Optional<User> retrievedUser = userRepository.findById(user.getId());

        // Assert: verify the user was retrieved correctly
        assertTrue(retrievedUser.isPresent(), "User should be present");
        assertEquals("johndoe", retrievedUser.get().getUsername());
        assertEquals("John Doe", retrievedUser.get().getName());
        assertEquals(Role.ADMIN, retrievedUser.get().getRole());

    }

    @Test
    public void testSave() {
        // Arrange: create a valid user with all required fields
        User user = new User("janedoe", "Jane Doe", "jane@example.com", "password0123", Role.PRIVILEGED_USER);
        user = userRepository.save(user);

        // Act:
        User savedUser = userRepository.save(user);

        // Assert: verify the user was saved correctly
        assertEquals("janedoe", savedUser.getUsername());
        assertEquals("Jane Doe", savedUser.getName());
        assertEquals(Role.PRIVILEGED_USER, savedUser.getRole());
        assertTrue(userRepository.findById(savedUser.getId()).isPresent(), "User should be present in repository");
    }

    @Test
    public void testDeleteById() {
        // Arrange: create a valid user with all required fields
        User user = new User("johnsmith", "John Smith", "johnsmith@example.com", "deleteplease", Role.ADMIN);

        user = userRepository.save(user);
        Long userId = user.getId();

        // Act
        userRepository.deleteById(userId);

        // Assert
        assertFalse(userRepository.findById(userId).isPresent(), "User should have been deleted");
    }

    @Test
    public void testFindByEmail() {
        // Arrange: create a valid user with all required fields
        User user = new User("janedoe", "Jane Doe", "janedoe@example.com", "emailpassword", Role.ADMIN);

        userRepository.save(user);

        // Act
        Optional<User> foundUser = userRepository.findByEmail("janedoe@example.com");

        // Assert
        assertTrue(foundUser.isPresent(), "User should be found by email");
        assertEquals("janedoe", foundUser.get().getUsername());
        assertEquals("Jane Doe", foundUser.get().getName());
        assertEquals(Role.ADMIN, foundUser.get().getRole());
    }

}
