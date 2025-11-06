package com.delivery.deliveryapi.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TelegramUpdatePoller {
    private static final Logger log = LoggerFactory.getLogger(TelegramUpdatePoller.class);

    private final TelegramBotClient tg;
    private final OtpService otpService;

    private Long offset = null;

    public TelegramUpdatePoller(TelegramBotClient tg, OtpService otpService) {
        this.tg = tg;
        this.otpService = otpService;
    }

    // Poll every 3 seconds; in prod, use webhook instead
    @Scheduled(fixedDelay = 3000L, initialDelay = 3000L)
    public void poll() {
        try {
            if (!tg.isPollingEnabled()) return;

            Map<String, Object> resp = tg.getUpdates(offset);
            if (resp == null || !(Boolean.TRUE.equals(resp.get("ok")))) return;
            Object result = resp.get("result");
            if (!(result instanceof List<?> list) || list.isEmpty()) return;

            for (Object o : list) {
                if (o instanceof Map<?, ?> m) processUpdate(m);
            }
        } catch (Exception e) {
            log.warn("Telegram poll error: {}", e.toString());
        }
    }

    private void processUpdate(Map<?, ?> m) {
        Number updId = (Number) m.get("update_id");
        if (updId != null) offset = updId.longValue() + 1;

        Object msgObj = m.get("message");
        if (!(msgObj instanceof Map<?, ?> msg)) return;
        Number chatIdNum = (Number) ((Map<?, ?>) msg.get("chat")).get("id");
        if (chatIdNum == null) return;
        long chatId = chatIdNum.longValue();

        String text = (String) msg.get("text");
        if (text != null && text.startsWith("/start link_")) {
            log.info("Processing /start with text: {}", text);
            String linkCode = text.substring("/start link_".length()).trim();
            log.info("Parsed linkCode: {}", linkCode);
            @SuppressWarnings("unchecked")
            Map<String, Object> from = (Map<String, Object>) msg.get("from");
            otpService.linkAndSendOtp(linkCode, chatId, from);
            log.info("Called linkAndSendOtp for linkCode: {} chatId: {}", linkCode, chatId);
            return;
        }

        // Check for contact sharing
        Object contactObj = msg.get("contact");
        if (contactObj instanceof Map<?, ?> contact) {
            String phoneNumber = (String) contact.get("phone_number");
            log.info("Received contact message with phone: {} for chatId: {}", phoneNumber, chatId);
            if (phoneNumber != null) {
                // Normalize phone number
                String normalizedPhone = phoneNumber.replaceAll("\\s+", "").trim();
                if (!normalizedPhone.startsWith("+")) normalizedPhone = "+" + normalizedPhone;
                log.info("Normalized phone: {}", normalizedPhone);
                otpService.processContact(chatId, normalizedPhone);
            }
        }
    }
}
