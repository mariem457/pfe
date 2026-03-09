package com.example.demo.controller;

import com.example.demo.dto.TruckLocationRequest;
import com.example.demo.service.TruckLocationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class TruckWsController {

    private final TruckLocationService truckLocationService;

    public TruckWsController(TruckLocationService truckLocationService) {
        this.truckLocationService = truckLocationService;
    }

    @MessageMapping("/trucks/location")
    public void receiveLocation(TruckLocationRequest in) {
        truckLocationService.save(in);
    }
}