package com.example.demo.mqtt;

import com.example.demo.config.MqttConfig;
import com.example.demo.dto.BinTelemetryDTO;
import com.example.demo.service.BinService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttListener {

    private final ObjectMapper mapper;
    private final BinService binService;

    public MqttListener(BinService binService, ObjectMapper mapper) {
        this.binService = binService;
        this.mapper = mapper;
    }

    @ServiceActivator(inputChannel = MqttConfig.MQTT_INPUT_CHANNEL)
    public void handle(Message<String> message) {
        try {
            String payload = message.getPayload();
            System.out.println("📦 RAW PAYLOAD: " + payload);

            BinTelemetryDTO dto = mapper.readValue(payload, BinTelemetryDTO.class);
            System.out.println("✅ DTO parsed successfully");

            binService.processTelemetry(dto);
            System.out.println("💾 Telemetry sent to service");

        } catch (Exception e) {
            System.out.println("❌ ERROR processing MQTT message");
            e.printStackTrace();
        }
    }
}