package com.example.installation.web;

import com.example.installation.db.DbOrderService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/orders")
public class OrderController {
    private final DbOrderService dbOrderService;
    private final JdbcTemplate jdbc;
    
    public OrderController(DbOrderService dbOrderService, JdbcTemplate jdbc) {
        this.dbOrderService = dbOrderService;
        this.jdbc = jdbc;
    }
    
    // 顯示新增訂單頁面
    @GetMapping("/new")
    public String newOrderForm(Model model) {
        // 可以加入當前庫存資訊給前端參考
        model.addAttribute("inventoryStatus", dbOrderService.getInventoryStatus());
        return "order-input";
    }
    
    // 處理訂單提交
    @PostMapping("/create")
    public String createOrder(@RequestParam Map<String, String> params, 
                            RedirectAttributes redirectAttributes) {
        try {
            String machineName = params.get("machineName");
            LocalDate dueDate = LocalDate.parse(params.get("dueDate"));
            
            // 插入訂單主記錄
            String insertOrderSql = "INSERT INTO orders (machine_name, due_date, strategy) VALUES (?, ?, 'Partial')";
            jdbc.update(insertOrderSql, machineName, dueDate);
            
            // 獲取剛插入的訂單ID
            Long orderId = jdbc.queryForObject(
                "SELECT id FROM orders WHERE machine_name = ? AND due_date = ? ORDER BY id DESC LIMIT 1", 
                Long.class, machineName, dueDate);
            
            // 插入材料需求
            insertMaterialIfNotZero(orderId, "氮氣管", params.get("nitrogenPipe"));
            insertMaterialIfNotZero(orderId, "水管", params.get("waterPipe"));
            insertMaterialIfNotZero(orderId, "真空管", params.get("vacuumPipe"));
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("訂單 %s 已成功建立！系統正在重新計算排程...", machineName));
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "訂單建立失敗：" + e.getMessage());
        }
        
        return "redirect:/db/orders";
    }
    
    private void insertMaterialIfNotZero(Long orderId, String material, String qtyStr) {
        if (qtyStr != null && !qtyStr.trim().isEmpty()) {
            try {
                int qty = Integer.parseInt(qtyStr.trim());
                if (qty > 0) {
                    jdbc.update("INSERT INTO order_materials (order_id, material, qty_needed) VALUES (?, ?, ?)",
                               orderId, material, qty);
                }
            } catch (NumberFormatException e) {
                // 忽略無效數字
            }
        }
    }
    
    // API: 預覽訂單影響 (可選實現)
    @PostMapping("/preview")
    @ResponseBody
    public Map<String, Object> previewOrder(@RequestBody Map<String, Object> orderData) {
        // 這裡可以實現簡單的排程預覽邏輯
        // 目前先返回模擬數據
        return Map.of(
            "materialAnalysis", "材料需求分析完成",
            "scheduleImpact", "可能影響現有訂單完成時間",
            "recommendation", "建議調整截止日期以避免延遲風險"
        );
    }
}