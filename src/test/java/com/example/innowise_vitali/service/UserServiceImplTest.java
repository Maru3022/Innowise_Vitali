package com.example.innowise_vitali.service;

import com.example.innowise_vitali.dto.PaymentCardRequestDto;
import com.example.innowise_vitali.dto.PaymentCardResponseDto;
import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.dto.UserResponseDto;
import com.example.innowise_vitali.entity.PaymentCard;
import com.example.innowise_vitali.entity.Role;
import com.example.innowise_vitali.entity.User;
import com.example.innowise_vitali.exception.UserAlreadyExistsException;
import com.example.innowise_vitali.exception.UserNotFoundException;
import com.example.innowise_vitali.mapper.PaymentCardMapper;
import com.example.innowise_vitali.mapper.UserMapper;
import com.example.innowise_vitali.repository.PaymentCardRepository;
import com.example.innowise_vitali.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PaymentCardRepository cardRepository;
    @Mock private UserMapper userMapper;
    @Mock private PaymentCardMapper cardMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRequestDto requestDto;
    private User user;
    private UserResponseDto responseDto;
    private PaymentCard paymentCard;
    private PaymentCardRequestDto cardRequestDto;
    private PaymentCardResponseDto cardResponseDto;

    @BeforeEach
    void setUp() {
        requestDto = UserRequestDto.builder()
                .username("vitali")
                .password("password123")
                .name("Vitali")
                .surname("Ivanov")
                .email("vitali@example.com")
                .birthDate(LocalDate.of(1995, 5, 20))
                .build();

        user = User.builder()
                .id(1L)
                .username("vitali")
                .name("Vitali")
                .surname("Ivanov")
                .email("vitali@example.com")
                .birthDate(LocalDate.of(1995, 5, 20))
                .passwordHash("hashed")
                .role(Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        responseDto = UserResponseDto.builder()
                .id(1L)
                .username("vitali")
                .name("Vitali")
                .surname("Ivanov")
                .email("vitali@example.com")
                .role(Role.USER)
                .isActive(true)
                .build();

        paymentCard = PaymentCard.builder()
                .id(1L)
                .cardNumber("1234567812345678")
                .cardHolder("VITALI IVANOV")
                .expirationDate("12/29")
                .isActive(true)
                .user(user)
                .build();

        cardRequestDto = new PaymentCardRequestDto();
        cardResponseDto = new PaymentCardResponseDto();
    }

    @Test
    void createUser_success() {
        when(userRepository.existsByUsernameNative(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userMapper.toEntity(any(), anyString())).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toResponseDto(any())).thenReturn(responseDto);

        UserResponseDto result = userService.createUser(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("vitali");
        verify(userRepository).save(any());
    }

    @Test
    void createUser_throwsWhenUsernameExists() {
        when(userRepository.existsByUsernameNative("vitali")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(requestDto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("username");
    }

    @Test
    void createUser_throwsWhenEmailExists() {
        when(userRepository.existsByUsernameNative(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("vitali@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(requestDto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDto(user)).thenReturn(responseDto);

        UserResponseDto result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_throwsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUser_success() {
        UserRequestDto updateRequest = UserRequestDto.builder()
                .name("NewName").surname("NewSurname")
                .email("newemail@example.com")
                .password("newpassword")
                .birthDate(LocalDate.of(1995, 5, 20))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("new_hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponseDto(any(User.class))).thenReturn(responseDto);
        when(cacheManager.getCache("users")).thenReturn(cache);

        UserResponseDto result = userService.updateUser(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_throwsWhenEmailAlreadyTaken() {
        UserRequestDto updateRequest = UserRequestDto.builder()
                .email("taken@example.com")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");
    }

    @Test
    void deactivateUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cacheManager.getCache("users")).thenReturn(cache);

        userService.deactivateUser(1L);

        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void activateUser_success() {
        user.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cacheManager.getCache("users")).thenReturn(cache);

        userService.activateUser(1L);

        assertThat(user.getIsActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void addCardToUser_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(2L);
        when(cardMapper.toEntity(any())).thenReturn(paymentCard);
        when(cardRepository.save(any())).thenReturn(paymentCard);
        when(cardMapper.toResponseDto(any())).thenReturn(cardResponseDto);

        PaymentCardResponseDto result = userService.addCardToUser(1L, cardRequestDto);

        assertThat(result).isNotNull();
        verify(cardRepository).save(paymentCard);
    }

    @Test
    void addCardToUser_throwsWhenLimitReached() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.countByUserIdAndIsActiveTrue(1L)).thenReturn(5L);

        assertThatThrownBy(() -> userService.addCardToUser(1L, cardRequestDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5");
    }

    @Test
    void isCardOwner_returnsTrueWhenMatches() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(paymentCard));

        assertThat(userService.isCardOwner(1L, "vitali")).isTrue();
    }

    @Test
    void isCardOwner_returnsFalseWhenNotMatches() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(paymentCard));

        assertThat(userService.isCardOwner(1L, "wrong_user")).isFalse();
    }
}
