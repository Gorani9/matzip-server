package com.matzip.server.domain.admin.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matzip.server.ExpectedStatus;
import com.matzip.server.domain.user.dto.UserDto;
import com.matzip.server.domain.user.model.User;
import com.matzip.server.domain.user.repository.UserRepository;
import com.matzip.server.global.auth.dto.LoginDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminUserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin-password}")
    private String adminPassword;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            UserDto.SignUpRequest signUpRequest = new UserDto.SignUpRequest("admin", adminPassword);
            User user = new User(signUpRequest, passwordEncoder);
            userRepository.save(user.toAdmin());
        }
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private String signUp(String username, String password) throws Exception {
        long beforeUserCount = userRepository.count();
        UserDto.SignUpRequest signUpRequest = new UserDto.SignUpRequest(username, password);
        ResultActions resultActions = mockMvc.perform(post("/api/v1/users/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().is(ExpectedStatus.OK.getStatusCode()));
        long afterUserCount = userRepository.count();
        resultActions.andExpect(header().exists("Authorization"));
        assertThat(afterUserCount).isEqualTo(beforeUserCount + 1);
        signIn(username, password, "NORMAL");
        return resultActions.andReturn().getResponse().getHeader("Authorization");
    }

    private String signIn(String username, String password, String expectedRole) throws Exception {
        long beforeUserCount = userRepository.count();
        LoginDto.LoginRequest signUpRequest = new LoginDto.LoginRequest(username, password);
        ResultActions resultActions = mockMvc.perform(post("/api/v1/users/login/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().is(ExpectedStatus.OK.getStatusCode()));
        long afterUserCount = userRepository.count();
        assertThat(afterUserCount).isEqualTo(beforeUserCount);
        resultActions.andExpect(header().exists("Authorization"))
                .andExpect(content().string(containsString(expectedRole)));
        return resultActions.andReturn().getResponse().getHeader("Authorization");
    }

    private void getUsers(
            MultiValueMap<String, String> parameters, String token, ExpectedStatus expectedStatus
    ) throws Exception {
        long beforeUserCount = userRepository.count();
        ResultActions resultActions = mockMvc.perform(get("/api/v1/admin/users/")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(parameters))
                .andExpect(status().is(expectedStatus.getStatusCode()));
        if (expectedStatus == ExpectedStatus.OK) {
            if (parameters.getOrDefault("withAdmin", List.of()).contains("true")) {
                resultActions.andExpect(jsonPath("$.total_elements").value(beforeUserCount));
            } else {
                Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("id").ascending());
                long count = userRepository.findAllByRoleEquals(pageable, "NORMAL").getTotalElements();
                resultActions.andExpect(jsonPath("$.total_elements").value(count));
            }
        }
        long afterUserCount = userRepository.count();
        assertThat(afterUserCount).isEqualTo(beforeUserCount);
    }

    private void getUserById(String token, Long id, ExpectedStatus expectedStatus) throws Exception {
        long beforeUserCount = userRepository.count();
        ResultActions resultActions = mockMvc.perform(get("/api/v1/admin/users/" + id + "/")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(expectedStatus.getStatusCode()));
        if (expectedStatus == ExpectedStatus.OK) {
            UserDto.Response response = new UserDto.Response(userRepository.findById(id).orElseThrow());
            resultActions.andExpect(content().string(objectMapper.writeValueAsString(response)));
        }
        long afterUserCount = userRepository.count();
        assertThat(afterUserCount).isEqualTo(beforeUserCount);
    }

    private void changeUserStatus(
            MultiValueMap<String, String> parameters, String token, Long id, ExpectedStatus expectedStatus
    ) throws Exception {
        long beforeUserCount = userRepository.count();
        User beforeUser = userRepository.findById(id).orElse(null);
        mockMvc.perform(patch("/api/v1/admin/users/" + id + "/status/")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(parameters))
                .andExpect(status().is(expectedStatus.getStatusCode()));
        if (beforeUser == null)
            return;
        User afterUser = userRepository.findById(id).orElseThrow();
        if (expectedStatus == ExpectedStatus.OK) {
            if (parameters.getOrDefault("activate", List.of()).contains("true")) {
                assertFalse(beforeUser.getActive());
                assertTrue(afterUser.getActive());
            } else {
                assertTrue(beforeUser.getActive());
                assertFalse(afterUser.getActive());
            }
        } else {
            assertThat(beforeUser.getRole()).isEqualTo(afterUser.getRole());
        }
        long afterUserCount = userRepository.count();
        assertThat(afterUserCount).isEqualTo(beforeUserCount);
    }

    private void deleteUser(String token, Long id, ExpectedStatus expectedStatus) throws Exception {
        long beforeUserCount = userRepository.count();
        mockMvc.perform(delete("/api/v1/admin/users/" + id + "/")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(expectedStatus.getStatusCode()));
        long afterUserCount = userRepository.count();
        if (expectedStatus == ExpectedStatus.OK) {
            assertTrue(userRepository.findById(id).isEmpty());
            assertThat(afterUserCount).isEqualTo(beforeUserCount - 1);
        } else if (expectedStatus != ExpectedStatus.FORBIDDEN) {
            if (expectedStatus == ExpectedStatus.NOT_FOUND)
                assertTrue(userRepository.findById(id).isEmpty());
            else
                assertTrue(userRepository.findById(id).isPresent());
            assertThat(afterUserCount).isEqualTo(beforeUserCount);
        }
    }

    private final int pageSize = 15;
    private final int pageNumber = 0;

    private MultiValueMap<String, String> pageParameters() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.put("pageNumber", Collections.singletonList(String.valueOf(pageNumber)));
        parameters.put("pageSize", Collections.singletonList(String.valueOf(pageSize)));
        return parameters;
    }

    private MultiValueMap<String, String> makeParameters(String key, String value) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        if (key != null)
            parameters.put(key, Collections.singletonList(value));
        return parameters;
    }

    @Test
    void accessAdminApiTest() throws Exception {
        String normalToken = signUp(
                "foo", "fooPassword1!");

        getUsers(pageParameters(), normalToken, ExpectedStatus.FORBIDDEN);

        getUserById(normalToken, 0L, ExpectedStatus.FORBIDDEN);
        getUserById(normalToken, 1L, ExpectedStatus.FORBIDDEN);
        getUserById(normalToken, 2L, ExpectedStatus.FORBIDDEN);
        getUserById(normalToken, 100L, ExpectedStatus.FORBIDDEN);

        changeUserStatus(makeParameters("activate", "true"), normalToken, 0L, ExpectedStatus.FORBIDDEN);
        changeUserStatus(makeParameters("activate", "false"), normalToken, 0L, ExpectedStatus.FORBIDDEN);
        changeUserStatus(makeParameters(null, null), normalToken, 0L, ExpectedStatus.FORBIDDEN);

        deleteUser(normalToken, 0L, ExpectedStatus.FORBIDDEN);
        deleteUser(normalToken, 1L, ExpectedStatus.FORBIDDEN);
        deleteUser(normalToken, 2L, ExpectedStatus.FORBIDDEN);
    }

    @Test
    void getUsersTest() throws Exception {
        String adminToken = signIn("admin", adminPassword, "ADMIN");

        signUp("foo1", "fooPassword1!");
        signUp("foo2", "fooPassword1!");
        signUp("foo3", "fooPassword1!");
        signUp("foo4", "fooPassword1!");
        signUp("foo5", "fooPassword1!");
        signUp("foo6", "fooPassword1!");

        MultiValueMap<String, String> parameters = pageParameters();
        getUsers(parameters, adminToken, ExpectedStatus.OK);
        parameters.put("withAdmin", Collections.singletonList("false"));
        getUsers(parameters, adminToken, ExpectedStatus.OK);
        parameters.put("withAdmin", Collections.singletonList("true"));
        getUsers(parameters, adminToken, ExpectedStatus.OK);
        parameters.put("withAdmin", Collections.singletonList("Boolean"));
        getUsers(parameters, adminToken, ExpectedStatus.BAD_REQUEST);
    }

    @Test
    void getUserByIdTest() throws Exception {
        String adminToken = signIn("admin", adminPassword, "ADMIN");

        signUp("foo", "fooPassword1!");
        Long fooId = userRepository.findByUsername("foo").orElseThrow().getId();

        getUserById(adminToken, fooId, ExpectedStatus.OK);
        getUserById(adminToken, -1L, ExpectedStatus.BAD_REQUEST);
        getUserById(adminToken, fooId + 100, ExpectedStatus.NOT_FOUND);
    }

    @Test
    void changeUserStatusTest() throws Exception {
        String adminToken = signIn("admin", adminPassword, "ADMIN");

        signUp("foo", "fooPassword1!");
        Long fooId = userRepository.findByUsername("foo").orElseThrow().getId();

        changeUserStatus(makeParameters("activate", "true"), adminToken, fooId, ExpectedStatus.BAD_REQUEST);
        changeUserStatus(makeParameters("activate", "false"), adminToken, fooId, ExpectedStatus.OK);
        changeUserStatus(makeParameters("activate", "false"), adminToken, fooId, ExpectedStatus.BAD_REQUEST);
        changeUserStatus(makeParameters("activate", "true"), adminToken, fooId, ExpectedStatus.OK);
        changeUserStatus(makeParameters("activate", "true"), adminToken, fooId + 100, ExpectedStatus.NOT_FOUND);
        changeUserStatus(makeParameters("activate", "false"), adminToken, fooId + 100, ExpectedStatus.NOT_FOUND);
        changeUserStatus(makeParameters("activate", "false"), adminToken, fooId + 100, ExpectedStatus.NOT_FOUND);
        changeUserStatus(makeParameters("activate", "true"), adminToken, fooId + 100, ExpectedStatus.NOT_FOUND);
        changeUserStatus(makeParameters(null, null), adminToken, fooId, ExpectedStatus.BAD_REQUEST);

        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        changeUserStatus(makeParameters("activate", "false"), adminToken, adminId, ExpectedStatus.BAD_REQUEST);
    }

    @Test
    void deleteUserTest() throws Exception {
        String adminToken = signIn("admin", adminPassword, "ADMIN");

        signUp("bar", "barPassword1!");
        Long barId = userRepository.findByUsername("bar").orElseThrow().getId();

        deleteUser(adminToken, barId, ExpectedStatus.BAD_REQUEST);
        changeUserStatus(makeParameters("activate", "false"), adminToken, barId, ExpectedStatus.OK);
        deleteUser(adminToken, barId + 100, ExpectedStatus.NOT_FOUND);
        deleteUser(adminToken, barId, ExpectedStatus.OK);
        deleteUser(adminToken, barId, ExpectedStatus.NOT_FOUND);

        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        deleteUser(adminToken, adminId, ExpectedStatus.BAD_REQUEST);
    }


}