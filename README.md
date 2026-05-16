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
- Add book feature, tested with Harry Potter.
- Edit book feature, tested with Hunger Games reading level.
- RDF/XML upload and graph visualization.
- OWL ontology file for the recommendation system.
- Five SPARQL queries in `queries/sparql_owl.txt`.

## To be implemented

The full LLM/vector database chatbot from exercise 7 is not implemented yet.

## How to run

1. Open the project folder in IntelliJ IDEA.
2. Make sure Project SDK is Java 21.
3. Let Maven import dependencies.
4. Run `BookRecommenderApplication`.
5. Open `http://localhost:8080/books`.

## Main pages

- `/books` — list all books.
- `/books/add` — add Harry Potter.
- `/books/hunger-games/edit` — change Hunger Games reading level.
- `/rdf/graph` — visualize current RDF graph.
- upload form on `/books` — upload an RDF/XML file and visualize it.
