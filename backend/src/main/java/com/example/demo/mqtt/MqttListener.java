package com.example.demo.mqtt;

import com.example.demo.config.MqttConfig;
import com.example.demo.dto.BinSensorDataDto;
import com.example.demo.service.MqttSubscriberService;
import com.example.demo.service.TelemetryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MqttListener {

    private final ObjectMapper mapper;
    private final MqttSubscriberService mqttSubscriberService;
    private final TelemetryService telemetryService;

    public MqttListener(
            ObjectMapper mapper,
            MqttSubscriberService mqttSubscriberService,
            TelemetryService telemetryService
    ) {
        this.mapper = mapper;
        this.mqttSubscriberService = mqttSubscriberService;
        this.telemetryService = telemetryService;
    }

    @ServiceActivator(inputChannel = MqttConfig.MQTT_INPUT_CHANNEL)
    public void handle(Message<String> message) {
        try {
            String payload = message.getPayload();

            System.out.println("=== MQTT MESSAGE RECEIVED ===");
            System.out.println(payload);

            JsonNode node = mapper.readTree(payload);

            String binCode = node.path("binCode").asText(null);

            if (binCode != null && !binCode.isBlank()) {
                short fillLevel = (short) node.path("fillLevel").asInt(0);
                Short battery = node.has("batteryLevel")
                        ? (short) node.path("batteryLevel").asInt()
                        : null;

                String status = node.path("status").asText("OK");
                String source = node.path("source").asText("MQTT");

                BigDecimal weight = node.has("weightKg")
                        ? BigDecimal.valueOf(node.path("weightKg").asDouble())
                        : null;

                Short rssi = node.has("rssi")
                        ? (short) node.path("rssi").asInt()
                        : null;

                telemetryService.saveTelemetry(
                        binCode,
                        fillLevel,
                        battery,
                        weight,
                        status,
                        source,
                        rssi,
                        false
                );

                return;
            }

            BinSensorDataDto dto = mapper.readValue(payload, BinSensorDataDto.class);
            mqttSubscriberService.handleDto(dto);

        } catch (Exception e) {
            System.out.println("=== ERROR IN MQTT LISTENER ===");
            e.printStackTrace();
        }
    }
}