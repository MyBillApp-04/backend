package com.mybill.MyBill_Backend.service.notification.channel;

public interface NotificationChannelProvider {
    String getChannelName(); // e.g. WHATSAPP, EMAIL, SMS

    /**
     * Sends a message to a recipient.
     * @param recipient The target recipient (e.g., phone number or email)
     * @param subject Optional subject (for email)
     * @param body The content body of the message
     * @return Provider response string (JSON or transaction ID)
     * @throws Exception if sending fails
     */
    String sendNotification(String recipient, String subject, String body) throws Exception;
}
