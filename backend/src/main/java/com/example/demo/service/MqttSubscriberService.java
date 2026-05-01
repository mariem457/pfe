package com.example.demo.service;

import com.example.demo.dto.BinSensorDataDto;
import com.example.demo.entity.BinSensorData;
import com.example.demo.repository.BinSensorDataRepository;
import org.springframework.stereotype.Service;

@Service
public class MqttSubscriberService {

    private final BinSensorDataRepository repository;

    public MqttSubscriberService(BinSensorDataRepository repository) {
        this.repository = repository;
    }

    public void handleDto(BinSensorDataDto dto) {
        try {
            System.out.println(">>> handleDto CALLED");
            System.out.println("binId = " + dto.getBinId());
            System.out.println("fillLevel = " + dto.getFillLevel());
            System.out.println("gasValue = " + dto.getGasValue());
            System.out.println("fireDetected = " + dto.isFireDetected());
            System.out.println("status = " + dto.getStatus());

            BinSensorData entity = new BinSensorData();
            entity.setBinId(dto.getBinId());
            entity.setFillLevel(dto.getFillLevel());
            entity.setGasValue(dto.getGasValue());
            entity.setFireDetected(dto.isFireDetected());
            entity.setStatus(dto.getStatus());

            BinSensorData saved = repository.save(entity);
            repository.flush();

            System.out.println(">>> SAVED ID = " + saved.getId());

        } catch (Exception e) {
            System.out.println(">>> ERROR IN handleDto");
            e.printStackTrace();
        }
    }
}