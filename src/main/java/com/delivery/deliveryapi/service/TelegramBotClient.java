package com.delivery.deliveryapi.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.delivery.deliveryapi.config.TelegramBotProperties;

@Service
public class TelegramBotClient {

    private final TelegramBotProperties props;
    private final RestClient http;

    public TelegramBotClient(TelegramBotProperties props) {
        this.props = props;
        this.http = RestClient.create("https://api.telegram.org");
    }

    public String getBotUsername() { return props.getBotUsername(); }
    public boolean isPollingEnabled() { return props.isPollingEnabled(); }

    private String basePath() {
        String token = props.getBotToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN is not configured");
        }
        return "/bot" + token;
    }

    public void sendMessage(long chatId, String text) {
    http.get()
        .uri(uriBuilder -> uriBuilder
            .path(basePath() + "/sendMessage")
            .queryParam("chat_id", chatId)
            .queryParam("text", text)
            .build())
        .retrieve()
        .toBodilessEntity();
    }

    public void sendContactRequest(long chatId, String text) {
        String keyboardJson = """
            {
                "keyboard": [[{"text": "Share Phone Number", "request_contact": true}]],
                "one_time_keyboard": true,
                "resize_keyboard": true
            }
            """;
        http.post()
            .uri(basePath() + "/sendMessage")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(Map.of(
                "chat_id", chatId,
                "text", text,
                "reply_markup", keyboardJson
            ))
            .retrieve()
            .toBodilessEntity();
    }

    public Map<String, Object> getUpdates(Long offset) {
        String uri = basePath() + "/getUpdates?timeout=25";
        if (offset != null) {
            uri += "&offset=" + offset;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = http.get().uri(uri).retrieve().body(Map.class);
        return body;
    }
}
