package com.blablatwo.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfig {
    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> additionalConnector(
            @Value("${server.http.port}") int httpPort
    ) {
        return (tomcat) -> {
            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setPort(httpPort);
            tomcat.addAdditionalConnectors(connector);
        };
    }
}

