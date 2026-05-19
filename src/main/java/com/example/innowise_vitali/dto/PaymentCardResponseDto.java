package com.example.innowise_vitali.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCardResponseDto {

    private Long id;
    private String cardNumber;
    private String expirationDate;
    private String cardHolder;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
