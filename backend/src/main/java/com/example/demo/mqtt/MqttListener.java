package com.example.demo.mqtt;


import com.example.demo.config.MqttConfig;
import com.example.demo.dto.BinSensorDataDto;
import com.example.demo.service.MqttSubscriberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import com.example.demo.service.TelemetryService;
import org.springframework.stereotype.Component;

@Component
public class MqttListener {

    private final ObjectMapper mapper;
    private final MqttSubscriberService mqttSubscriberService;
    private final TelemetryService telemetryService;

    public MqttListener(ObjectMapper mapper, MqttSubscriberService mqttSubscriberService, TelemetryService telemetryService) {
        this.mapper = mapper;
        this.mqttSubscriberService = mqttSubscriberService;
        this.telemetryService = telemetryService;
    }

@ServiceActivator(inputChannel = MqttConfig.MQTT_INPUT_CHANNEL)
public void handle(Message<String> message) {
    System.out.println("=== MQTT MESSAGE RECEIVED ==="); // ← زيد هذا
    try {
        String payload = message.getPayload();
        System.out.println("=== RAW PAYLOAD ===");
        System.out.println(payload);

        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(payload);
        String binCode = node.path("binCode").asText(null);
        short fillLevel = (short) node.path("fillLevel").asInt(0);
        Short battery = node.has("batteryLevel") ? (short) node.path("batteryLevel").asInt() : null;
        String status = node.path("status").asText("OK");
        String source = node.path("source").asText("MQTT");

        java.math.BigDecimal weight = node.has("weightKg")
                ? java.math.BigDecimal.valueOf(node.path("weightKg").asDouble())
                : null;
        Short rssi = node.has("rssi") ? (short) node.path("rssi").asInt() : null;

        if (binCode == null || binCode.isBlank()) {
            System.out.println("=== NO binCode IN PAYLOAD, SKIPPING ===");
            return;
        }

        System.out.println("=== SAVING TELEMETRY FOR: " + binCode + " ==="); // ← زيد هذا
        telemetryService.saveTelemetry(binCode, fillLevel, battery, weight, status, source, rssi, false);

    } catch (Exception e) {
        System.out.println("=== ERROR IN MQTT LISTENER ===");
        e.printStackTrace();
    }
}
}