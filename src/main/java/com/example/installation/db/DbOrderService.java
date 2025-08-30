package com.example.installation.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class DbOrderService {
    private final JdbcTemplate jdbc;
    
    // ✅ 修正：資料庫實際使用 A/B/C，對應到中文顯示名稱
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
        System.out.println("🔧 DbOrderService 初始化完成");
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
            o.setStrategy("Partial");
            String status = rs.getString("status");
            o.setStatus(status != null ? status.trim() : "DRAFT");
            return o;
        }
    }
    
    public List<DbOrder> list() {
        try {
            System.out.println("📋 開始載入訂單列表...");
            
            List<DbOrder> orders = jdbc.query(
                "SELECT id, machine_name, due_date, eta_date, status FROM orders ORDER BY due_date, id",
                new OrderMapper()
            );
            
            System.out.println("✅ 成功載入 " + orders.size() + " 個訂單");
            
            // 載入庫存資料 (A/B/C 轉換為中文名稱)
            Map<String, Integer> inventory = getInventoryMap();
            System.out.println("📦 庫存資料: " + inventory);
            
            // 為每個訂單載入材料需求
            for (DbOrder order : orders) {
                try {
                    List<DbOrder.MaterialRequirement> materials = jdbc.query(
                        "SELECT material, qty_needed FROM order_materials WHERE order_id = ?",
                        (rs, rowNum) -> {
                            String code = rs.getString("material").trim(); // A, B, C
                            String chineseName = MATERIAL_CODE_TO_NAME.get(code); // 轉為中文
                            int qtyNeeded = rs.getInt("qty_needed");
                            int qtyOnHand = inventory.getOrDefault(code, 0); // 用代碼查庫存
                            
                            System.out.println("  📦 " + order.getMachineName() + " 需要 " + chineseName + "(" + code + ") " + qtyNeeded + " 單位，庫存 " + qtyOnHand);
                            
                            return new DbOrder.MaterialRequirement(chineseName, qtyNeeded, qtyOnHand);
                        },
                        order.getId()
                    );
                    order.setMaterials(materials);
                } catch (Exception e) {
                    System.err.println("⚠️ 載入訂單 " + order.getId() + " 的材料需求失敗: " + e.getMessage());
                    order.setMaterials(new ArrayList<>());
                }
            }
            
            return orders;
            
        } catch (Exception e) {
            System.err.println("❌ 載入訂單列表失敗: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<InventoryStatus> getInventoryStatus() {
        try {
            System.out.println("📦 開始載入庫存狀態...");
            
            List<InventoryStatus> result = jdbc.query(
                "SELECT i.material, i.qty_on_hand, " +
                "COALESCE(SUM(om.qty_needed), 0) as total_demand " +
                "FROM inventory i " +
                "LEFT JOIN order_materials om ON i.material = om.material " +
                "GROUP BY i.material, i.qty_on_hand " +
                "ORDER BY i.material",
                (rs, rowNum) -> {
                    InventoryStatus status = new InventoryStatus();
                    String code = rs.getString("material").trim(); // A, B, C
                    String chineseName = MATERIAL_CODE_TO_NAME.get(code); // 轉為中文顯示
                    
                    status.setMaterial(chineseName != null ? chineseName : code);
                    status.setQtyOnHand(rs.getInt("qty_on_hand"));
                    status.setTotalDemand(rs.getInt("total_demand"));
                    status.setShortage(Math.max(0, status.getTotalDemand() - status.getQtyOnHand()));
                    
                    System.out.println("  📦 " + status.getMaterial() + ": 庫存 " + status.getQtyOnHand() + ", 需求 " + status.getTotalDemand() + ", 缺口 " + status.getShortage());
                    
                    return status;
                }
            );
            
            System.out.println("✅ 成功載入 " + result.size() + " 個庫存項目");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 載入庫存狀態失敗: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<InboundPlan> getInboundPlans() {
        try {
            System.out.println("🚚 開始載入到貨計劃...");
            
            List<InboundPlan> result = jdbc.query(
                "SELECT material, arrival_date, qty FROM inbound_plans ORDER BY arrival_date, material",
                (rs, rowNum) -> {
                    InboundPlan plan = new InboundPlan();
                    String code = rs.getString("material").trim(); // A, B, C
                    String chineseName = MATERIAL_CODE_TO_NAME.get(code); // 轉為中文顯示
                    
                    plan.setMaterial(chineseName != null ? chineseName : code);
                    plan.setArrivalDate(rs.getDate("arrival_date").toLocalDate());
                    plan.setQty(rs.getInt("qty"));
                    return plan;
                }
            );
            
            System.out.println("✅ 成功載入 " + result.size() + " 個到貨計劃");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 載入到貨計劃失敗: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<InboundPlan> getUpcomingInboundPlans(int limit) {
        try {
            System.out.println("🚚 開始載入近期到貨計劃 (前" + limit + "個)...");
            
            List<InboundPlan> result = jdbc.query(
                "SELECT TOP (" + limit + ") material, arrival_date, qty FROM inbound_plans " +
                "WHERE arrival_date >= CAST(GETDATE() AS DATE) " +
                "ORDER BY arrival_date, material",
                (rs, rowNum) -> {
                    InboundPlan plan = new InboundPlan();
                    String code = rs.getString("material").trim(); // A, B, C
                    String chineseName = MATERIAL_CODE_TO_NAME.get(code); // 轉為中文顯示
                    
                    plan.setMaterial(chineseName != null ? chineseName : code);
                    plan.setArrivalDate(rs.getDate("arrival_date").toLocalDate());
                    plan.setQty(rs.getInt("qty"));
                    return plan;
                }
            );
            
            System.out.println("✅ 成功載入 " + result.size() + " 個近期到貨計劃");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 載入近期到貨計劃失敗: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public List<WorkerCapacity> getWorkerCapacity() {
        try {
            System.out.println("👷 開始載入工人產能...");
            
            List<WorkerCapacity> capacities = jdbc.query(
                "SELECT work_date, hours_total FROM worker_capacity ORDER BY work_date",
                (rs, rowNum) -> {
                    WorkerCapacity capacity = new WorkerCapacity();
                    capacity.setWorkDate(rs.getDate("work_date").toLocalDate());
                    capacity.setWorkerCount(3);
                    capacity.setHoursPerWorker(8);
                    capacity.setUnitsPerHour(1);
                    capacity.setTotalUnitsPerDay(rs.getInt("hours_total"));
                    return capacity;
                }
            );
            
            // 如果沒有資料，返回預設值
            if (capacities.isEmpty()) {
                System.out.println("⚠️ 沒有工人產能資料，使用預設值");
                WorkerCapacity defaultCapacity = new WorkerCapacity();
                defaultCapacity.setWorkDate(LocalDate.now());
                defaultCapacity.setWorkerCount(3);
                defaultCapacity.setHoursPerWorker(8);
                defaultCapacity.setUnitsPerHour(1);
                defaultCapacity.setTotalUnitsPerDay(24);
                capacities.add(defaultCapacity);
            }
            
            System.out.println("✅ 成功載入 " + capacities.size() + " 個工人產能記錄");
            return capacities;
            
        } catch (Exception e) {
            System.err.println("❌ 載入工人產能失敗: " + e.getMessage());
            // 返回預設值
            WorkerCapacity defaultCapacity = new WorkerCapacity();
            defaultCapacity.setWorkDate(LocalDate.now());
            defaultCapacity.setWorkerCount(3);
            defaultCapacity.setHoursPerWorker(8);
            defaultCapacity.setUnitsPerHour(1);
            defaultCapacity.setTotalUnitsPerDay(24);
            List<WorkerCapacity> result = new ArrayList<>();
            result.add(defaultCapacity);
            return result;
        }
    }
    
    public List<String> getAvailableMaterials() {
        try {
            List<String> result = jdbc.query(
                "SELECT material FROM inventory ORDER BY material",
                (rs, rowNum) -> {
                    String code = rs.getString("material").trim(); // A, B, C
                    String chineseName = MATERIAL_CODE_TO_NAME.get(code); // 轉為中文
                    return chineseName != null ? chineseName : code;
                }
            );
            
            System.out.println("✅ 可用材料: " + result);
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ 載入可用材料失敗: " + e.getMessage());
            // 返回預設材料
            List<String> materials = new ArrayList<>();
            materials.add("氮氣管");
            materials.add("水管");
            materials.add("真空管");
            return materials;
        }
    }
    
    public boolean isMachineNameExists(String machineName) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE machine_name = ?",
                Integer.class,
                machineName
            );
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("❌ 檢查機台名稱失敗: " + e.getMessage());
            return false;
        }
    }
    
    public DashboardStats getDashboardStats() {
        try {
            System.out.println("📊 開始計算統計資料...");
            
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
            
            System.out.println("📊 統計完成: 總訂單 " + stats.getTotalOrders() + 
                             ", 準時 " + stats.getOnTimeOrders() + 
                             ", 風險 " + stats.getAtRiskOrders());
            
            return stats;
            
        } catch (Exception e) {
            System.err.println("❌ 載入統計資料失敗: " + e.getMessage());
            e.printStackTrace();
            // 返回空統計
            return new DashboardStats();
        }
    }
    
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
                    String code = rs.getString("material").trim(); // A, B, C
                    String chineseName = MATERIAL_CODE_TO_NAME.get(code); // 轉為中文
                    int qtyNeeded = rs.getInt("qty_needed");
                    int qtyOnHand = inventory.getOrDefault(code, 0); // 用代碼查庫存
                    return new DbOrder.MaterialRequirement(
                        chineseName != null ? chineseName : code, 
                        qtyNeeded, 
                        qtyOnHand
                    );
                },
                order.getId()
            );
            order.setMaterials(materials);
            
            return order;
        } catch (Exception e) {
            System.err.println("❌ 根據ID載入訂單失敗: " + e.getMessage());
            return null;
        }
    }
    
    // ✅ 修正：返回 A/B/C 代碼對應的庫存，不做中文轉換
    private Map<String, Integer> getInventoryMap() {
        try {
            return jdbc.query(
                "SELECT material, qty_on_hand FROM inventory",
                (rs) -> {
                    Map<String, Integer> inv = new HashMap<>();
                    while (rs.next()) {
                        String code = rs.getString("material").trim(); // A, B, C
                        inv.put(code, rs.getInt("qty_on_hand")); // 保持原代碼
                    }
                    return inv;
                }
            );
        } catch (Exception e) {
            System.err.println("❌ 載入庫存對應表失敗: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    // 靜態內部類：統計數據
    public static class DashboardStats {
        private int totalOrders = 0;
        private int onTimeOrders = 0;
        private int lateOrders = 0;
        private int atRiskOrders = 0;
        private int lowStockMaterials = 0;
        private int onTimeRate = 0;
        
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