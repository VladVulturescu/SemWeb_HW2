package com.example.bookrecommender.controller;

import com.example.bookrecommender.model.BookForm;
import com.example.bookrecommender.model.BookInfo;
import com.example.bookrecommender.service.RdfBookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class BookController {
    private final RdfBookService rdfBookService;

    public BookController(RdfBookService rdfBookService) {
        this.rdfBookService = rdfBookService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/books";
    }

    @GetMapping("/books")
    public String books(Model model) {
        List<BookInfo> books = rdfBookService.findAllBooks();
        model.addAttribute("books", books);
        return "books";
    }

    @GetMapping("/books/{id}")
    public String bookDetails(@PathVariable String id, Model model) {
        BookInfo book = rdfBookService.findBookById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
        model.addAttribute("book", book);
        return "book-details";
    }

    @GetMapping("/books/add")
    public String addBookForm(Model model) {
        BookForm form = new BookForm();
        form.setTitle("Harry Potter");
        form.setAuthor("J. K. Rowling");
        form.setThemes("Fantasy, Magic");
        form.setReadingLevel("Beginner");
        form.setDescription("A fantasy novel used here as the homework test example for adding a book.");
        model.addAttribute("bookForm", form);
        model.addAttribute("mode", "add");
        return "book-form";
    }

    @PostMapping("/books/add")
    public String addBook(@ModelAttribute BookForm bookForm) {
        rdfBookService.addBook(bookForm);
        return "redirect:/books";
    }

    @GetMapping("/books/{id}/edit")
    public String editBookForm(@PathVariable String id, Model model) {
        BookInfo book = rdfBookService.findBookById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
        model.addAttribute("bookForm", rdfBookService.toForm(book));
        model.addAttribute("mode", "edit");
        return "book-form";
    }

    @PostMapping("/books/{id}/edit")
    public String editBook(@PathVariable String id, @ModelAttribute BookForm bookForm) {
        rdfBookService.updateBook(id, bookForm);
        return "redirect:/books/" + id;
    }
}
