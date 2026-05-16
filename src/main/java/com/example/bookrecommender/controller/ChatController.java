package com.example.bookrecommender.controller;

import com.example.bookrecommender.model.ChatRequest;
import com.example.bookrecommender.model.ChatResponse;
import com.example.bookrecommender.model.ConversationStarterResponse;
import com.example.bookrecommender.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat/message")
    public ChatResponse message(@RequestBody ChatRequest request) {
        return chatService.answer(request);
    }

    @GetMapping("/api/chat/starters")
    public ConversationStarterResponse starters(
            @RequestParam(value = "pageType", required = false) String pageType,
            @RequestParam(value = "bookId", required = false) String bookId
    ) {
        return chatService.starters(pageType, bookId);
    }
}