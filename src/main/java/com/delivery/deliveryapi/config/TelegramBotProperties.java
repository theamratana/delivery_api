package com.delivery.deliveryapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public class TelegramBotProperties extends TelegramProperties {
    // Optional extra props
    private String botUsername;
    private boolean pollingEnabled = true; // enable simple polling in dev by default

    public String getBotUsername() { return botUsername; }
    public void setBotUsername(String botUsername) { this.botUsername = botUsername; }

    public boolean isPollingEnabled() { return pollingEnabled; }
    public void setPollingEnabled(boolean pollingEnabled) { this.pollingEnabled = pollingEnabled; }
}
