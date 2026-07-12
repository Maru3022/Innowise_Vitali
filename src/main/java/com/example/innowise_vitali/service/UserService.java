package com.example.innowise_vitali.service;

import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.dto.UserResponseDto;
import com.example.innowise_vitali.dto.PaymentCardRequestDto;
import com.example.innowise_vitali.dto.PaymentCardResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserResponseDto createUser(UserRequestDto userRequestDto);
    UserResponseDto getUserById(Long id);
    UserResponseDto getUserByEmail(String email);
    Optional<UserResponseDto> findByUsername(String username);
    Page<UserResponseDto> getAllUsers(String name, String surname, Pageable pageable);
    UserResponseDto updateUser(Long id, UserRequestDto request);
    void deactivateUser(Long id);
    void activateUser(Long id);

    PaymentCardResponseDto addCardToUser(Long userId, PaymentCardRequestDto cardDto);
    List<PaymentCardResponseDto> getUserCards(Long userId);
    void deactivateCard(Long cardId);
    void activateCard(Long cardId);
}