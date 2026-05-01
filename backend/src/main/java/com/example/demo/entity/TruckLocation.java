package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "truck_locations")
public class TruckLocation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "driver_id", nullable = false)
  private Driver driver;

  @Column(nullable = false)
  private Instant timestamp;

  @Column(nullable = false)
  private Double lat;

  @Column(nullable = false)
  private Double lng;

  @Column(name="speed_kmh", precision = 6, scale = 2)
  private BigDecimal speedKmh;

@Column(name="heading_deg", precision = 6, scale = 2)
private BigDecimal headingDeg;

  // getters/setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public Driver getDriver() { return driver; }
  public void setDriver(Driver driver) { this.driver = driver; }

  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

  public Double getLat() { return lat; }
  public void setLat(Double lat) { this.lat = lat; }

  public Double getLng() { return lng; }
  public void setLng(Double lng) { this.lng = lng; }
public BigDecimal getSpeedKmh() {
	return speedKmh;
}
public void setSpeedKmh(BigDecimal speedKmh) {
	this.speedKmh = speedKmh;
}
public BigDecimal getHeadingDeg() {
	return headingDeg;
}
public void setHeadingDeg(BigDecimal headingDeg) {
	this.headingDeg = headingDeg;
}

  
}