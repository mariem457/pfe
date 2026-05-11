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

    @Value("${app.weather.no-sun-cloud-cover-threshold:85}")
    private int noSunCloudCoverThreshold;

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

    public boolean hasInsufficientSunlight(Double lat, Double lng) {
        if (!enabled || lat == null || lng == null) {
            return false;
        }

        try {
            JsonNode response = webClient.get()
                    .uri(
                            baseUrl + "?latitude={lat}&longitude={lng}&current=is_day,cloud_cover,weather_code&timezone=auto",
                            lat,
                            lng
                    )
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (response == null || response.get("current") == null) {
                return false;
            }

            JsonNode current = response.get("current");
            int isDay = current.path("is_day").asInt(1);
            int cloudCover = current.path("cloud_cover").asInt(0);
            int weatherCode = current.path("weather_code").asInt(0);

            return isDay == 0
                    || cloudCover >= noSunCloudCoverThreshold
                    || isLowSunWeatherCode(weatherCode);

        } catch (Exception e) {
            System.out.println("WEATHER SOLAR FALLBACK => " + e.getMessage());
            return false;
        }
    }

    private boolean isLowSunWeatherCode(int code) {
        return code == 45 || code == 48
                || (code >= 51 && code <= 67)
                || (code >= 71 && code <= 77)
                || (code >= 80 && code <= 82)
                || (code >= 85 && code <= 86)
                || (code >= 95 && code <= 99);
    }
}
