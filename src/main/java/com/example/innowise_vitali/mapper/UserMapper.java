package com.example.innowise_vitali.mapper;

import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.dto.UserResponseDto;
import com.example.innowise_vitali.entity.Role;
import com.example.innowise_vitali.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(
            UserRequestDto dto,
            String encodedPassword) {
        return User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(encodedPassword)
                .role(dto.getRole() != null ? dto.getRole() : Role.USER)
                .isActive(true)
                .build();
    }

    public UserResponseDto toResponseDto(
            User user
    ) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

}
