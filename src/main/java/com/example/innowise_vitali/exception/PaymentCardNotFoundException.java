package com.example.innowise_vitali.exception;

public class PaymentCardNotFoundException extends RuntimeException {

    public PaymentCardNotFoundException(Long id) {
        super("Payment card not found with id: " + id);
    }
}