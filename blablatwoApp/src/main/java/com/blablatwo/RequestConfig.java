package com.blablatwo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true); // This is crucial for logging the request body
        filter.setMaxPayloadLength(10000); // Set a reasonable max length for the payload
        filter.setIncludeHeaders(false); // Optional: Set to true to include headers
        filter.setAfterMessagePrefix("REQUEST DATA : "); // Prefix for the logged message
        return filter;
    }
}
