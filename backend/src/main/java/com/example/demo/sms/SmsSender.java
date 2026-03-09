package com.example.demo.sms;

public interface SmsSender {
    void send(String toPhoneE164, String message);
}