package com.example.innowise_vitali.service;

import com.example.innowise_vitali.dto.PaymentCardRequestDto;
import com.example.innowise_vitali.dto.PaymentCardResponseDto;
import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.dto.UserResponseDto;
import com.example.innowise_vitali.entity.PaymentCard;
import com.example.innowise_vitali.entity.User;
import com.example.innowise_vitali.exception.PaymentCardNotFoundException;
import com.example.innowise_vitali.exception.UserAlreadyExistsException;
import com.example.innowise_vitali.exception.UserNotFoundException;
import com.example.innowise_vitali.mapper.PaymentCardMapper;
import com.example.innowise_vitali.mapper.UserMapper;
import com.example.innowise_vitali.repository.PaymentCardRepository;
import com.example.innowise_vitali.repository.UserRepository;
import com.example.innowise_vitali.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PaymentCardRepository cardRepository;
    private final UserMapper userMapper;
    private final PaymentCardMapper cardMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto request) {
        log.info("Creating user with username='{}' email='{}'", request.getUsername(), request.getEmail());
        if (userRepository.existsByUsernameNative(request.getUsername())) {
            log.warn("Username '{}' already exists", request.getUsername());
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email '{}' already exists", request.getEmail());
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
        User user = userMapper.toEntity(request, passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        log.info("User created: id={} username='{}'", saved.getId(), saved.getUsername());
        return userMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id")
    public UserResponseDto getUserById(Long id) {
        log.debug("Fetching user by id={}", id);
        return userRepository.findById(id)
                .map(userMapper::toResponseDto)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new UserNotFoundException(id);
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#email")
    public UserResponseDto getUserByEmail(String email) {
        log.debug("Fetching user by email='{}'", email);
        return userRepository.findByEmailCustom(email)
                .map(userMapper::toResponseDto)
                .orElseThrow(() -> {
                    log.warn("User not found: email='{}'", email);
                    return new UserNotFoundException("email", email);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(String name, String surname, Pageable pageable) {
        log.debug("Listing users: name='{}' surname='{}' page={} size={}", name, surname, pageable.getPageNumber(), pageable.getPageSize());
        Specification<User> spec = Specification
                .where(UserSpecifications.hasName(name))
                .and(UserSpecifications.hasSurname(surname));
        Page<UserResponseDto> result = userRepository.findAll(spec, pageable).map(userMapper::toResponseDto);
        log.debug("Found {} users (total={})", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto request) {
        log.info("Updating user id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed — user not found: id={}", id);
                    return new UserNotFoundException(id);
                });

        String oldEmail = user.getEmail();
        if (!oldEmail.equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            log.warn("Update failed — email '{}' is already taken", request.getEmail());
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setBirthDate(request.getBirthDate());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        UserResponseDto result = userMapper.toResponseDto(userRepository.save(user));
        evictUserCaches(id, oldEmail);
        evictUserCaches(id, request.getEmail());
        log.info("User updated: id={}", id);
        return result;
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        log.info("Deactivating user id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Deactivate failed — user not found: id={}", id);
                    return new UserNotFoundException(id);
                });
        user.setIsActive(false);
        userRepository.save(user);
        evictUserCaches(id, user.getEmail());
        log.info("User deactivated: id={}", id);
    }

    @Override
    @Transactional
    public void activateUser(Long id) {
        log.info("Activating user id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Activate failed — user not found: id={}", id);
                    return new UserNotFoundException(id);
                });
        user.setIsActive(true);
        userRepository.save(user);
        evictUserCaches(id, user.getEmail());
        log.info("User activated: id={}", id);
    }

    @Override
    @Transactional
    public PaymentCardResponseDto addCardToUser(Long userId, PaymentCardRequestDto cardDto) {
        log.info("Adding card for user id={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Add card failed — user not found: id={}", userId);
                    return new UserNotFoundException(userId);
                });

        long activeCount = cardRepository.countByUserIdAndIsActiveTrue(userId);
        log.debug("User id={} has {} active cards", userId, activeCount);
        if (activeCount >= 5) {
            log.warn("Add card failed — user id={} already has 5 active cards", userId);
            throw new IllegalStateException("User cannot have more than 5 active payment cards");
        }

        PaymentCard card = cardMapper.toEntity(cardDto);
        card.setUser(user);
        card.setIsActive(true);
        PaymentCard saved = cardRepository.save(card);
        log.info("Card added: cardId={} userId={}", saved.getId(), userId);
        return cardMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentCardResponseDto> getUserCards(Long userId) {
        log.debug("Fetching cards for user id={}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("Get cards failed — user not found: id={}", userId);
            throw new UserNotFoundException(userId);
        }
        List<PaymentCardResponseDto> cards = cardRepository.findByUserId(userId).stream()
                .map(cardMapper::toResponseDto)
                .collect(Collectors.toList());
        log.debug("Found {} cards for user id={}", cards.size(), userId);
        return cards;
    }

    @Override
    @Transactional
    public void deactivateCard(Long cardId) {
        log.info("Deactivating card id={}", cardId);
        PaymentCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("Deactivate card failed — card not found: id={}", cardId);
                    return new PaymentCardNotFoundException(cardId);
                });
        card.setIsActive(false);
        cardRepository.save(card);
        log.info("Card deactivated: id={}", cardId);
    }

    @Override
    @Transactional
    public void activateCard(Long cardId) {
        log.info("Activating card id={}", cardId);
        PaymentCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("Activate card failed — card not found: id={}", cardId);
                    return new PaymentCardNotFoundException(cardId);
                });
        card.setIsActive(true);
        cardRepository.save(card);
        log.info("Card activated: id={}", cardId);
    }

    @Transactional(readOnly = true)
    public boolean isCardOwner(Long cardId, String username) {
        log.debug("Checking card ownership: cardId={} username='{}'", cardId, username);
        return cardRepository.findById(cardId)
                .map(card -> card.getUser().getUsername().equals(username))
                .orElse(false);
    }

    private void evictUserCaches(Long id, String email) {
        org.springframework.cache.Cache cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(id);
            if (email != null) {
                cache.evict(email);
            }
        }
    }
}
