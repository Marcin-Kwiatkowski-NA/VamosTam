package com.blablatwo.search;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@EnableConfigurationProperties(SearchProperties.class)
public class SearchConfigController {

    private final SearchProperties searchProperties;

    public SearchConfigController(SearchProperties searchProperties) {
        this.searchProperties = searchProperties;
    }

    @GetMapping("/search/config")
    public ResponseEntity<SearchConfigDto> getSearchConfig() {
        var sm = searchProperties.smartMatch();
        var dto = new SearchConfigDto(
                sm.radiusKm(),
                sm.maxSegments(),
                sm.refreshCooldownSeconds()
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(dto);
    }
}
