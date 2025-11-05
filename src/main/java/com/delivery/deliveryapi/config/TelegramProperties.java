package com.delivery.deliveryapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String botToken;

    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }
}
