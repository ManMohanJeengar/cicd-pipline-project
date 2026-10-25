package com.example.cicd.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Assignment 2 - API tests with MockMvc (HTTP layer only, service is mocked). */
@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PostService postService;

    @Test
    void testGetPosts() throws Exception {
        when(postService.getAll()).thenReturn(List.of(new Post(1L, "Hello", "World")));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Hello"));
    }

    @Test
    void testGetPostById() throws Exception {
        when(postService.getById(1L)).thenReturn(new Post(1L, "Hello", "World"));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.body").value("World"));
    }

    @Test
    void testGetUnknownPostReturns404() throws Exception {
        when(postService.getById(99L)).thenThrow(new PostNotFoundException(99L));

        mockMvc.perform(get("/posts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreatePost() throws Exception {
        when(postService.create("Hello", "World")).thenReturn(new Post(5L, "Hello", "World"));

        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hello\",\"body\":\"World\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void testCreatePostWithBlankTitleReturns400() throws Exception {
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"body\":\"World\"}"))
                .andExpect(status().isBadRequest());
    }
}
