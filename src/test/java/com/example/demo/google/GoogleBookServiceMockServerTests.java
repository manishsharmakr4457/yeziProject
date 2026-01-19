package com.example.demo.google;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.hamcrest.Matchers.startsWith;

/**
 * Integration-ish test using Spring's in-process HTTP mocking (no sockets).
 * Serves the JSON from src/test/resources/effectivejava.json.
 */
class GoogleBookServiceMockServerTests {

    private MockRestServiceServer server;
    private GoogleBookService googleBookService;

    @BeforeEach
    void setup() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://example.test");
        server = MockRestServiceServer.bindTo(builder).build();
        googleBookService = new GoogleBookService(builder.build());

        Path path = Paths.get("src", "test", "resources", "effectivejava.json");
        String body = Files.readString(path);

        server.expect(requestTo(startsWith("http://example.test/volumes")))
                .andExpect(method(GET))
                .andExpect(queryParam("q", "effective+java"))
                .andExpect(queryParam("maxResults", "5"))
                .andExpect(queryParam("startIndex", "0"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    void search_mocked_returnsEffectiveJava() {
        GoogleBook result = googleBookService.searchBooks("effective+java", 5, 0);
        assertThat(result).isNotNull();
        assertThat(result.kind()).isEqualTo("books#volumes");
        assertThat(result.items()).isNotEmpty();
        GoogleBook.Item first = result.items().get(0);
        assertThat(first.volumeInfo().title()).isEqualTo("Effective Java");
        server.verify();
    }
}
