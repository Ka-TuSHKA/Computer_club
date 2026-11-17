package com.computer.lub.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.computer.lub.backend.entity.Role;
import com.computer.lub.backend.entity.User;
import com.computer.lub.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtFilter jwtFilter;

    // Создаем объекты в setUp, чтобы каждый тест получал чистые экземпляры
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext(); // Гарантируем чистый контекст
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private User dbUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("client");
        user.setPassword("hashed");
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void shouldAuthenticateWithValidToken() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("client");
        when(userRepository.findByUsername("client")).thenReturn(Optional.of(dbUser()));

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = currentAuth();
        assertNotNull(auth);
        assertEquals("client", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
    }

    @Test
    void shouldNotAuthenticateWhenUserNotFound() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(currentAuth());
    }

    @Test
    void shouldSkipInvalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer invalid.fake.token");
        when(jwtUtils.getUsernameFromToken("invalid.fake.token"))
                .thenThrow(new RuntimeException("Невалидный токен"));

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(currentAuth());
    }

    @Test
    void shouldSkipRequestWithoutAuthHeader() throws Exception {
        // Убедимся, что заголовка нет
        assertNull(request.getHeader("Authorization"));

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(currentAuth());
        // Самая важная проверка: jwtUtils не должен был вызываться
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldSkipNonBearerHeader() throws Exception {
        request.addHeader("Authorization", "Basic Y2xpZW50OmNsaWVudA==");

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(currentAuth());
        verifyNoInteractions(jwtUtils);
    }
}
