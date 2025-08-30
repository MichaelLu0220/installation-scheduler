package com.example.installation.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DbOrderService {
    private final JdbcTemplate jdbc;
    
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
            o.setStrategy(rs.getString("strategy"));
            o.setStatus(rs.getString("status"));
            return o;
        }
    }
    
    public List<DbOrder> list() {
        // 獲取所有訂單
        List<DbOrder> orders = jdbc.query(
            "SELECT id, machine_name, due_date, eta_date, strategy, status FROM orders ORDER BY due_date, id",
            new OrderMapper()
        );
        
        // 獲取庫存資料
        Map<String, Integer> inventory = jdbc.query(
            "SELECT material, qty_on_hand FROM inventory",
            (rs) -> {
                Map<String, Integer> inv = new java.util.HashMap<>();
                while (rs.next()) {
                    inv.put(rs.getString("material"), rs.getInt("qty_on_hand"));
                }
                return inv;
            }
        );
        
        // 為每個訂單載入材料需求
        for (DbOrder order : orders) {
            List<DbOrder.MaterialRequirement> materials = jdbc.query(
                "SELECT material, qty_needed FROM order_materials WHERE order_id = ?",
                (rs, rowNum) -> {
                    String material = rs.getString("material");
                    int qtyNeeded = rs.getInt("qty_needed");
                    int qtyOnHand = inventory.getOrDefault(material, 0);
                    return new DbOrder.MaterialRequirement(material, qtyNeeded, qtyOnHand);
                },
                order.getId()
            );
            order.setMaterials(materials);
        }
        
        return orders;
    }
    
    // 獲取庫存總覽
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
                status.setMaterial(rs.getString("material"));
                status.setQtyOnHand(rs.getInt("qty_on_hand"));
                status.setTotalDemand(rs.getInt("total_demand"));
                status.setShortage(Math.max(0, status.getTotalDemand() - status.getQtyOnHand()));
                return status;
            }
        );
    }
    
    // 獲取到貨計劃
    public List<InboundPlan> getInboundPlans() {
        return jdbc.query(
            "SELECT material, arrival_date, qty FROM inbound_plans ORDER BY arrival_date, material",
            (rs, rowNum) -> {
                InboundPlan plan = new InboundPlan();
                plan.setMaterial(rs.getString("material"));
                plan.setArrivalDate(rs.getDate("arrival_date").toLocalDate());
                plan.setQty(rs.getInt("qty"));
                return plan;
            }
        );
    }
    
    // 獲取工人產能
    public List<WorkerCapacity> getWorkerCapacity() {
        return jdbc.query(
            "SELECT work_date, worker_count, hours_per_worker, units_per_hour, total_units_per_day " +
            "FROM worker_capacity ORDER BY work_date LIMIT 30",
            (rs, rowNum) -> {
                WorkerCapacity capacity = new WorkerCapacity();
                capacity.setWorkDate(rs.getDate("work_date").toLocalDate());
                capacity.setWorkerCount(rs.getInt("worker_count"));
                capacity.setHoursPerWorker(rs.getInt("hours_per_worker"));
                capacity.setUnitsPerHour(rs.getInt("units_per_hour"));
                capacity.setTotalUnitsPerDay(rs.getInt("total_units_per_day"));
                return capacity;
            }
        );
    }
}