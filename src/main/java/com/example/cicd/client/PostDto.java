package com.example.cicd.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** What the consumer (frontend / other service) expects from GET /posts/{id}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PostDto(Long id, String title, String body) {
}
