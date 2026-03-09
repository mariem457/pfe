package com.example.demo.dto;

import java.time.Instant;

public class TruckLocationResponse {
  public Long driverId;
  public Double lat;
  public Double lng;
  public Double speedKmh;
  public Double headingDeg;
  public Instant timestamp;

  public static TruckLocationResponse of(Long driverId, Double lat, Double lng,
                                         Double speedKmh, Double headingDeg, Instant timestamp) {
    TruckLocationResponse r = new TruckLocationResponse();
    r.driverId = driverId;
    r.lat = lat;
    r.lng = lng;
    r.speedKmh = speedKmh;
    r.headingDeg = headingDeg;
    r.timestamp = timestamp;
    return r;
  }
}