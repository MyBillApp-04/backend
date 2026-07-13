package com.mybill.MyBill_Backend.service.notification.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class WhatsAppChannelProvider implements NotificationChannelProvider {

    @Override
    public String getChannelName() {
        return "WHATSAPP";
    }

    @Override
    public String sendNotification(String recipient, String subject, String body) throws Exception {
        log.info("Sending WhatsApp notification recipientPresent={} subjectPresent={} bodyLength={}",
                recipient != null && !recipient.isBlank(),
                subject != null && !subject.isBlank(),
                body != null ? body.length() : 0);
        
        // Simulating WhatsApp API Gateway call (e.g. Meta Cloud API, Gupshup, or Twilio)
        // Here we just return a simulated JSON response with a unique message reference
        String messageId = UUID.randomUUID().toString();
        return String.format("{\"status\":\"success\",\"message_id\":\"%s\",\"provider\":\"mock-whatsapp-gateway\"}", messageId);
    }
}
