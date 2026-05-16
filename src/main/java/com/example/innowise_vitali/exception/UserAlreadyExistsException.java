package com.example.innowise_vitali.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(
            String field,
            String value
    ) {
        super("User already exists with field: " + field + " and value: " + value);
    }
}
