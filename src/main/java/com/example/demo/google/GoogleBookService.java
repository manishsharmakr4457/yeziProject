package com.example.demo.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class GoogleBookService {
    private final RestClient restClient;

    @Autowired
    public GoogleBookService(@Value("${google.books.base-url:https://www.googleapis.com/books/v1}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    protected GoogleBookService(RestClient restClient) {
        this.restClient = restClient;
    }

    public GoogleBook searchBooks(String query, Integer maxResults, Integer startIndex) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/volumes")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults != null ? maxResults : 10)
                        .queryParam("startIndex", startIndex != null ? startIndex : 0)
                        .build())
                .retrieve()
                .body(GoogleBook.class);
    }

    public GoogleVolume getVolumeById(String googleId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/volumes/{id}")
                        .build(googleId))
                .retrieve()
                .body(GoogleVolume.class);
    }
}

