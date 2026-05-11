package com.example.demo.mqtt;

import com.example.demo.config.MqttConfig;
import com.example.demo.entity.BinSensorData;
import com.example.demo.repository.BinSensorDataRepository;
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
    private final TelemetryService telemetryService;
    private final BinSensorDataRepository binSensorDataRepository;

    public MqttListener(
            ObjectMapper mapper,
            TelemetryService telemetryService,
            BinSensorDataRepository binSensorDataRepository
    ) {
        this.mapper = mapper;
        this.telemetryService = telemetryService;
        this.binSensorDataRepository = binSensorDataRepository;
    }

    @ServiceActivator(inputChannel = MqttConfig.MQTT_INPUT_CHANNEL)
    public void handle(Message<String> message) {
        System.out.println("=== MQTT MESSAGE RECEIVED ===");

        try {
            String payload = message.getPayload();

            System.out.println("=== RAW PAYLOAD ===");
            System.out.println(payload);

            JsonNode node = mapper.readTree(payload);

            String binCode = node.has("binCode")
                    ? node.path("binCode").asText(null)
                    : node.path("binId").asText(null);

            if (binCode == null || binCode.isBlank()) {
                System.out.println("=== NO binCode/binId IN PAYLOAD, SKIPPING ===");
                return;
            }

            double fillLevelDouble = node.path("fillLevel").asDouble(0);
            short fillLevel = (short) Math.round(fillLevelDouble);

            Short battery = node.has("batteryLevel")
                    ? (short) node.path("batteryLevel").asInt()
                    : (node.has("battery") ? (short) node.path("battery").asInt() : null);

            int gasValue = node.path("gasValue").asInt(0);
            boolean fireDetected = node.path("fireDetected").asBoolean(false);

            String rawStatus = node.path("status").asText("OK");
            String status = normalizeStatus(rawStatus, fillLevel, fireDetected, gasValue);

            String source = node.path("source").asText("MQTT_SIM");

            Double weightKgRaw = node.has("weightKg")
                    ? node.path("weightKg").asDouble()
                    : null;

            BigDecimal weight = weightKgRaw != null
                    ? BigDecimal.valueOf(weightKgRaw)
                    : null;

            Short rssi = node.has("rssi")
                    ? (short) node.path("rssi").asInt()
                    : null;

            saveRawSensorData(
                    binCode,
                    fillLevelDouble,
                    gasValue,
                    fireDetected,
                    rawStatus,
                    weightKgRaw
            );

            System.out.println("=== SAVING TELEMETRY ===");
            System.out.println("binCode = " + binCode);
            System.out.println("fillLevel = " + fillLevel);
            System.out.println("battery = " + battery);
            System.out.println("gasValue = " + gasValue);
            System.out.println("fireDetected = " + fireDetected);
            System.out.println("rawStatus = " + rawStatus);
            System.out.println("normalizedStatus = " + status);
            System.out.println("source = " + source);
            System.out.println("weightKg = " + weightKgRaw);
            System.out.println("rssi = " + rssi);

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

            System.out.println("=== TELEMETRY SAVED SUCCESSFULLY ===");

        } catch (Exception e) {
            System.out.println("=== ERROR IN MQTT LISTENER ===");
            e.printStackTrace();
        }
    }

    private void saveRawSensorData(
            String binCode,
            double fillLevel,
            int gasValue,
            boolean fireDetected,
            String rawStatus,
            Double weightKg
    ) {
        try {
            BinSensorData data = new BinSensorData();
            data.setBinId(binCode);
            data.setFillLevel(fillLevel);
            data.setGasValue(gasValue);
            data.setFireDetected(fireDetected);
            data.setStatus(rawStatus);
            data.setWeightKg(weightKg);

            binSensorDataRepository.save(data);

            System.out.println("=== RAW SENSOR DATA SAVED ===");
        } catch (Exception e) {
            System.out.println("=== ERROR SAVING RAW SENSOR DATA ===");
            e.printStackTrace();
        }
    }

    private String normalizeStatus(
            String status,
            short fillLevel,
            boolean fireDetected,
            int gasValue
    ) {
        if (fireDetected) {
            return "ERROR";
        }

        if (gasValue >= 600) {
            return "ERROR";
        }

        if (status == null || status.isBlank()) {
            return "OK";
        }

        if ("FIRE_DETECTED".equalsIgnoreCase(status)
                || "GAS_DANGER".equalsIgnoreCase(status)
                || "ULTRASONIC_ERROR".equalsIgnoreCase(status)
                || "ERROR".equalsIgnoreCase(status)) {
            return "ERROR";
        }

        if ("BIN_FULL".equalsIgnoreCase(status)
                || "FULL".equalsIgnoreCase(status)
                || "OVERFLOW".equalsIgnoreCase(status)
                || fillLevel >= 95) {
            return "OVERFLOW";
        }

        return "OK";
    }
}