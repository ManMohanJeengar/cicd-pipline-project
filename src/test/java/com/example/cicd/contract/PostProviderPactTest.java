package com.example.cicd.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.example.cicd.post.Post;
import com.example.cicd.post.PostController;
import com.example.cicd.post.PostNotFoundException;
import com.example.cicd.post.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

/**
 * Contract testing - PROVIDER side.
 * Replays every interaction from target/pacts/ against the real PostController
 * and fails the build if the backend no longer matches what the consumer expects
 * (e.g. backend renames "title" to "heading").
 */
@WebMvcTest(PostController.class)
@Provider("post-service")
@PactFolder("target/pacts")
class PostProviderPactTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PostService postService;

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("post 1 exists")
    void post1Exists() {
        when(postService.getById(1L)).thenReturn(new Post(1L, "Hello", "World"));
    }

    @State("post 999 does not exist")
    void post999DoesNotExist() {
        when(postService.getById(999L)).thenThrow(new PostNotFoundException(999L));
    }
}
