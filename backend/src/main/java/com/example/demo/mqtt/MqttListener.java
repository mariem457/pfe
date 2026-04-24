package com.example.demo.mqtt;

import com.example.demo.config.MqttConfig;
import com.example.demo.dto.BinSensorDataDto;
import com.example.demo.service.MqttSubscriberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttListener {

    private final ObjectMapper mapper;
    private final MqttSubscriberService mqttSubscriberService;

    public MqttListener(ObjectMapper mapper, MqttSubscriberService mqttSubscriberService) {
        this.mapper = mapper;
        this.mqttSubscriberService = mqttSubscriberService;
    }

    @ServiceActivator(inputChannel = MqttConfig.MQTT_INPUT_CHANNEL)
    public void handle(Message<String> message) {
        try {
            String payload = message.getPayload();
            System.out.println("=== RAW PAYLOAD ARRIVED ===");
            System.out.println(payload);

            BinSensorDataDto dto = mapper.readValue(payload, BinSensorDataDto.class);

            System.out.println("=== DTO PARSED ===");
            System.out.println("binId = " + dto.getBinId());

            mqttSubscriberService.handleDto(dto);

        } catch (Exception e) {
            System.out.println("=== ERROR IN MQTT LISTENER ===");
            e.printStackTrace();
        }
    }
}