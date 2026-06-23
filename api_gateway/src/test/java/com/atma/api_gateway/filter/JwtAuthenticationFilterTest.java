package com.atma.api_gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.atma.api_gateway.client.AuthValidationClient;
import com.atma.api_gateway.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthValidationClient authValidationClient;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtUtil,
                authValidationClient,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        when(jwtUtil.validateToken("expired-token")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sensor/latest");
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsRevokedTokenEvenWhenJwtSignatureIsValid() throws Exception {
        when(jwtUtil.validateToken("revoked-token")).thenReturn(true);
        when(authValidationClient.validate("revoked-token"))
                .thenReturn(new AuthValidationClient.ValidationResult(false, false));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sensor/latest");
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void failsClosedWhenAuthValidationServiceUnavailable() throws Exception {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(authValidationClient.validate("valid-token"))
                .thenReturn(new AuthValidationClient.ValidationResult(false, true));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sensor/latest");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        assertEquals(503, response.getStatus());
    }
}
