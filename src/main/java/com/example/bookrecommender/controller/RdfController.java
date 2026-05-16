package com.example.bookrecommender.controller;

import com.example.bookrecommender.model.TripleDto;
import com.example.bookrecommender.service.RdfBookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class RdfController {

    private final RdfBookService rdfBookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RdfController(RdfBookService rdfBookService) {
        this.rdfBookService = rdfBookService;
    }

    @GetMapping("/rdf/graph")
    public String storedGraph(Model model) throws Exception {
        List<TripleDto> triples = rdfBookService.triplesFromStoredModel();

        model.addAttribute("title", "Current RDF Graph");
        model.addAttribute("triples", triples);
        model.addAttribute("triplesJson", objectMapper.writeValueAsString(triples));

        return "graph";
    }

    @PostMapping("/rdf/upload")
    public String uploadRdf(@RequestParam("file") MultipartFile file, Model model) throws Exception {
        List<TripleDto> triples = rdfBookService.triplesFromUploadedFile(file.getInputStream());

        model.addAttribute("title", "Uploaded RDF Graph: " + file.getOriginalFilename());
        model.addAttribute("triples", triples);
        model.addAttribute("triplesJson", objectMapper.writeValueAsString(triples));

        return "graph";
    }
}