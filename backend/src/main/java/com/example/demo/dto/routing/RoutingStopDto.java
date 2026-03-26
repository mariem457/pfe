package com.example.demo.dto.routing;

public class RoutingStopDto {

    private Long binId;
    private Integer orderIndex;

    public RoutingStopDto() {
    }

    public Long getBinId() {
        return binId;
    }

    public void setBinId(Long binId) {
        this.binId = binId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}