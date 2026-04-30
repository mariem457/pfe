package com.example.demo.dto.routing;

public class RoutingStopDto {

    private String stopType = "BIN_PICKUP";
    private Long binId;
    private Long disposalSiteId;
    private Integer orderIndex;

    public RoutingStopDto() {
    }

    public String getStopType() {
        return stopType;
    }

    public void setStopType(String stopType) {
        this.stopType = stopType;
    }

    public Long getBinId() {
        return binId;
    }

    public void setBinId(Long binId) {
        this.binId = binId;
    }

    public Long getDisposalSiteId() {
        return disposalSiteId;
    }

    public void setDisposalSiteId(Long disposalSiteId) {
        this.disposalSiteId = disposalSiteId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}