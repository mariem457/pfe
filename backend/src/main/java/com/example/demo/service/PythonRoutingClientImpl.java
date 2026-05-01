package com.example.demo.service;

import com.example.demo.dto.routing.RoutingRequestDto;
import com.example.demo.dto.routing.RoutingResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PythonRoutingClientImpl implements PythonRoutingClient {

    private final WebClient webClient;

    public PythonRoutingClientImpl(WebClient webClient) {
        this.webClient = webClient;
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