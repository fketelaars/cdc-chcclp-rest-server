package com.ibm.cdc.chcclp.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Registers Spring's {@link CommonsRequestLoggingFilter} so that every inbound
 * HTTP request (method, URI, query string, client IP, headers, and body) is
 * written to the log at DEBUG level.
 *
 * <p>Logging is gated by the logger level for
 * {@code org.springframework.web.filter.CommonsRequestLoggingFilter}, which is
 * set to DEBUG in {@code application.properties}. In production you can silence
 * the output by bumping that logger back to INFO without touching this class.
 */
@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);   // remote host + session id
        filter.setIncludeQueryString(true);  // ?foo=bar appended to URI
        filter.setIncludeHeaders(true);      // all request headers
        filter.setIncludePayload(true);      // request body
        filter.setMaxPayloadLength(10_000);  // cap body dump at 10 kB
        return filter;
    }
}
