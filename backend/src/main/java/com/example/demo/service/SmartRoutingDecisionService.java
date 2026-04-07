package com.example.demo.service;

import com.example.demo.entity.Truck;
import java.util.List;

public interface SmartRoutingDecisionService {

    RoutingDecision makeDecision(List<Truck> trucks);
}