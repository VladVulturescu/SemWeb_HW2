document.addEventListener("DOMContentLoaded", function () {
    const contextElement = document.getElementById("chat-context");

    const pageType = contextElement?.dataset.pageType || "books-list";
    const bookId = contextElement?.dataset.bookId || "";

    const chatRoot = document.createElement("div");
    chatRoot.className = "chat-root";

    chatRoot.innerHTML = `
        <button class="chat-toggle" id="chatToggle" type="button">💬 Chat</button>

        <section class="chat-window hidden" id="chatWindow">
            <div class="chat-header">
                <div>
                    <strong>Book Assistant</strong>
                    <small>RDF-based chatbot</small>
                </div>
                <button class="chat-close" id="chatClose" type="button">×</button>
            </div>

            <div class="chat-starters" id="chatStarters"></div>

            <div class="chat-messages" id="chatMessages">
                <div class="chat-message assistant">
                    Hello! Ask me about books, authors, themes, reading levels, or recommendations from the RDF database.
                </div>
            </div>

            <form class="chat-form" id="chatForm">
                <input
                    id="chatInput"
                    type="text"
                    placeholder="Ask about a book..."
                    autocomplete="off"
                >
                <button type="submit">Send</button>
            </form>
        </section>
    `;

    document.body.appendChild(chatRoot);

    const chatToggle = document.getElementById("chatToggle");
    const chatWindow = document.getElementById("chatWindow");
    const chatClose = document.getElementById("chatClose");
    const chatMessages = document.getElementById("chatMessages");
    const chatForm = document.getElementById("chatForm");
    const chatInput = document.getElementById("chatInput");
    const chatStarters = document.getElementById("chatStarters");

    chatToggle.addEventListener("click", function () {
        chatWindow.classList.remove("hidden");
        chatToggle.classList.add("hidden");
        chatInput.focus();
    });

    chatClose.addEventListener("click", function () {
        chatWindow.classList.add("hidden");
        chatToggle.classList.remove("hidden");
    });

    chatForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const message = chatInput.value.trim();

        if (!message) {
            return;
        }

        chatInput.value = "";
        addMessage(message, "user");

        await sendMessage(message);
    });

    loadStarters();

    async function loadStarters() {
        try {
            const params = new URLSearchParams();
            params.append("pageType", pageType);

            if (bookId) {
                params.append("bookId", bookId);
            }

            const response = await fetch(`/api/chat/starters?${params.toString()}`);

            if (!response.ok) {
                return;
            }

            const data = await response.json();
            renderStarters(data.starters || []);
        } catch (error) {
            console.error("Could not load chat starters", error);
        }
    }

    function renderStarters(starters) {
        chatStarters.innerHTML = "";

        if (starters.length === 0) {
            return;
        }

        const title = document.createElement("p");
        title.className = "chat-starters-title";
        title.textContent = "Try asking:";
        chatStarters.appendChild(title);

        starters.forEach(function (starter) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "chat-starter-button";
            button.textContent = starter;

            button.addEventListener("click", async function () {
                addMessage(starter, "user");
                await sendMessage(starter);
            });

            chatStarters.appendChild(button);
        });
    }

    async function sendMessage(message) {
        const loadingMessage = addMessage("Thinking using RDF data...", "assistant");

        try {
            const response = await fetch("/api/chat/message", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    message: message,
                    pageType: pageType,
                    bookId: bookId || null
                })
            });

            if (!response.ok) {
                loadingMessage.textContent = "The chat service returned an error.";
                return;
            }

            const data = await response.json();
            loadingMessage.textContent = data.answer || "No answer was returned.";

            if (data.retrievedBooks && data.retrievedBooks.length > 0) {
                const sources = document.createElement("div");
                sources.className = "chat-sources";
                sources.textContent = "Retrieved books: " + data.retrievedBooks.join(", ");
                loadingMessage.appendChild(sources);
            }
        } catch (error) {
            console.error("Could not send chat message", error);
            loadingMessage.textContent = "Could not connect to the chat service.";
        }
    }

    function addMessage(text, sender) {
        const messageElement = document.createElement("div");
        messageElement.className = "chat-message " + sender;
        messageElement.textContent = text;

        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;

        return messageElement;
    }
});