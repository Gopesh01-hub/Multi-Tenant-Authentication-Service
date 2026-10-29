package com.gopesh.identity.tenant;

import com.gopesh.identity.exception.TenantNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantInterceptorTest {

    @Mock
    private TenantConfigRegistry registry;

    private TenantInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        TenantConfigProperties properties = new TenantConfigProperties();
        properties.setHeaderName("X-Tenant-ID");
        properties.setDefaultTenant("default");

        interceptor = new TenantInterceptor(properties, registry);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should set tenant from header")
    void shouldSetTenantFromHeader() {
        request.addHeader("X-Tenant-ID", "sky-uk");
        when(registry.getConfig("sky-uk"))
                .thenReturn(new TenantConfigProperties.TenantConfig());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(TenantContext.getTenantId()).isEqualTo("sky-uk");
    }

    @Test
    @DisplayName("Should fall back to default tenant when header is missing")
    void shouldFallbackToDefaultTenant() {
        when(registry.getConfig("default"))
                .thenReturn(new TenantConfigProperties.TenantConfig());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(TenantContext.getTenantId()).isEqualTo("default");
    }

    @Test
    @DisplayName("Should throw when tenant is not registered")
    void shouldThrowForUnregisteredTenant() {
        request.addHeader("X-Tenant-ID", "unknown");
        when(registry.getConfig("unknown"))
                .thenThrow(new TenantNotFoundException("unknown"));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    @DisplayName("Should clear tenant context after completion")
    void shouldClearContextAfterCompletion() {
        TenantContext.setTenantId("sky-uk");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(TenantContext.getTenantId()).isNull();
    }
}
