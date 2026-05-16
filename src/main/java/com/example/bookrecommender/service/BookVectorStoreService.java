package com.example.bookrecommender.service;

import com.example.bookrecommender.model.BookDocument;
import com.example.bookrecommender.model.BookInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookVectorStoreService {
    private final RdfBookService rdfBookService;

    public BookVectorStoreService(RdfBookService rdfBookService) {
        this.rdfBookService = rdfBookService;
    }

    public List<BookDocument> allDocuments() {
        List<BookInfo> books = rdfBookService.findAllBooks();
        List<BookDocument> documents = new ArrayList<>();

        for (BookInfo book : books) {
            BookDocument document = toDocument(book);
            documents.add(document);
        }

        return documents;
    }

    public List<BookDocument> search(String query, int limit) {
        List<BookDocument> documents = allDocuments();
        List<ScoredBookDocument> scoredDocuments = new ArrayList<>();

        for (BookDocument document : documents) {
            int score = score(query, document);

            if (score > 0) {
                scoredDocuments.add(new ScoredBookDocument(document, score));
            }
        }

        scoredDocuments.sort(
                Comparator.comparingInt(ScoredBookDocument::getScore).reversed()
                        .thenComparing(scored -> scored.getDocument().getTitle(), String.CASE_INSENSITIVE_ORDER)
        );

        List<BookDocument> result = new ArrayList<>();

        for (ScoredBookDocument scoredDocument : scoredDocuments) {
            if (result.size() >= limit) {
                break;
            }

            result.add(scoredDocument.getDocument());
        }

        return result;
    }

    private BookDocument toDocument(BookInfo book) {
        String text = buildText(book);

        return new BookDocument(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getThemes(),
                book.getReadingLevel(),
                book.getDescription(),
                book.getRecommendedUsers(),
                text
        );
    }

    private String buildText(BookInfo book) {
        StringBuilder builder = new StringBuilder();

        builder.append("Book title: ").append(nullSafe(book.getTitle())).append("\n");
        builder.append("Author: ").append(nullSafe(book.getAuthor())).append("\n");
        builder.append("Themes: ").append(String.join(", ", book.getThemes())).append("\n");
        builder.append("Reading level: ").append(nullSafe(book.getReadingLevel())).append("\n");
        builder.append("Description: ").append(nullSafe(book.getDescription())).append("\n");

        if (!book.getRecommendedUsers().isEmpty()) {
            builder.append("Recommended users: ")
                    .append(String.join(", ", book.getRecommendedUsers()))
                    .append("\n");
        }

        return builder.toString();
    }

    private int score(String query, BookDocument document) {
        Set<String> queryTokens = tokenize(query);
        Set<String> documentTokens = tokenize(document.getText());

        int score = 0;

        for (String token : queryTokens) {
            if (documentTokens.contains(token)) {
                score++;
            }
        }

        score += exactFieldBonus(query, document);

        return score;
    }

    private int exactFieldBonus(String query, BookDocument document) {
        String normalizedQuery = normalize(query);
        int bonus = 0;

        if (!nullSafe(document.getTitle()).isBlank()
                && normalizedQuery.contains(normalize(document.getTitle()))) {
            bonus += 5;
        }

        if (!nullSafe(document.getAuthor()).isBlank()
                && normalizedQuery.contains(normalize(document.getAuthor()))) {
            bonus += 4;
        }

        if (!nullSafe(document.getReadingLevel()).isBlank()
                && normalizedQuery.contains(normalize(document.getReadingLevel()))) {
            bonus += 3;
        }

        for (String theme : document.getThemes()) {
            if (!theme.isBlank() && normalizedQuery.contains(normalize(theme))) {
                bonus += 3;
            }
        }

        return bonus;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        String normalizedText = normalize(text);

        for (String part : normalizedText.split("\\s+")) {
            String token = part.trim();

            if (!token.isBlank() && token.length() > 2) {
                tokens.add(token);
            }
        }

        return tokens;
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

    private static class ScoredBookDocument {
        private final BookDocument document;
        private final int score;

        private ScoredBookDocument(BookDocument document, int score) {
            this.document = document;
            this.score = score;
        }

        public BookDocument getDocument() {
            return document;
        }

        public int getScore() {
            return score;
        }
    }
}