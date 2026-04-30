package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;

public interface CollectionScheduleService {

    boolean isCollectionAllowed(RoutingBinDto bin);

    String explainCollectionWindow(RoutingBinDto bin);
}