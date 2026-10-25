package com.example.cicd.post;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Assignment 1 - Unit tests of the service layer (repository is mocked). */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    PostRepository repository;

    @InjectMocks
    PostService service;

    @Test
    void createTrimsTitleAndSaves() {
        when(repository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post saved = service.create("  Hello  ", "World");

        assertEquals("Hello", saved.getTitle());
        assertEquals("World", saved.getBody());
        verify(repository).save(any(Post.class));
    }

    @Test
    void createWithBlankTitleThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.create("   ", "body"));
        verifyNoInteractions(repository);
    }

    @Test
    void getByIdReturnsPost() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Post(1L, "t", "b")));

        assertEquals("t", service.getById(1L).getTitle());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> service.getById(99L));
    }
}
