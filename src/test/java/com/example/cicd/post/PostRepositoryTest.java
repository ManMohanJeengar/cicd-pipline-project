package com.example.cicd.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Assignment 3 - Repository tested against a REAL PostgreSQL started by Testcontainers. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    PostRepository repository;

    @Test
    void savesAndFindsPost() {
        Post saved = repository.save(new Post("Hello", "World"));

        assertNotNull(saved.getId());
        assertEquals("Hello", repository.findById(saved.getId()).orElseThrow().getTitle());
    }

    @Test
    void findsByTitleIgnoringCase() {
        repository.save(new Post("Docker Basics", "..."));
        repository.save(new Post("Kubernetes", "..."));

        List<Post> result = repository.findByTitleContainingIgnoreCase("docker");

        assertEquals(1, result.size());
        assertEquals("Docker Basics", result.get(0).getTitle());
    }
}
