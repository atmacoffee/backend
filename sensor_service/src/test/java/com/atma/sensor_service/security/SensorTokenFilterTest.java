package com.atma.sensor_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.atma.sensor_service.client.AuthValidationClient;
import com.fasterxml.jackson.databind.ObjectMapper;

class SensorTokenFilterTest {

    @Mock
    private AuthValidationClient authValidationClient;

    private SensorTokenFilter sensorTokenFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sensorTokenFilter = new SensorTokenFilter(
                authValidationClient,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void rejectsMissingAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sensor/latest");
        MockHttpServletResponse response = new MockHttpServletResponse();

        sensorTokenFilter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void returnsServiceUnavailableWhenAuthValidationDown() throws Exception {
        when(authValidationClient.validate("jwt-token"))
                .thenReturn(new AuthValidationClient.ValidationResult(false, true));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sensor/latest");
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        sensorTokenFilter.doFilter(request, response, new MockFilterChain());

        assertEquals(503, response.getStatus());
    }
}
