package com.mybill.MyBill_Backend.service.notification.channel;

public interface NotificationChannelProvider {
    String getChannelName(); // e.g. WHATSAPP, SMS

    /**
     * Sends a message to a recipient.
     * @param recipient The target recipient (for example phone number)
     * @param subject Optional subject for channels that support one
     * @param body The content body of the message
     * @return Provider response string (JSON or transaction ID)
     * @throws Exception if sending fails
     */
    String sendNotification(String recipient, String subject, String body) throws Exception;
}
