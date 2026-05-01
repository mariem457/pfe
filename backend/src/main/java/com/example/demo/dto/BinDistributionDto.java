package com.example.demo.dto;

public class BinDistributionDto {

    private long emptyBins;
    private long partialBins;
    private long fullBins;
    private long totalBins;

    public BinDistributionDto() {
    }

    public BinDistributionDto(long emptyBins, long partialBins, long fullBins, long totalBins) {
        this.emptyBins = emptyBins;
        this.partialBins = partialBins;
        this.fullBins = fullBins;
        this.totalBins = totalBins;
    }

    public long getEmptyBins() {
        return emptyBins;
    }

    public void setEmptyBins(long emptyBins) {
        this.emptyBins = emptyBins;
    }

    public long getPartialBins() {
        return partialBins;
    }

    public void setPartialBins(long partialBins) {
        this.partialBins = partialBins;
    }

    public long getFullBins() {
        return fullBins;
    }

    public void setFullBins(long fullBins) {
        this.fullBins = fullBins;
    }

    public long getTotalBins() {
        return totalBins;
    }

    public void setTotalBins(long totalBins) {
        this.totalBins = totalBins;
    }
}