package com.example.demo;

import com.example.demo.db.Book;
import com.example.demo.db.BookRepository;
import com.example.demo.google.GoogleBook;
import com.example.demo.google.GoogleBookService;
import com.example.demo.google.GoogleVolume;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
public class BookController {
    private final BookRepository bookRepository;
    private final GoogleBookService googleBookService;

    @Autowired
    public BookController(BookRepository bookRepository, GoogleBookService googleBookService) {
        this.bookRepository = bookRepository;
        this.googleBookService = googleBookService;
    }

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @PostMapping("/books/{googleId}")
    public ResponseEntity<Book> addBookFromGoogle(@PathVariable("googleId") String googleId) {
        GoogleVolume volume;
        try {
            volume = googleBookService.getVolumeById(googleId);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid googleId or upstream error", ex);
        }

        if (volume == null || volume.volumeInfo() == null || volume.volumeInfo().title() == null || volume.volumeInfo().title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required upstream data");
        }

        String id = (volume.id() != null && !volume.id().isBlank()) ? volume.id() : googleId;
        String title = volume.volumeInfo().title();
        String author = null;
        if (volume.volumeInfo().authors() != null && !volume.volumeInfo().authors().isEmpty()) {
            author = volume.volumeInfo().authors().get(0);
        }
        Integer pageCount = volume.volumeInfo().pageCount();

        Book persisted = bookRepository.save(new Book(id, title, author, pageCount));
        return ResponseEntity.status(HttpStatus.CREATED).body(persisted);
    }

    @GetMapping("/google")
    public GoogleBook searchGoogleBooks(@RequestParam("q") String query,
                                        @RequestParam(value = "maxResults", required = false) Integer maxResults,
                                        @RequestParam(value = "startIndex", required = false) Integer startIndex) {
        return googleBookService.searchBooks(query, maxResults, startIndex);
    }
}
