package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class WeatherService {

    private final WebClient webClient;

    @Value("${app.weather.enabled:true}")
    private boolean enabled;

    @Value("${app.weather.base-url:https://api.open-meteo.com/v1/forecast}")
    private String baseUrl;

    @Value("${app.weather.default-temperature-celsius:28.0}")
    private double defaultTemperatureCelsius;

    @Value("${app.weather.high-temperature-threshold-celsius:30.0}")
    private double highTemperatureThresholdCelsius;

    public WeatherService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public double getCurrentTemperatureCelsius(Double lat, Double lng) {
        if (!enabled || lat == null || lng == null) {
            return defaultTemperatureCelsius;
        }

        try {
            JsonNode response = webClient.get()
                    .uri(
                            baseUrl + "?latitude={lat}&longitude={lng}&current=temperature_2m&timezone=auto",
                            lat,
                            lng
                    )
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (response == null || response.get("current") == null) {
                return defaultTemperatureCelsius;
            }

            JsonNode tempNode = response.get("current").get("temperature_2m");
            if (tempNode == null || !tempNode.isNumber()) {
                return defaultTemperatureCelsius;
            }

            return tempNode.asDouble();

        } catch (Exception e) {
            System.out.println("WEATHER API FALLBACK => " + e.getMessage());
            return defaultTemperatureCelsius;
        }
    }

    public boolean isHighTemperature(Double lat, Double lng) {
        return getCurrentTemperatureCelsius(lat, lng) >= highTemperatureThresholdCelsius;
    }
}