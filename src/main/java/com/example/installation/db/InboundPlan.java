package com.example.installation.db;

import java.time.LocalDate;

public class InboundPlan {
    private String material;
    private LocalDate arrivalDate;
    private int qty;
    
    // 建構子
    public InboundPlan() {}
    
    public InboundPlan(String material, LocalDate arrivalDate, int qty) {
        this.material = material;
        this.arrivalDate = arrivalDate;
        this.qty = qty;
    }
    
    // Getters and Setters
    public String getMaterial() {
        return material;
    }
    
    public void setMaterial(String material) {
        this.material = material;
    }
    
    public LocalDate getArrivalDate() {
        return arrivalDate;
    }
    
    public void setArrivalDate(LocalDate arrivalDate) {
        this.arrivalDate = arrivalDate;
    }
    
    public int getQty() {
        return qty;
    }
    
    public void setQty(int qty) {
        this.qty = qty;
    }
}