package com.lifestrat.service;

import com.lifestrat.entity.User;
import com.lifestrat.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Найти пользователя по ID
    public Optional<User> findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }

    // Найти пользователя по имени
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    // Найти пользователя по email
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    // Проверить существование пользователя по имени
    public Boolean existsByUsername(String username) {
        log.debug("Checking if user exists by username: {}", username);
        return userRepository.existsByUsername(username);
    }

    // Проверить существование пользователя по email
    public Boolean existsByEmail(String email) {
        log.debug("Checking if user exists by email: {}", email);
        return userRepository.existsByEmail(email);
    }

    // Сохранить пользователя
    public User save(User user) {
        log.debug("Saving user with username: {}", user.getUsername());
        User savedUser = userRepository.save(user);
        log.info("User saved successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    // Создать нового пользователя с хешированием пароля
    public User createUser(String username, String email, String password) {
        log.debug("Creating new user with username: {}", username);

        // Проверяем, не существует ли уже пользователь с таким username или email
        if (existsByUsername(username)) {
            log.warn("User with username '{}' already exists", username);
            throw new IllegalArgumentException("User with username '" + username + "' already exists");
        }

        if (existsByEmail(email)) {
            log.warn("User with email '{}' already exists", email);
            throw new IllegalArgumentException("User with email '" + email + "' already exists");
        }

        // Хешируем пароль
        String encodedPassword = passwordEncoder.encode(password);

        // Создаем нового пользователя
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodedPassword);

        User savedUser = save(user);
        log.info("User created successfully with ID: {} and username: {}", savedUser.getId(), username);
        return savedUser;
    }

    // Удалить пользователя
    public void delete(User user) {
        log.debug("Deleting user with ID: {} and username: {}", user.getId(), user.getUsername());
        userRepository.delete(user);
        log.info("User deleted successfully with ID: {}", user.getId());
    }

    // Проверить пароль
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // Хешировать пароль
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}