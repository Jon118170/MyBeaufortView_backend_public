package com.mybeaufortviewproject.mybeaufortview_backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import com.mybeaufortviewproject.mybeaufortview_backend.common.web.CorrelationIdFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

@ActiveProfiles("test")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    public void whenHeaderMissing_generatesAndSetsResponseHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();

        // MDC should be cleared after request
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();

        verify(chain, times(1)).doFilter(any(), any(HttpServletResponse.class));
    }

    @Test
    public void whenHeaderProvided_echoesSameValue() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "client-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("client-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();

        verify(chain, times(1)).doFilter(any(), any(HttpServletResponse.class));
    }
}
