package com.example.innowise_vitali.integration;

import com.example.innowise_vitali.dto.PaymentCardRequestDto;
import com.example.innowise_vitali.dto.UserRequestDto;
import com.example.innowise_vitali.entity.Role;
import com.example.innowise_vitali.entity.User;
import com.example.innowise_vitali.grpc.AuthGrpcServiceGrpc;
import com.example.innowise_vitali.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cache.type=none",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "grpc.client.auth-service.address=in-process:test",
                "grpc.client.auth-service.negotiationType=PLAINTEXT"
        }
)
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("online_store_users_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    AuthGrpcServiceGrpc.AuthGrpcServiceBlockingStub authStub;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_returnsCreated() throws Exception {
        UserRequestDto req = UserRequestDto.builder()
                .username("testuser").password("password123")
                .name("Test").surname("User")
                .email("test@example.com")
                .birthDate(LocalDate.of(1995, 1, 15))
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_conflict_whenUsernameAlreadyExists() throws Exception {
        userRepository.save(buildUser("testuser", "existing@example.com"));

        UserRequestDto req = UserRequestDto.builder()
                .username("testuser").password("password123")
                .name("Another").surname("Person")
                .email("another@example.com")
                .birthDate(LocalDate.of(1992, 3, 10))
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllUsers_returnsPaginatedResult() throws Exception {
        userRepository.save(buildUser("alice", "alice@example.com"));

        mockMvc.perform(get("/api/v1/users").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUserById_returnsUser() throws Exception {
        User saved = userRepository.save(buildUser("bob", "bob@example.com"));

        mockMvc.perform(get("/api/v1/users/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUserById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deactivateAndActivateUser_works() throws Exception {
        User saved = userRepository.save(buildUser("carol", "carol@example.com"));
        long id = saved.getId();

        mockMvc.perform(patch("/api/v1/users/" + id + "/deactivate"))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/users/" + id + "/activate"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void addCard_returnsCreated() throws Exception {
        User saved = userRepository.save(buildUser("dave", "dave@example.com"));

        PaymentCardRequestDto card = PaymentCardRequestDto.builder()
                .cardNumber("1234567812345678")
                .cardHolder("DAVE SMITH")
                .expirationDate("12/28")
                .build();

        mockMvc.perform(post("/api/v1/users/" + saved.getId() + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").value("1234567812345678"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getCards_returnsList() throws Exception {
        User saved = userRepository.save(buildUser("eve", "eve@example.com"));

        mockMvc.perform(get("/api/v1/users/" + saved.getId() + "/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private User buildUser(String username, String email) {
        return User.builder()
                .username(username)
                .name(username)
                .surname("Test")
                .email(email)
                .birthDate(LocalDate.of(1990, 1, 1))
                .passwordHash(passwordEncoder.encode("pass1234"))
                .role(Role.USER)
                .isActive(true)
                .build();
    }
}
