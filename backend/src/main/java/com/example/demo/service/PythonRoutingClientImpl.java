package com.example.demo.service;

import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PythonRoutingClientImpl implements PythonRoutingClient {

    private final WebClient webClient;

    public PythonRoutingClientImpl(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("http://127.0.0.1:8000")
                .build();
    }

    @Override
    public RoutingResponseDto optimizeRoutes(RoutingRequestDto request) {
        return webClient.post()
                .uri("/optimize")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RoutingResponseDto.class)
                .block();
    }
}