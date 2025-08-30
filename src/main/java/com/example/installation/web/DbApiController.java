package com.example.installation.web;

import com.example.installation.db.DbOrder;
import com.example.installation.db.DbOrderService;
import com.example.installation.db.InventoryStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/db")
public class DbApiController {
    private final DbOrderService svc;
    
    public DbApiController(DbOrderService svc) {
        this.svc = svc;
    }
    
    @GetMapping("/orders")
    public List<DbOrder> orders() {
        return svc.list();
    }
    
    @GetMapping("/inventory")
    public List<InventoryStatus> inventory() {
        return svc.getInventoryStatus();
    }
    
    @GetMapping("/inbound")
    public Object inboundPlans() {
        return svc.getInboundPlans();
    }
    
    @GetMapping("/capacity") 
    public Object workerCapacity() {
        return svc.getWorkerCapacity();
    }
    
    // 綜合儀表板數據
    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> result = new HashMap<>();
        
        List<DbOrder> orders = svc.list();
        List<InventoryStatus> inventory = svc.getInventoryStatus();
        
        // 統計數據
        result.put("totalOrders", orders.size());
        result.put("onTimeOrders", orders.stream().filter(o -> "ON_TIME".equals(o.getStatus())).count());
        result.put("lateOrders", orders.stream().filter(o -> "LATE".equals(o.getStatus())).count());
        result.put("materialsAtRisk", inventory.stream().filter(InventoryStatus::hasShortage).count());
        
        // 詳細資料
        result.put("orders", orders);
        result.put("inventory", inventory);
        result.put("inboundPlans", svc.getInboundPlans());
        result.put("workerCapacity", svc.getWorkerCapacity());
        
        return result;
    }
}