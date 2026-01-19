package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import com.example.demo.google.GoogleVolume;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookControllerAddFromGoogleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GoogleBookService googleBookService;

    @BeforeEach
    void setup() {
        bookRepository.deleteAll();
    }

    @Test
    void postBooks_googleId_happyPath_persistsAndReturns201() throws Exception {
        GoogleBook.VolumeInfo volumeInfo = new GoogleBook.VolumeInfo(
                "Effective Java",
                List.of("Joshua Bloch"),
                null,
                null,
                416,
                null,
                null,
                null,
                "en",
                null,
                null
        );
        ((FakeGoogleBookService) googleBookService).stubVolume("12muzgEACAAJ",
                new GoogleVolume("books#volume", "12muzgEACAAJ", "http://example.test/volumes/12muzgEACAAJ", volumeInfo));

        mockMvc.perform(post("/books/{googleId}", "12muzgEACAAJ"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("12muzgEACAAJ"))
                .andExpect(jsonPath("$.title").value("Effective Java"))
                .andExpect(jsonPath("$.author").value("Joshua Bloch"))
                .andExpect(jsonPath("$.pageCount").value(416));

        assertThat(bookRepository.findAll()).hasSize(1);
        Book persisted = bookRepository.findAll().get(0);
        assertThat(persisted.getId()).isEqualTo("12muzgEACAAJ");
        assertThat(persisted.getTitle()).isEqualTo("Effective Java");
        assertThat(persisted.getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(persisted.getPageCount()).isEqualTo(416);
    }

    @Test
    void postBooks_googleId_upstream404_returns400AndDoesNotPersist() throws Exception {
        ((FakeGoogleBookService) googleBookService).stubFailure("does-not-exist");

        mockMvc.perform(post("/books/{googleId}", "does-not-exist"))
                .andExpect(status().isBadRequest());

        assertThat(bookRepository.findAll()).isEmpty();
    }

    @Test
    void postBooks_googleId_missingUpstreamData_returns400AndDoesNotPersist() throws Exception {
        ((FakeGoogleBookService) googleBookService).stubVolume("x", new GoogleVolume("books#volume", "x", null, null));

        mockMvc.perform(post("/books/{googleId}", "x"))
                .andExpect(status().isBadRequest());

        assertThat(bookRepository.findAll()).isEmpty();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        GoogleBookService googleBookService() {
            return new FakeGoogleBookService();
        }
    }

    static class FakeGoogleBookService extends GoogleBookService {
        private final Map<String, GoogleVolume> volumes = new HashMap<>();
        private final Map<String, RuntimeException> failures = new HashMap<>();

        FakeGoogleBookService() {
            super(RestClient.builder().baseUrl("http://example.test").build());
        }

        void stubVolume(String googleId, GoogleVolume volume) {
            volumes.put(googleId, volume);
        }

        void stubFailure(String googleId) {
            failures.put(googleId, new RuntimeException("upstream failure"));
        }

        @Override
        public GoogleVolume getVolumeById(String googleId) {
            RuntimeException failure = failures.get(googleId);
            if (failure != null) {
                throw failure;
            }
            return volumes.get(googleId);
        }
    }
}

