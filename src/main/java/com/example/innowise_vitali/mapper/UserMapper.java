package com.example.innowise_vitali.mapper;

import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.dto.UserResponseDto;
import com.example.innowise_vitali.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "passwordHash", source = "encodedPassword")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "paymentCards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    User toEntity(UserRequestDto dto, String encodedPassword);

    UserResponseDto toResponseDto(User user);
}