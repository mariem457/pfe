package com.example.demo.dto;

import java.time.Instant;

public class TruckLocationRequest {
  public Long driverId;
  public Double lat;
  public Double lng;
  public Double speedKmh;
  public Double headingDeg;
  public Instant timestamp;
}
