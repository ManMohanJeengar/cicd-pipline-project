package com.example.cicd.post;

import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(@NotBlank String title, @NotBlank String body) {
}
