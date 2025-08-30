package com.example.installation.db;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class DbOrder {
    private Long id;
    private String machineName;
    private LocalDate dueDate;
    private LocalDate etaDate;
    private String strategy;
    private String status;
    private List<MaterialRequirement> materials = new ArrayList<>();
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public LocalDate getEtaDate() { return etaDate; }
    public void setEtaDate(LocalDate etaDate) { this.etaDate = etaDate; }
    
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<MaterialRequirement> getMaterials() { return materials; }
    public void setMaterials(List<MaterialRequirement> materials) { this.materials = materials; }
    
    // 內部類：材料需求
    public static class MaterialRequirement {
        private String material;
        private int qtyNeeded;
        private int qtyOnHand;
        private int shortage;
        
        public MaterialRequirement() {}
        
        public MaterialRequirement(String material, int qtyNeeded, int qtyOnHand) {
            this.material = material;
            this.qtyNeeded = qtyNeeded;
            this.qtyOnHand = qtyOnHand;
            this.shortage = Math.max(0, qtyNeeded - qtyOnHand);
        }
        
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        
        public int getQtyNeeded() { return qtyNeeded; }
        public void setQtyNeeded(int qtyNeeded) { this.qtyNeeded = qtyNeeded; }
        
        public int getQtyOnHand() { return qtyOnHand; }
        public void setQtyOnHand(int qtyOnHand) { this.qtyOnHand = qtyOnHand; }
        
        public int getShortage() { return shortage; }
        public void setShortage(int shortage) { this.shortage = shortage; }
        
        public boolean isShortage() { return shortage > 0; }
    }
}