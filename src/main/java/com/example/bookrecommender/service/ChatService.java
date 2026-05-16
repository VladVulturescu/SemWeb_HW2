package com.example.bookrecommender.service;

import com.example.bookrecommender.model.BookDocument;
import com.example.bookrecommender.model.ChatRequest;
import com.example.bookrecommender.model.ChatResponse;
import com.example.bookrecommender.model.ConversationStarterResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {
    private final BookVectorStoreService bookVectorStoreService;
    private final RdfBookService rdfBookService;

    public ChatService(
            BookVectorStoreService bookVectorStoreService,
            RdfBookService rdfBookService
    ) {
        this.bookVectorStoreService = bookVectorStoreService;
        this.rdfBookService = rdfBookService;
    }

    public ChatResponse answer(ChatRequest request) {
        String message = nullSafe(request.getMessage());

        if (message.isBlank()) {
            return new ChatResponse(
                    "Please ask a question about the books from the RDF database.",
                    List.of()
            );
        }

        String normalizedMessage = normalize(message);

        if (asksWhatGraphDescribes(normalizedMessage)) {
            return describeRdfGraph();
        }

        if (asksAboutRecommendedUsers(normalizedMessage)) {
            return describeRecommendedUsers();
        }

        List<BookDocument> authorAndThemeMatches = findBooksByAuthorAndTheme(message);

        if (!authorAndThemeMatches.isEmpty()) {
            return buildMatchingBooksResponse(authorAndThemeMatches);
        }

        List<BookDocument> retrievedDocuments = bookVectorStoreService.search(message, 3);

        if (retrievedDocuments.isEmpty()) {
            return new ChatResponse(
                    "I could not find relevant information in the RDF book database for this question.",
                    List.of()
            );
        }

        return buildSimpleRagResponse(message, retrievedDocuments);
    }

    public ConversationStarterResponse starters(String pageType, String bookId) {
        String safePageType = nullSafe(pageType);
        String safeBookId = nullSafe(bookId);

        if ("book-details".equals(safePageType) && !safeBookId.isBlank()) {
            return rdfBookService.findBookById(safeBookId)
                    .map(book -> new ConversationStarterResponse(List.of(
                            "Who wrote " + book.getTitle() + "?",
                            "What themes does " + book.getTitle() + " have?",
                            "Is " + book.getTitle() + " suitable for " + book.getReadingLevel() + " readers?"
                    )))
                    .orElseGet(() -> defaultStarters());
        }

        if ("graph".equals(safePageType)) {
            return new ConversationStarterResponse(List.of(
                    "What does this RDF graph describe?",
                    "Which books are connected to Science Fiction?",
                    "Which users have recommended books?"
            ));
        }

        if ("book-form".equals(safePageType)) {
            return new ConversationStarterResponse(List.of(
                    "What fields are used to describe a book?",
                    "How is a book stored in the RDF database?",
                    "Which reading levels are available?"
            ));
        }

        return defaultStarters();
    }

    private ConversationStarterResponse defaultStarters() {
        return new ConversationStarterResponse(List.of(
                "What is a book that I am most likely to enjoy from this list?",
                "Which books are suitable for Beginner readers?",
                "Which Science Fiction books are available?"
        ));
    }

    private List<BookDocument> findBooksByAuthorAndTheme(String message) {
        String normalizedMessage = normalize(message);
        List<BookDocument> allDocuments = bookVectorStoreService.allDocuments();
        List<BookDocument> matches = new ArrayList<>();

        for (BookDocument document : allDocuments) {
            boolean authorMatches = !nullSafe(document.getAuthor()).isBlank()
                    && normalizedMessage.contains(normalize(document.getAuthor()));

            boolean themeMatches = false;

            for (String theme : document.getThemes()) {
                if (!nullSafe(theme).isBlank()
                        && normalizedMessage.contains(normalize(theme))) {
                    themeMatches = true;
                    break;
                }
            }

            if (authorMatches && themeMatches) {
                matches.add(document);
            }
        }

        return matches;
    }

    private ChatResponse buildMatchingBooksResponse(List<BookDocument> matches) {
        List<String> titles = titles(matches);

        if (titles.size() == 1) {
            return new ChatResponse(
                    "The matching book is " + titles.get(0) + ".",
                    titles
            );
        }

        return new ChatResponse(
                "The matching books are: " + String.join(", ", titles) + ".",
                titles
        );
    }

    private ChatResponse buildSimpleRagResponse(String message, List<BookDocument> documents) {
        String normalizedMessage = normalize(message);
        BookDocument firstDocument = documents.get(0);
        List<String> retrievedBooks = titles(documents);

        if (asksAboutAuthor(normalizedMessage)) {
            return new ChatResponse(
                    firstDocument.getTitle() + " was written by " + firstDocument.getAuthor() + ".",
                    retrievedBooks
            );
        }

        if (asksAboutThemes(normalizedMessage)) {
            return new ChatResponse(
                    firstDocument.getTitle() + " has the following themes: "
                            + String.join(", ", firstDocument.getThemes()) + ".",
                    retrievedBooks
            );
        }

        if (asksAboutReadingLevel(normalizedMessage)) {
            return new ChatResponse(
                    firstDocument.getTitle() + " is suitable for "
                            + firstDocument.getReadingLevel() + " readers.",
                    retrievedBooks
            );
        }

        if (asksForRecommendation(normalizedMessage)) {
            return new ChatResponse(
                    "Based on the RDF book database, a relevant book is "
                            + firstDocument.getTitle()
                            + ". It matches the query through its author, themes, reading level, or description.",
                    retrievedBooks
            );
        }

        return new ChatResponse(
                "I found relevant RDF book data for: " + String.join(", ", retrievedBooks)
                        + ". The most relevant result is " + firstDocument.getTitle()
                        + ", written by " + firstDocument.getAuthor()
                        + ", with themes "
                        + String.join(", ", firstDocument.getThemes())
                        + " and reading level "
                        + firstDocument.getReadingLevel()
                        + ".",
                retrievedBooks
        );
    }

    private boolean asksWhatGraphDescribes(String normalizedMessage) {
        return normalizedMessage.contains("what does this rdf graph describe")
                || normalizedMessage.contains("what does this graph describe")
                || normalizedMessage.contains("rdf graph describe")
                || normalizedMessage.contains("describe graph")
                || normalizedMessage.contains("what is this graph about");
    }

    private boolean asksAboutRecommendedUsers(String normalizedMessage) {
        return normalizedMessage.contains("which users have recommended books")
                || normalizedMessage.contains("recommended users")
                || normalizedMessage.contains("matching users")
                || normalizedMessage.contains("users have recommended")
                || normalizedMessage.contains("recommendation matches");
    }

    private ChatResponse describeRdfGraph() {
        List<BookDocument> documents = bookVectorStoreService.allDocuments();
        List<String> titles = titles(documents);

        if (titles.isEmpty()) {
            return new ChatResponse(
                    "This RDF graph describes a semantic book recommendation system, but no books are currently available in the RDF database.",
                    List.of()
            );
        }

        return new ChatResponse(
                "This RDF graph describes a semantic book recommendation system. "
                        + "It contains books, authors, themes, reading levels, users, and recommendation relations. "
                        + "The available books in the RDF database are: "
                        + String.join(", ", titles) + ".",
                titles
        );
    }

    private ChatResponse describeRecommendedUsers() {
        List<BookDocument> documents = bookVectorStoreService.allDocuments();
        List<String> bookTitles = new ArrayList<>();
        List<String> recommendationLines = new ArrayList<>();

        for (BookDocument document : documents) {
            bookTitles.add(document.getTitle());

            if (!document.getRecommendedUsers().isEmpty()) {
                recommendationLines.add(
                        document.getTitle()
                                + " is recommended for "
                                + String.join(", ", document.getRecommendedUsers())
                );
            }
        }

        if (recommendationLines.isEmpty()) {
            return new ChatResponse(
                    "No books currently have matching recommended users. "
                            + "A recommendation requires both the user's preferred theme and reading level to match the book.",
                    bookTitles
            );
        }

        return new ChatResponse(
                "The RDF database contains the following book-user recommendation matches: "
                        + String.join("; ", recommendationLines) + ".",
                bookTitles
        );
    }

    private boolean asksAboutAuthor(String normalizedMessage) {
        return normalizedMessage.contains("who wrote")
                || normalizedMessage.contains("author")
                || normalizedMessage.contains("written by");
    }

    private boolean asksAboutThemes(String normalizedMessage) {
        return normalizedMessage.contains("theme")
                || normalizedMessage.contains("genre");
    }

    private boolean asksAboutReadingLevel(String normalizedMessage) {
        return normalizedMessage.contains("reading level")
                || normalizedMessage.contains("level")
                || normalizedMessage.contains("beginner")
                || normalizedMessage.contains("intermediate")
                || normalizedMessage.contains("advanced");
    }

    private boolean asksForRecommendation(String normalizedMessage) {
        return normalizedMessage.contains("recommend")
                || normalizedMessage.contains("enjoy")
                || normalizedMessage.contains("like")
                || normalizedMessage.contains("suitable");
    }

    private List<String> titles(List<BookDocument> documents) {
        List<String> titles = new ArrayList<>();

        for (BookDocument document : documents) {
            titles.add(document.getTitle());
        }

        return titles;
    }

    private String normalize(String text) {
        return nullSafe(text)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String nullSafe(String text) {
        if (text == null) {
            return "";
        }

        return text;
    }
}