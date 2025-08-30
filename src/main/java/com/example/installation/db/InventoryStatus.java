package com.example.installation.db;

import java.time.LocalDate;

// 庫存狀態類別 (新增)
public class InventoryStatus {
    private String material;
    private int qtyOnHand;
    private int totalDemand;
    private int shortage;
    
    // Getters and Setters
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    
    public int getQtyOnHand() { return qtyOnHand; }
    public void setQtyOnHand(int qtyOnHand) { this.qtyOnHand = qtyOnHand; }
    
    public int getTotalDemand() { return totalDemand; }
    public void setTotalDemand(int totalDemand) { this.totalDemand = totalDemand; }
    
    public int getShortage() { return shortage; }
    public void setShortage(int shortage) { this.shortage = shortage; }
    
    public boolean hasShortage() { return shortage > 0; }
}
