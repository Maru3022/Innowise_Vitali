package com.example.innowise_vitali.exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(
            Long id
    ){
        super("User not found with id: " + id);
    }

    public UserNotFoundException(
            String field,
            String value){
        super("User not found with field: " + field + " and value: " + value);
    }
}
