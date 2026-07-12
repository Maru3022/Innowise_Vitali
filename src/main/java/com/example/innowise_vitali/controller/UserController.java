package com.example.innowise_vitali.controller;

import com.example.innowise_vitali.dto.PaymentCardRequestDto;
import com.example.innowise_vitali.dto.PaymentCardResponseDto;
import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.dto.UserResponseDto;
import com.example.innowise_vitali.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/internal/by-username/{username}")
    public ResponseEntity<UserResponseDto> getUserByUsernameInternal(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL') or @userServiceImpl.getUserById(#id).username == authentication.name")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or #email == @userServiceImpl.getUserByEmail(#email).email")
    public ResponseEntity<UserResponseDto> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(name, surname, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL') or @userServiceImpl.getUserById(#id).username == authentication.name")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL') or @userServiceImpl.getUserById(#id).username == authentication.name")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cards")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL') or @userServiceImpl.getUserById(#id).username == authentication.name")
    public ResponseEntity<PaymentCardResponseDto> addCard(@PathVariable Long id, @Valid @RequestBody PaymentCardRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addCardToUser(id, request));
    }

    @GetMapping("/{id}/cards")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INTERNAL') or @userServiceImpl.getUserById(#id).username == authentication.name")
    public ResponseEntity<List<PaymentCardResponseDto>> getCards(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserCards(id));
    }

    @PatchMapping("/cards/{cardId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or @userServiceImpl.isCardOwner(#cardId, authentication.name)")
    public ResponseEntity<Void> deactivateCard(@PathVariable Long cardId) {
        userService.deactivateCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/cards/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN') or @userServiceImpl.isCardOwner(#cardId, authentication.name)")
    public ResponseEntity<Void> activateCard(@PathVariable Long cardId) {
        userService.activateCard(cardId);
        return ResponseEntity.noContent().build();
    }
}