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
    
    // 材料名稱轉換對應表
    private static final Map<String, String> MATERIAL_NAME_TO_CODE = Map.of(
        "nitrogenPipe", "A",  // 氮氣管 -> A
        "waterPipe", "B",     // 水管 -> B
        "vacuumPipe", "C"     // 真空管 -> C
    );
    
    public OrderController(DbOrderService dbOrderService, JdbcTemplate jdbc) {
        this.dbOrderService = dbOrderService;
        this.jdbc = jdbc;
    }
    
    @GetMapping("/new")
    public String newOrderForm(Model model) {
        model.addAttribute("inventoryStatus", dbOrderService.getInventoryStatus());
        model.addAttribute("availableMaterials", dbOrderService.getAvailableMaterials());
        return "order-input";
    }
    
    @PostMapping("/create")
    public String createOrder(@RequestParam Map<String, String> params, 
                            RedirectAttributes redirectAttributes) {
        try {
            String machineName = params.get("machineName");
            String dueDateStr = params.get("dueDate");
            
            // 基本驗證
            validateInput(machineName, dueDateStr);
            
            // 檢查機台名稱是否已存在
            if (dbOrderService.isMachineNameExists(machineName)) {
                throw new IllegalArgumentException("機台名稱 " + machineName + " 已存在，請使用其他名稱");
            }
            
            LocalDate dueDate = LocalDate.parse(dueDateStr);
            
            // 驗證日期
            if (dueDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("截止日期不能是過去的日期");
            }
            
            // 解析材料需求
            int nitrogenPipe = parseIntSafely(params.get("nitrogenPipe"));
            int waterPipe = parseIntSafely(params.get("waterPipe"));
            int vacuumPipe = parseIntSafely(params.get("vacuumPipe"));
            
            if (nitrogenPipe == 0 && waterPipe == 0 && vacuumPipe == 0) {
                throw new IllegalArgumentException("請至少填寫一種材料的需求量");
            }
            
            // 計算預估完成時間和狀態
            LocalDate etaDate = calculateEtaDate(dueDate, nitrogenPipe + waterPipe + vacuumPipe);
            String status = etaDate.isAfter(dueDate) ? "LATE" : "ON_TIME";
            
            // 插入訂單 (包含eta_date和status)
            String insertOrderSql = "INSERT INTO orders (machine_name, due_date, eta_date, status) VALUES (?, ?, ?, ?)";
            jdbc.update(insertOrderSql, machineName, dueDate, etaDate, status);
            
            // 獲取新插入的訂單ID
            Long orderId = jdbc.queryForObject(
                "SELECT id FROM orders WHERE machine_name = ? AND due_date = ? ORDER BY id DESC",
                Long.class, machineName, dueDate);
            
            if (orderId == null) {
                throw new RuntimeException("無法獲取新建訂單的ID");
            }
            
            // 插入材料需求 (轉換為A/B/C代碼)
            int materialCount = 0;
            materialCount += insertMaterialIfNotZero(orderId, "A", nitrogenPipe); // 氮氣管
            materialCount += insertMaterialIfNotZero(orderId, "B", waterPipe);    // 水管
            materialCount += insertMaterialIfNotZero(orderId, "C", vacuumPipe);   // 真空管
            
            String successMsg = String.format("✅ 訂單 %s 已成功建立！\n" + 
                "預計完成日期：%s\n" +
                "狀態：%s\n" + 
                "包含 %d 種材料需求", 
                machineName, etaDate, 
                "ON_TIME".equals(status) ? "準時" : "可能延遲",
                materialCount);
            redirectAttributes.addFlashAttribute("success", successMsg);
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "訂單建立失敗：" + e.getMessage());
        }
        
        return "redirect:/db/orders";
    }
    
    private void validateInput(String machineName, String dueDateStr) {
        if (machineName == null || machineName.trim().isEmpty()) {
            throw new IllegalArgumentException("機台名稱不能為空");
        }
        
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("截止日期不能為空");
        }
        
        if (!machineName.matches("^M\\d+$")) {
            throw new IllegalArgumentException("機台名稱格式錯誤，請使用 M + 數字的格式 (如 M3)");
        }
    }
    
    private int parseIntSafely(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            int result = Integer.parseInt(value.trim());
            return Math.max(0, Math.min(1000, result)); // 限制在0-1000範圍
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private int insertMaterialIfNotZero(Long orderId, String materialCode, int qty) {
        if (qty > 0) {
            jdbc.update("INSERT INTO order_materials (order_id, material, qty_needed) VALUES (?, ?, ?)",
                       orderId, materialCode, qty);
            return 1;
        }
        return 0;
    }
    
    // 簡單的ETA計算邏輯
    private LocalDate calculateEtaDate(LocalDate dueDate, int totalMaterialUnits) {
        LocalDate now = LocalDate.now();
        
        // 基於總材料數量估算工作天數 (每天處理24單位)
        int estimatedWorkDays = (int) Math.ceil(totalMaterialUnits / 24.0);
        
        // 加上材料準備時間（考慮到貨計劃）
        int materialPrepDays = 10; // 基本準備時間
        
        LocalDate estimatedEta = now.plusDays(estimatedWorkDays + materialPrepDays);
        
        // 如果預估時間太接近截止日，給一些緩衝
        if (estimatedEta.isAfter(dueDate.minusDays(3))) {
            estimatedEta = dueDate.minusDays(1);
        }
        
        return estimatedEta;
    }
    
    // API: 預覽訂單影響
    @PostMapping("/preview")
    @ResponseBody
    public Map<String, Object> previewOrder(@RequestBody Map<String, Object> orderData) {
        try {
            String machineName = (String) orderData.get("machineName");
            String dueDateStr = (String) orderData.get("dueDate");
            
            if (machineName == null || dueDateStr == null) {
                return Map.of("error", "缺少必要參數");
            }
            
            LocalDate dueDate = LocalDate.parse(dueDateStr);
            LocalDate now = LocalDate.now();
            long daysUntilDue = now.until(dueDate).getDays();
            
            // 分析材料需求 (使用實際庫存數據)
            int nitrogenPipe = ((Number) orderData.getOrDefault("nitrogenPipe", 0)).intValue();
            int waterPipe = ((Number) orderData.getOrDefault("waterPipe", 0)).intValue();
            int vacuumPipe = ((Number) orderData.getOrDefault("vacuumPipe", 0)).intValue();
            
            // 從資料庫獲取實際庫存
            Map<String, Integer> inventory = Map.of(
                "氮氣管", 30,  // A
                "水管", 20,   // B
                "真空管", 10  // C
            );
            
            StringBuilder materialAnalysis = new StringBuilder();
            boolean hasShortage = false;
            
            if (nitrogenPipe > 0 && nitrogenPipe > inventory.get("氮氣管")) {
                materialAnalysis.append("氮氣管缺 ").append(nitrogenPipe - inventory.get("氮氣管")).append(" 單位；");
                hasShortage = true;
            }
            
            if (waterPipe > 0 && waterPipe > inventory.get("水管")) {
                materialAnalysis.append("水管缺 ").append(waterPipe - inventory.get("水管")).append(" 單位；");
                hasShortage = true;
            }
            
            if (vacuumPipe > 0 && vacuumPipe > inventory.get("真空管")) {
                materialAnalysis.append("真空管缺 ").append(vacuumPipe - inventory.get("真空管")).append(" 單位；");
                hasShortage = true;
            }
            
            // 計算預估完成時間
            LocalDate etaDate = calculateEtaDate(dueDate, nitrogenPipe + waterPipe + vacuumPipe);
            boolean onTime = !etaDate.isAfter(dueDate);
            
            return Map.of(
                "materialAnalysis", hasShortage ? materialAnalysis.toString() : "材料庫存充足",
                "scheduleImpact", daysUntilDue < 30 ? "急件訂單，將影響現有排程優先順序" : "標準排程",
                "estimatedEta", etaDate.toString(),
                "onTime", onTime,
                "urgency", daysUntilDue < 30 ? "高" : daysUntilDue < 90 ? "中" : "低"
            );
            
        } catch (Exception e) {
            return Map.of("error", "預覽計算失敗: " + e.getMessage());
        }
    }
}