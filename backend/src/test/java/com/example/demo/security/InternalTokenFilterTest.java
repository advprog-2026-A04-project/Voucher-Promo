package com.example.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalTokenFilterTest {

    @Test
    void doFilter_checkoutPath_withValidToken_allowsRequest() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/vouchers/validate");
        request.addHeader("X-Internal-Token", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_checkoutPath_missingToken_returns401() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/vouchers/claim");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verifyNoInteractions(chain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("missing or invalid internal token");
    }

    @Test
    void doFilter_nonCheckoutPath_skipsFilter() throws Exception {
        InternalTokenFilter filter = new InternalTokenFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/vouchers/active");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
