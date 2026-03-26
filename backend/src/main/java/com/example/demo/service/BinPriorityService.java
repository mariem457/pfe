package com.example.demo.service;

import com.example.demo.dto.routing.RoutingBinDto;

import java.util.List;

public interface BinPriorityService {

    List<RoutingBinDto> getPriorityBinsForRouting();
}