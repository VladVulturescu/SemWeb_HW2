# Semantic Book Recommender

Semantic Web Applications Homework 2 project.

## Team members

- Andrei Dragota
- Alberto-George Udrea
- Vlad Alexandru Vulturescu

## Public GitHub repository

https://github.com/VladVulturescu/SemWeb_HW2

## What this project implements

- RDF/XML data file for the book recommendation scenario.
- Spring Boot web app.
- Apache Jena for reading, writing, modifying, and querying RDF data.
- List of all books from RDF.
- Dedicated page for each book.
- Add book feature, tested with `Harry Potter`.
- Edit book feature, tested with `Hunger Games` reading level.
- RDF/XML upload and graph visualization.
- OWL ontology file for the recommendation system.
- Five SPARQL queries in `queries/sparql_owl.txt`.
- Floating chatbot available on all application pages.
- Context-aware conversation starters for the chatbot.
- RDF-grounded RAG chatbot using a local vector-like book retrieval service.
- LLM integration through LM Studio using an OpenAI-compatible local API.
- Deterministic fallback responses when the LLM is disabled or unavailable.

## Chatbot / RAG implementation

The application includes a chatbot for exercise 7. The chatbot is available as a floating chat window over all pages of the website.

The chatbot uses the following flow:

```text
User question
   |  
Floating chat widget
   |
/api/chat/message
   |
ChatService
   |
BookVectorStoreService
   |
Relevant RDF book documents
   |
LlmService
   |
LM Studio local LLM
   |
Answer grounded in RDF data
```

The chatbot does not send the entire RDF/XML file to the LLM on every request. Instead, the application first reads the RDF data using Apache Jena, converts the available books into small textual book documents, retrieves the most relevant documents for the user question, and sends only those retrieved documents to the LLM.

Each retrieved document contains information such as:

```text
Title
Author
Themes
Reading level
Description
Recommended users
```

This means the LLM response is based on the RDF data stored in the project, not on the model's general knowledge.

For example, if the RDF database says that `Harry Potter` was written by `Gigel`, then the chatbot should answer `Gigel` when asked:

```text
Who wrote Harry Potter?
```

This is used as a dumb basic RAG correctness test.

## Chatbot features

### 1. Floating chat window

The chat window is loaded on all main pages of the website:

- `/books`
- `/books/add`
- `/books/{id}`
- `/books/{id}/edit`
- `/rdf/graph`

### 2. Context-aware conversation starters

The chatbot provides three conversation starters depending on the current page.

On the books list page, examples include:

```text
What is a book that I am most likely to enjoy from this list?
Which books are suitable for Beginner readers?
Which Science Fiction books are available?
```

On a book details page, the starters are generated using the current book. For example, on the `Harry Potter` page:

```text
Who wrote Harry Potter?
What themes does Harry Potter have?
Is Harry Potter suitable for Beginner readers?
```

On the RDF graph page, examples include:

```text
What does this RDF graph describe?
Which books are connected to Science Fiction?
Which users have recommended books?
```

### 3. RDF-grounded answers

The chatbot retrieves book data from the RDF model and uses that data as context for the LLM.

Example question:

```text
Which books are suitable for Beginner readers?
```

Example answer:

```text
Based on the RDF context:
- Hunger Games is suitable for Beginner readers.
- Harry Potter is also suitable for Beginner readers.
```

### 4. Author and theme search

The chatbot can answer questions such as:

```text
What book has the author Frank Herbert and the theme Science Fiction?
```

Expected answer:

```text
The matching book is Dune.
```

This case is handled safely using the RDF-derived book data, so the answer is stable during the demo.

### 5. LLM fallback

If the LLM is disabled or unavailable, the application falls back to deterministic RDF-based responses. This keeps the chatbot usable even when LM Studio is not running.

The fallback can be enabled by setting:

```properties
llm.enabled=false
```

## Main backend components added for the chatbot

### Models

- `ChatRequest` — request body for chatbot messages.
- `ChatResponse` — response body containing the chatbot answer and retrieved books.
- `ConversationStarterResponse` — response body for context-aware conversation starters.
- `BookDocument` — RDF book data converted into a retrieval-friendly document.

### Services

- `BookVectorStoreService` — builds a simple local vector-like index from RDF book data.
- `ChatService` — coordinates retrieval, special RDF queries, LLM calls, and fallback responses.
- `LlmService` — calls LM Studio through the local OpenAI-compatible API.

### Controller

- `ChatController` — exposes the chatbot API endpoints.

## Chatbot API endpoints

### Send a chat message

```text
POST /api/chat/message
```

Example request:

