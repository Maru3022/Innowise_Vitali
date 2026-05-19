package com.example.innowise_vitali.repository;

import com.example.innowise_vitali.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
    List<PaymentCard> findByUserId(Long userId);
    long countByUserIdAndIsActiveTrue(Long userId);
}