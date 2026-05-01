package com.example.demo.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwilioSmsSender implements SmsSender {

    private final String fromPhone;

    public TwilioSmsSender(
            @Value("${twilio.sid}") String sid,
            @Value("${twilio.token}") String token,
            @Value("${twilio.phone}") String fromPhone
    ) {
        Twilio.init(sid, token);
        this.fromPhone = fromPhone;
    }

    @Override
    public void send(String toPhoneE164, String message) {
        Message.creator(
                new PhoneNumber(toPhoneE164),
                new PhoneNumber(fromPhone),
                message
        ).create();
    }
}