package com.example.demo.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class CsrfControllerTest {

    @Test
    void csrf_whenTokenMissing_returnsEmptyMap() {
        CsrfController controller = new CsrfController();
        MockHttpServletRequest request = new MockHttpServletRequest();

        Map<String, String> resp = controller.csrf(request);

        assertThat(resp).isEmpty();
    }

    @Test
    void csrf_whenTokenPresent_returnsTokenMetadata() {
        CsrfController controller = new CsrfController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "token123");
        request.setAttribute(CsrfToken.class.getName(), token);

        Map<String, String> resp = controller.csrf(request);

        assertThat(resp).containsEntry("headerName", "X-XSRF-TOKEN");
        assertThat(resp).containsEntry("parameterName", "_csrf");
        assertThat(resp).containsEntry("token", "token123");
    }
}

