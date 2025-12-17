package com.stellarix.hse.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FcmSender {

    public void send(String deviceToken, String title, String body) {
        Message message = Message.builder()
                .setToken(deviceToken)
                .putData("eventType", "message_read")
                .putData("messageId", "903248")
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Message sent successfully: " + response);
        } catch (Exception e) {
            System.out.println("Message failed to send");
            e.printStackTrace();
        }
    }
}