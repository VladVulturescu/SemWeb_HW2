package com.example.bookrecommender.service;

import com.example.bookrecommender.model.BookForm;
import com.example.bookrecommender.model.BookInfo;
import com.example.bookrecommender.model.TripleDto;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RdfBookService {
    public static final String NS = "http://example.org/book-recommender#";
    private static final String BOOK_BASE = "http://example.org/book-recommender/book/";
    private static final String USER_BASE = "http://example.org/book-recommender/user/";
    private static final String THEME_BASE = "http://example.org/book-recommender/theme/";
    private static final String LEVEL_BASE = "http://example.org/book-recommender/level/";
    private static final String AUTHOR_BASE = "http://example.org/book-recommender/author/";

    private final Path rdfPath = findRdfPath();

    private final Resource bookClass = resource("Book");
    private final Resource userClass = resource("User");
    private final Resource themeClass = resource("Theme");
    private final Resource readingLevelClass = resource("ReadingLevel");
    private final Resource authorClass = resource("Author");

    private final Property title = property("title");
    private final Property description = property("description");
    private final Property hasTheme = property("hasTheme");
    private final Property themeName = property("themeName");
    private final Property suitableForLevel = property("suitableForLevel");
    private final Property levelName = property("levelName");
    private final Property hasAuthor = property("hasAuthor");
    private final Property authorName = property("authorName");
    private final Property userName = property("userName");
    private final Property prefersTheme = property("prefersTheme");
    private final Property hasReadingLevel = property("hasReadingLevel");

    public List<BookInfo> findAllBooks() {
        Model model = loadModel();
        List<BookInfo> books = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        model.listSubjectsWithProperty(title).forEachRemaining(bookResource -> {
            if (bookResource.isURIResource() && seen.add(bookResource.getURI())) {
                books.add(toBookInfo(model, bookResource));
            }
        });

        books.sort(Comparator.comparing(BookInfo::getTitle, String.CASE_INSENSITIVE_ORDER));
        return books;
    }

    public Optional<BookInfo> findBookById(String id) {
        Model model = loadModel();
        Resource bookResource = model.getResource(BOOK_BASE + id);

        if (!model.contains(bookResource, title)) {
            return Optional.empty();
        }

        return Optional.of(toBookInfo(model, bookResource));
    }

    private Path findRdfPath() {
        Path nestedProjectFile = Path.of("semantic-book-recommender/src/main/resources/data/books.rdf");

        if (Files.exists(nestedProjectFile)) {
            return nestedProjectFile;
        }

        return Path.of("src/main/resources/data/books.rdf");
    }

    public BookForm toForm(BookInfo book) {
        BookForm form = new BookForm();
        form.setId(book.getId());
        form.setTitle(book.getTitle());
        form.setAuthor(book.getAuthor());
        form.setThemes(String.join(", ", book.getThemes()));
        form.setReadingLevel(book.getReadingLevel());
        form.setDescription(book.getDescription());
        return form;
    }

    public void addBook(BookForm form) {
        String id = slug(form.getTitle());
        form.setId(id);
        saveBook(form);
    }

    public void updateBook(String id, BookForm form) {
        form.setId(id);
        saveBook(form);
    }

    public List<TripleDto> triplesFromStoredModel() {
        return triplesFromModel(loadModel());
    }

    public List<TripleDto> triplesFromUploadedFile(InputStream inputStream) {
        Model model = ModelFactory.createDefaultModel();
        model.read(inputStream, null, "RDF/XML");
        return triplesFromModel(model);
    }

    private void saveBook(BookForm form) {
        Model model = loadModel();
        String bookId = safeId(form.getId(), form.getTitle());
        Resource book = model.createResource(BOOK_BASE + bookId);

        model.removeAll(book, null, null);
        model.add(book, RDF.type, bookClass);
        model.add(book, title, clean(form.getTitle()));
        model.add(book, description, clean(form.getDescription()));

        String authorText = clean(form.getAuthor());
        if (!authorText.isBlank()) {
            Resource author = model.createResource(AUTHOR_BASE + slug(authorText));
            model.add(author, RDF.type, authorClass);
            model.removeAll(author, authorName, null);
            model.add(author, authorName, authorText);
            model.add(book, hasAuthor, author);
        }

        String levelText = clean(form.getReadingLevel());
        if (!levelText.isBlank()) {
            Resource level = model.createResource(LEVEL_BASE + slug(levelText));
            model.add(level, RDF.type, readingLevelClass);
            model.removeAll(level, levelName, null);
            model.add(level, levelName, levelText);
            model.add(book, suitableForLevel, level);
        }

        for (String themeText : splitCommaValues(form.getThemes())) {
            Resource theme = model.createResource(THEME_BASE + slug(themeText));
            model.add(theme, RDF.type, themeClass);
            model.removeAll(theme, themeName, null);
            model.add(theme, themeName, themeText);
            model.add(book, hasTheme, theme);
        }

        saveModel(model);
    }

    private BookInfo toBookInfo(Model model, Resource bookResource) {
        BookInfo book = new BookInfo();
        book.setId(lastPart(bookResource.getURI()));
        book.setTitle(literal(model, bookResource, title));
        book.setDescription(literal(model, bookResource, description));

        Statement authorStatement = model.getProperty(bookResource, hasAuthor);
        if (authorStatement != null && authorStatement.getObject().isResource()) {
            Resource author = authorStatement.getObject().asResource();
            book.setAuthor(literal(model, author, authorName));
        }

        Statement levelStatement = model.getProperty(bookResource, suitableForLevel);
        if (levelStatement != null && levelStatement.getObject().isResource()) {
            Resource level = levelStatement.getObject().asResource();
            book.setReadingLevel(literal(model, level, levelName));
        }

        List<String> themes = new ArrayList<>();
        model.listStatements(bookResource, hasTheme, (RDFNode) null).forEachRemaining(statement -> {
            if (statement.getObject().isResource()) {
                Resource theme = statement.getObject().asResource();
                themes.add(literal(model, theme, themeName));
            }
        });
        themes.sort(String.CASE_INSENSITIVE_ORDER);
        book.setThemes(themes);

        book.setRecommendedUsers(findRecommendedUsers(model, bookResource));
        return book;
    }

    private List<String> findRecommendedUsers(Model model, Resource bookResource) {
        Set<String> bookThemes = new HashSet<>();
        model.listStatements(bookResource, hasTheme, (RDFNode) null).forEachRemaining(statement -> {
            if (statement.getObject().isResource()) {
                bookThemes.add(statement.getObject().asResource().getURI());
            }
        });

        Statement bookLevelStatement = model.getProperty(bookResource, suitableForLevel);
        if (bookLevelStatement == null || !bookLevelStatement.getObject().isResource()) {
            return List.of();
        }
        String bookLevelUri = bookLevelStatement.getObject().asResource().getURI();

        List<String> users = new ArrayList<>();
        model.listResourcesWithProperty(RDF.type, userClass).forEachRemaining(user -> {
            Statement userLevelStatement = model.getProperty(user, hasReadingLevel);
            Statement preferredThemeStatement = model.getProperty(user, prefersTheme);

            if (userLevelStatement == null || preferredThemeStatement == null) {
                return;
            }
            if (!userLevelStatement.getObject().isResource() || !preferredThemeStatement.getObject().isResource()) {
                return;
            }

            String userLevelUri = userLevelStatement.getObject().asResource().getURI();
            String userThemeUri = preferredThemeStatement.getObject().asResource().getURI();

            if (bookLevelUri.equals(userLevelUri) && bookThemes.contains(userThemeUri)) {
                users.add(literal(model, user, userName));
            }
        });

        users.sort(String.CASE_INSENSITIVE_ORDER);
        return users;
    }

    private Model loadModel() {
        try {
            Model model = ModelFactory.createDefaultModel();
            if (Files.exists(rdfPath)) {
                try (InputStream inputStream = Files.newInputStream(rdfPath)) {
                    model.read(inputStream, null, "RDF/XML");
                }
            }
            model.setNsPrefix("ex", NS);
            return model;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load RDF file: " + rdfPath.toAbsolutePath(), e);
        }
    }

    private void saveModel(Model model) {
        try {
            Files.createDirectories(rdfPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(rdfPath)) {
                model.write(outputStream, "RDF/XML-ABBREV");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not save RDF file: " + rdfPath.toAbsolutePath(), e);
        }
    }

    private List<TripleDto> triplesFromModel(Model model) {
        List<TripleDto> triples = new ArrayList<>();
        model.listStatements().forEachRemaining(statement -> {
            String subjectText = shortName(statement.getSubject());
            String predicateText = shortUri(statement.getPredicate().getURI());
            String objectText = objectText(statement.getObject());
            triples.add(new TripleDto(subjectText, predicateText, objectText));
        });
        triples.sort(Comparator.comparing(TripleDto::getSubject).thenComparing(TripleDto::getPredicate));
        return triples;
    }

    private String literal(Model model, Resource resource, Property property) {
        Statement statement = model.getProperty(resource, property);
        if (statement == null || !statement.getObject().isLiteral()) {
            return "";
        }
        Literal value = statement.getObject().asLiteral();
        return value.getString();
    }

    private List<String> splitCommaValues(String text) {
        List<String> values = new ArrayList<>();
        if (text == null) {
            return values;
        }
        for (String part : text.split(",")) {
            String value = clean(part);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String safeId(String id, String titleText) {
        if (id != null && !id.isBlank()) {
            return slug(id);
        }
        return slug(titleText);
    }

    private String slug(String text) {
        String cleaned = clean(text).toLowerCase();
        cleaned = cleaned.replaceAll("[^a-z0-9]+", "-");
        cleaned = cleaned.replaceAll("^-+", "");
        cleaned = cleaned.replaceAll("-+$", "");
        if (cleaned.isBlank()) {
            return "unnamed";
        }
        return cleaned;
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.trim();
    }

    private Resource resource(String localName) {
        return ModelFactory.createDefaultModel().createResource(NS + localName);
    }

    private Property property(String localName) {
        return ModelFactory.createDefaultModel().createProperty(NS + localName);
    }

    private String objectText(RDFNode node) {
        if (node.isLiteral()) {
            return node.asLiteral().getString();
        }
        if (node.isResource()) {
            return shortName(node.asResource());
        }
        return node.toString();
    }

    private String shortName(Resource resource) {
        if (resource.getURI() == null) {
            return resource.toString();
        }
        return shortUri(resource.getURI());
    }

    private String shortUri(String uri) {
        if (uri == null) {
            return "";
        }
        if (uri.contains("#")) {
            return uri.substring(uri.lastIndexOf('#') + 1);
        }
        return lastPart(uri);
    }

    private String lastPart(String uri) {
        if (uri == null) {
            return "";
        }
        int slash = uri.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < uri.length()) {
            return uri.substring(slash + 1);
        }
        return uri;
    }
}