```json
{
  "message": "Which books are suitable for Beginner readers?",
  "pageType": "books-list",
  "bookId": null
}
```

Example response:

```json
{
  "answer": "Based on the RDF context:\n\n* Hunger Games is suitable for Beginner readers.\n* Harry Potter is also suitable for Beginner readers.",
  "retrievedBooks": ["Hunger Games", "Harry Potter", "Dune"]
}
```

### Get conversation starters

```text
GET /api/chat/starters?pageType=books-list
```

Example response:

```json
{
  "starters": [
    "What is a book that I am most likely to enjoy from this list?",
    "Which books are suitable for Beginner readers?",
    "Which Science Fiction books are available?"
  ]
}
```

For a book details page:

```text
GET /api/chat/starters?pageType=book-details&bookId=harry-potter
```

## LM Studio setup

The chatbot was tested with LM Studio using the model:

```text
llama-3.2-3b-instruct
```

### 1. Start LM Studio

Open LM Studio and load the model.

### 2. Start the local server

In LM Studio, open the local server / developer section and start the server.

The server should be reachable at:

```text
http://127.0.0.1:1234
```

The OpenAI-compatible chat completions endpoint is:

```text
http://127.0.0.1:1234/v1/chat/completions
```

### 3. Test LM Studio directly

Run:

```bash
curl http://127.0.0.1:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama-3.2-3b-instruct",
    "messages": [
      {
        "role": "system",
        "content": "Answer briefly."
      },
      {
        "role": "user",
        "content": "Say hello in one short sentence."
      }
    ],
    "temperature": 0.2,
    "max_tokens": 80,
    "stream": false
  }'
```

If LM Studio works correctly, the response should contain a `choices[0].message.content` field.

## Spring Boot LLM configuration

Create or update:

```text
src/main/resources/application.properties
```

For LM Studio, use:

```properties
llm.enabled=true
llm.api-url=http://127.0.0.1:1234/v1/chat/completions
llm.api-key=
llm.model=llama-3.2-3b-instruct
```

If LM Studio is not running and you still want to use the chatbot fallback, use:

```properties
llm.enabled=false
```

## How to run

1. Open the project folder in IntelliJ IDEA.
2. Make sure Project SDK is Java 21.
3. Let Maven import dependencies.
4. Start LM Studio and load `llama-3.2-3b-instruct`.
5. Start the LM Studio local server on `127.0.0.1:1234`.
6. Check `src/main/resources/application.properties`.
7. Run `BookRecommenderApplication`.
8. Open `http://localhost:8080/books`.

Alternatively, from terminal:

```bash
mvn clean compile
mvn spring-boot:run
```

## Main pages

- `/books` — list all books.
- `/books/add` — add Harry Potter.
- `/books/hunger-games/edit` — change Hunger Games reading level.
- `/books/{id}` — dedicated page for one book.
- `/rdf/graph` — visualize current RDF graph.
- upload form on `/books` — upload an RDF/XML file and visualize it.

## Manual demo checklist

### 1. Books list page

Open:

```text
http://localhost:8080/books
```

Open the chatbot and ask:

```text
Which books are suitable for Beginner readers?
```

Expected result: the answer should be based on retrieved RDF book data, for example `Hunger Games` and `Harry Potter` if both are marked as Beginner.

### 2. Author and theme question

Ask:

```text
What book has the author Frank Herbert and the theme Science Fiction?
```

Expected result:

```text
The matching book is Dune.
```

### 3. Book details page starters

Open:

```text
http://localhost:8080/books/harry-potter
```

The chatbot should show starters about `Harry Potter`, such as:

```text
Who wrote Harry Potter?
What themes does Harry Potter have?
Is Harry Potter suitable for Beginner readers?
```

### 4. RAG correctness test

Edit `Harry Potter` and change the author to:

```text
Gigel
```

Then ask:

```text
Who wrote Harry Potter?
```

Expected result:

```text
Harry Potter was written by Gigel.
```

This demonstrates that the chatbot uses RDF data instead of the LLM's general knowledge.

### 5. RDF graph page

Open:

```text
http://localhost:8080/rdf/graph
```

Ask:

```text
What does this RDF graph describe?
```

Ask:

```text
Which books are connected to Science Fiction?
```

Ask:

```text
Which users have recommended books?
```

Expected result: answers should be based on the RDF-derived book context.

## Notes

- The vector database used in this project is a simple local vector-like retrieval service built from RDF book data.
- The purpose is to demonstrate the RAG pipeline required by the homework without sending the full RDF/XML file to the LLM on every request.
- LM Studio must be running when `llm.enabled=true`.
- When `llm.enabled=false`, the application still provides deterministic RDF-based chatbot responses.