package com.example.innowise_vitali.service.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.dto.UserResponseDto;
import com.example.innowise_vitali.entity.User;
import com.example.innowise_vitali.exception.UserAlreadyExistsException;
import com.example.innowise_vitali.exception.UserNotFoundException;
import com.example.innowise_vitali.mapper.UserMapper;
import com.example.innowise_vitali.repository.UserRepository;
import com.example.innowise_vitali.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String CACHE_ID = "users_id";
    private static final String CACHE_EMAIL = "users_email";
    private static final String CACHE_LIST = "users_list";

    @Override
    @Transactional
    @Caching(
            put = @CachePut(value = CACHE_ID, key = "#result.id"),
            evict = @CacheEvict(value = CACHE_LIST, key = "'all'")
    )
    public UserResponseDto createUser(UserRequestDto request) {
        log.info("Creating user {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername()))
            throw new UserAlreadyExistsException("username", request.getUsername());

        if (userRepository.existsByEmail(request.getEmail()))
            throw new UserAlreadyExistsException("email", request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = userMapper.toEntity(request, encodedPassword);
        User saved = userRepository.save(user);

        log.info("User created successfully with id: {}", saved.getId());
        return userMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_ID, key = "#id")
    public UserResponseDto getUserById(Long id) {
        log.info("Fetching user by id {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_EMAIL, key = "#email")
    public UserResponseDto getUserByEmail(String email) {
        log.info("Fetching user by email {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email", email));
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_LIST, key = "'all'") // Исправлено: строка 'all' в одинарных кавычках для SpEL
    public List<UserResponseDto> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    @Caching(
            put = @CachePut(value = CACHE_ID, key = "#id"),
            evict = {
                    @CacheEvict(value = CACHE_EMAIL, allEntries = true),
                    @CacheEvict(value = CACHE_LIST, key = "'all'")
            }
    )
    public UserResponseDto updateUser(Long id, UserRequestDto request) {
        log.info("Updating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!user.getUsername().equals(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        log.info("User with id: {} updated successfully", id);
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_LIST, key = "'all'"),
            @CacheEvict(value = CACHE_EMAIL, allEntries = true),
            @CacheEvict(value = CACHE_ID, key = "#id") // ИСПРАВЛЕНО: удаляем конкретный id, а не все подряд
    })
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        if (!userRepository.existsById(id))
            throw new UserNotFoundException(id);

        userRepository.deleteById(id);
        log.info("User deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_LIST, key = "'all'"),
            @CacheEvict(value = CACHE_EMAIL, allEntries = true),
            @CacheEvict(value = CACHE_ID, key = "#id") // ИСПРАВЛЕНО: обновляем/удаляем конкретный ID из кэша
    })
    public void deactivateUser(Long id) {
        log.info("Deactivating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setIsActive(false);
        log.info("User with id: {} deactivated successfully", id);
    }
}