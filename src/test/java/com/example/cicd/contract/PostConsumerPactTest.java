package com.example.cicd.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.example.cicd.client.PostClient;
import com.example.cicd.client.PostDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract testing - CONSUMER side.
 * Runs against a Pact mock server and writes the contract to target/pacts/.
 * The provider test (PostProviderPactTest) then verifies the real backend against it.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "post-service", pactVersion = PactSpecVersion.V3)
class PostConsumerPactTest {

    @Pact(consumer = "post-frontend")
    public RequestResponsePact existingPost(PactDslWithProvider builder) {
        return builder
                .given("post 1 exists")
                .uponReceiving("a request for post 1")
                .path("/posts/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 1)
                        .stringType("title", "Hello")
                        .stringType("body", "World"))
                .toPact();
    }

    @Pact(consumer = "post-frontend")
    public RequestResponsePact missingPost(PactDslWithProvider builder) {
        return builder
                .given("post 999 does not exist")
                .uponReceiving("a request for post 999")
                .path("/posts/999")
                .method("GET")
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "existingPost")
    void clientReadsExistingPost(MockServer mockServer) {
        PostClient client = new PostClient(mockServer.getUrl());

        PostDto post = client.findPost(1).orElseThrow();

        assertEquals(1L, post.id());
        assertEquals("Hello", post.title());
        assertEquals("World", post.body());
    }

    @Test
    @PactTestFor(pactMethod = "missingPost")
    void clientHandlesMissingPost(MockServer mockServer) {
        PostClient client = new PostClient(mockServer.getUrl());

        Optional<PostDto> post = client.findPost(999);

        assertTrue(post.isEmpty());
    }
}
