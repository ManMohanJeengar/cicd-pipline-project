package com.example.cicd.client;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/** Consumer of the Posts API. The Pact contract is generated from how this client uses the API. */
public class PostClient {

    private final RestClient restClient;

    public PostClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Optional<PostDto> findPost(long id) {
        try {
            return Optional.ofNullable(
                    restClient.get().uri("/posts/{id}", id).retrieve().body(PostDto.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
