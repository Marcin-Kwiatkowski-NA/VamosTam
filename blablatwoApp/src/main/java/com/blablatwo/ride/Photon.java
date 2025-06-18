package com.blablatwo.ride;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.web.client.RestClient;

public class Photon implements Geocoder {
    RestClient restClient;
    @Value("${photon.url}")
    String url;

    public Photon() {
        this.restClient = RestClient.create();
    }

}
