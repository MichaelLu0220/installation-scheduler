package com.example.installation.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class DbOrderService {
    private final JdbcTemplate jdbc;
    
    // 材料代碼對應表：A=氮氣管, B=水管, C=真空管
    private static final Map<String, String> MATERIAL_CODE_TO_NAME = Map.of(
        "A", "氮氣管",
        "B", "水管", 
        "C", "真空管"
    );
    
    private static final Map<String, String> MATERIAL_NAME_TO_CODE = Map.of(
        "氮氣管", "A",
        "水管", "B", 
        "真空管", "C"
    );
    
    public DbOrderService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    
    private static class OrderMapper implements RowMapper<DbOrder> {
        @Override
        public DbOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
            DbOrder o = new DbOrder();
            o.setId(rs.getLong("id"));
            o.setMachineName(rs.getString("machine_name"));
            o.setDueDate(rs.getDate("due_date").toLocalDate());
            Date eta = rs.getDate("eta_date");
            o.setEtaDate(eta == null ? null : eta.toLocalDate());
            o.setStrategy("Partial"); // 固定為Partial
            String status = rs.getString("status");
            o.setStatus(status != null ? status.trim() : "DRAFT");
            return o;
        }
    }
    
    public List<DbOrder> list() {
        List<DbOrder> orders = jdbc.query(
            "SELECT id, machine_name, due_date, eta_date, status FROM orders ORDER BY due_date, id",
            new OrderMapper()
        );
        
        // 獲取庫存資料 (A/B/C轉換為中文顯示)
        Map<String, Integer> inventory = jdbc.query(
            "SELECT material, qty_on_hand FROM inventory",
            (rs) -> {
                Map<String, Integer> inv = new HashMap<>();
                while (rs.next()) {
                    String code = rs.getString("material").trim();
                    String chineseName = MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
                    inv.put(chineseName, rs.getInt("qty_on_hand"));
                }
                return inv;
            }
        );
        
        // 為每個訂單載入材料需求
        for (DbOrder order : orders) {
            List<DbOrder.MaterialRequirement> materials = jdbc.query(
                "SELECT material, qty_needed FROM order_materials WHERE order_id = ?",
                (rs, rowNum) -> {
                    String code = rs.getString("material").trim();
                    String chineseName = MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
                    int qtyNeeded = rs.getInt("qty_needed");
                    int qtyOnHand = inventory.getOrDefault(chineseName, 0);
                    return new DbOrder.MaterialRequirement(chineseName, qtyNeeded, qtyOnHand);
                },
                order.getId()
            );
            order.setMaterials(materials);
        }
        
        return orders;
    }
    
    // 獲取庫存總覽 - A/B/C轉中文顯示
    public List<InventoryStatus> getInventoryStatus() {
        return jdbc.query(
            "SELECT i.material, i.qty_on_hand, " +
            "COALESCE(SUM(om.qty_needed), 0) as total_demand " +
            "FROM inventory i " +
            "LEFT JOIN order_materials om ON i.material = om.material " +
            "GROUP BY i.material, i.qty_on_hand " +
            "ORDER BY i.material",
            (rs, rowNum) -> {
                InventoryStatus status = new InventoryStatus();
                String code = rs.getString("material").trim();
                String chineseName = MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
                
                status.setMaterial(chineseName);
                status.setQtyOnHand(rs.getInt("qty_on_hand"));
                status.setTotalDemand(rs.getInt("total_demand"));
                status.setShortage(Math.max(0, status.getTotalDemand() - status.getQtyOnHand()));
                return status;
            }
        );
    }
    
    // 獲取到貨計劃 - A/B/C轉中文顯示
    public List<InboundPlan> getInboundPlans() {
        return jdbc.query(
            "SELECT material, arrival_date, qty FROM inbound_plans ORDER BY arrival_date, material",
            (rs, rowNum) -> {
                InboundPlan plan = new InboundPlan();
                String code = rs.getString("material").trim();
                String chineseName = MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
                plan.setMaterial(chineseName);
                plan.setArrivalDate(rs.getDate("arrival_date").toLocalDate());
                plan.setQty(rs.getInt("qty"));
                return plan;
            }
        );
    }
    
    // 獲取近期到貨計劃
    public List<InboundPlan> getUpcomingInboundPlans(int limit) {
        return jdbc.query(
            "SELECT TOP " + limit + " material, arrival_date, qty FROM inbound_plans " +
            "WHERE arrival_date >= CAST(GETDATE() AS DATE) " +
            "ORDER BY arrival_date, material",
            (rs, rowNum) -> {
                InboundPlan plan = new InboundPlan();
                String code = rs.getString("material").trim();
                String chineseName = MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
                plan.setMaterial(chineseName);
                plan.setArrivalDate(rs.getDate("arrival_date").toLocalDate());
                plan.setQty(rs.getInt("qty"));
                return plan;
            }
        );
    }
    
    public List<WorkerCapacity> getWorkerCapacity() {
        return jdbc.query(
            "SELECT work_date, hours_total FROM worker_capacity ORDER BY work_date",
            (rs, rowNum) -> {
                WorkerCapacity capacity = new WorkerCapacity();
                capacity.setWorkDate(rs.getDate("work_date").toLocalDate());
                capacity.setWorkerCount(3); // 假設3人
                capacity.setHoursPerWorker(8); // 每人8小時
                capacity.setUnitsPerHour(1); // 每小時1單位
                capacity.setTotalUnitsPerDay(rs.getInt("hours_total"));
                return capacity;
            }
        );
    }
    
    // 獲取可用材料清單 (返回中文名稱)
    public List<String> getAvailableMaterials() {
        return jdbc.query(
            "SELECT material FROM inventory ORDER BY material",
            (rs, rowNum) -> {
                String code = rs.getString("material").trim();
                return MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
            }
        );
    }
    
    // 檢查機台名稱是否已存在
    public boolean isMachineNameExists(String machineName) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE machine_name = ?",
            Integer.class,
            machineName
        );
        return count != null && count > 0;
    }
    
    // 獲取儀表板統計數據
    public DashboardStats getDashboardStats() {
        List<DbOrder> orders = list();
        List<InventoryStatus> inventory = getInventoryStatus();
        
        DashboardStats stats = new DashboardStats();
        stats.setTotalOrders(orders.size());
        stats.setOnTimeOrders((int) orders.stream()
            .filter(o -> "ON_TIME".equals(o.getStatus()))
            .count());
        stats.setLateOrders((int) orders.stream()
            .filter(o -> "LATE".equals(o.getStatus()))
            .count());
        stats.setAtRiskOrders((int) orders.stream()
            .filter(order -> order.getMaterials().stream()
                .anyMatch(m -> m.getShortage() > 0))
            .count());
        stats.setLowStockMaterials((int) inventory.stream()
            .filter(InventoryStatus::hasShortage)
            .count());
        
        if (stats.getTotalOrders() > 0) {
            stats.setOnTimeRate((stats.getOnTimeOrders() * 100) / stats.getTotalOrders());
        } else {
            stats.setOnTimeRate(0);
        }
        
        return stats;
    }
    
    // 根據ID查找訂單
    public DbOrder findById(Long id) {
        try {
            List<DbOrder> orders = jdbc.query(
                "SELECT id, machine_name, due_date, eta_date, status FROM orders WHERE id = ?",
                new OrderMapper(),
                id
            );
            
            if (orders.isEmpty()) {
                return null;
            }
            
            DbOrder order = orders.get(0);
            
            // 載入材料需求
            Map<String, Integer> inventory = getInventoryMap();
            List<DbOrder.MaterialRequirement> materials = jdbc.query(
                "SELECT material, qty_needed FROM order_materials WHERE order_id = ?",
                (rs, rowNum) -> {
                    String code = rs.getString("material").trim();
                    String chineseName = MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
                    int qtyNeeded = rs.getInt("qty_needed");
                    int qtyOnHand = inventory.getOrDefault(chineseName, 0);
                    return new DbOrder.MaterialRequirement(chineseName, qtyNeeded, qtyOnHand);
                },
                order.getId()
            );
            order.setMaterials(materials);
            
            return order;
        } catch (Exception e) {
            return null;
        }
    }
    
    // 輔助方法：取得庫存對應表 (轉換為中文名稱)
    private Map<String, Integer> getInventoryMap() {
        return jdbc.query(
            "SELECT material, qty_on_hand FROM inventory",
            (rs) -> {
                Map<String, Integer> inv = new HashMap<>();
                while (rs.next()) {
                    String code = rs.getString("material").trim();
                    String chineseName = MATERIAL_CODE_TO_NAME.getOrDefault(code, code);
                    inv.put(chineseName, rs.getInt("qty_on_hand"));
                }
                return inv;
            }
        );
    }
    
    // 靜態內部類：統計數據
    public static class DashboardStats {
        private int totalOrders;
        private int onTimeOrders;
        private int lateOrders;
        private int atRiskOrders;
        private int lowStockMaterials;
        private int onTimeRate;
        
        // Getters and Setters
        public int getTotalOrders() { return totalOrders; }
        public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
        
        public int getOnTimeOrders() { return onTimeOrders; }
        public void setOnTimeOrders(int onTimeOrders) { this.onTimeOrders = onTimeOrders; }
        
        public int getLateOrders() { return lateOrders; }
        public void setLateOrders(int lateOrders) { this.lateOrders = lateOrders; }
        
        public int getAtRiskOrders() { return atRiskOrders; }
        public void setAtRiskOrders(int atRiskOrders) { this.atRiskOrders = atRiskOrders; }
        
        public int getLowStockMaterials() { return lowStockMaterials; }
        public void setLowStockMaterials(int lowStockMaterials) { this.lowStockMaterials = lowStockMaterials; }
        
        public int getOnTimeRate() { return onTimeRate; }
        public void setOnTimeRate(int onTimeRate) { this.onTimeRate = onTimeRate; }
    }
}