package com.example.installation.web;

import com.example.installation.service.JobService;
import com.example.installation.db.DbOrderService;
import com.example.installation.db.InboundPlan;
import com.example.installation.db.DbOrder;
import com.example.installation.db.InventoryStatus;
import com.example.installation.db.WorkerCapacity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PageController {
    private final JobService jobService;
    private final DbOrderService dbOrderService;
    
    public PageController(JobService jobService, DbOrderService dbOrderService) {
        this.jobService = jobService;
        this.dbOrderService = dbOrderService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        // 從資料庫獲取真實數據
        List<DbOrder> orders = dbOrderService.list();
        List<InventoryStatus> inventoryStatus = dbOrderService.getInventoryStatus();
        DbOrderService.DashboardStats stats = dbOrderService.getDashboardStats();
        
        // 添加到模型
        model.addAttribute("orders", orders);
        model.addAttribute("inventoryStatus", inventoryStatus);
//        model.addAttribute("inboundPlans", dbOrderService.getUpcomingInboundPlans(10)); // 只顯示最近10個到貨計劃
        model.addAttribute("inboundPlans", dbOrderService.getInboundPlans());
        model.addAttribute("totalOrders", stats.getTotalOrders());
        model.addAttribute("atRiskOrders", stats.getAtRiskOrders());
        model.addAttribute("onTimeRate", stats.getOnTimeRate());
        model.addAttribute("lowStockMaterials", stats.getLowStockMaterials());
        
        return "dashboard";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam(value = "q", required = false) String q, Model model) {
        // ✅ 改用真實資料庫資料
        List<DbOrder> orders = dbOrderService.list();
        
        // 搜尋功能
        if (q != null && !q.trim().isEmpty()) {
            String searchTerm = q.toLowerCase().trim();
            orders = orders.stream()
                .filter(order -> 
                    order.getMachineName().toLowerCase().contains(searchTerm) ||
                    order.getId().toString().contains(searchTerm) ||
                    (order.getStatus() != null && order.getStatus().toLowerCase().contains(searchTerm))
                )
                .collect(Collectors.toList());
        }
        
        // 載入庫存狀態用於顯示材料風險
        List<InventoryStatus> inventoryStatus = dbOrderService.getInventoryStatus();
        
        model.addAttribute("orders", orders);
        model.addAttribute("inventoryStatus", inventoryStatus);
        model.addAttribute("q", q == null ? "" : q);
        
        return "jobs";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetail(@PathVariable Long id, Model model) {
        // ✅ 改用資料庫查詢
        DbOrder order = dbOrderService.findById(id);
        if (order == null) {
            // 如果找不到訂單，重定向到工單列表
            return "redirect:/jobs";
        }
        
        model.addAttribute("job", order);
        return "job-detail";
    }

    @GetMapping("/scheduler")
    public String scheduler(Model model) {
        // 從DB載入排程資料
        List<DbOrder> orders = dbOrderService.list();
        List<InventoryStatus> inventoryStatus = dbOrderService.getInventoryStatus();
        List<InboundPlan> inboundPlans = dbOrderService.getInboundPlans();
        List<WorkerCapacity> workerCapacity = dbOrderService.getWorkerCapacity();
        
        // 計算統計數據
        long totalOrders = orders.size();
        long todayTasks = orders.stream()
            .filter(order -> order.getEtaDate() != null && order.getEtaDate().equals(LocalDate.now()))
            .count();
        long lateOrders = orders.stream()
            .filter(order -> "LATE".equals(order.getStatus()))
            .count();
        
        // 計算工人利用率 (簡化計算)
        int totalCapacity = workerCapacity.stream()
            .mapToInt(WorkerCapacity::getTotalUnitsPerDay)
            .findFirst()
            .orElse(24);
        int totalDemand = orders.stream()
            .flatMap(order -> order.getMaterials().stream())
            .mapToInt(material -> material.getQtyNeeded())
            .sum();
        double utilizationRate = totalCapacity > 0 ? Math.min(100.0, (totalDemand * 100.0) / (totalCapacity * 30)) : 0; // 假設30天
        
        // 為甘特圖準備時間軸數據
        List<LocalDate> timelineSpan = generateTimelineSpan();
        
        model.addAttribute("orders", orders);
        model.addAttribute("inventoryStatus", inventoryStatus);
        model.addAttribute("inboundPlans", inboundPlans);
        model.addAttribute("workerCapacity", workerCapacity);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("todayTasks", todayTasks);
        model.addAttribute("lateOrders", lateOrders);
        model.addAttribute("utilizationRate", Math.round(utilizationRate));
        model.addAttribute("timelineSpan", timelineSpan);
        
        return "scheduler";
    }

    // 輔助方法：生成時間軸範圍
    private List<LocalDate> generateTimelineSpan() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now().plusDays(45);
        
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(5)) {
            dates.add(date);
        }
        return dates;
    }

    @GetMapping("/workers")
    public String workers(Model model) {
        // 從DB載入工人產能資料
        model.addAttribute("workerCapacity", dbOrderService.getWorkerCapacity());
        return "workers";
    }
    
    @GetMapping("/order-input") 
    public String orderInput(Model model) {
        // 為新增訂單頁面提供庫存資訊
        model.addAttribute("inventoryStatus", dbOrderService.getInventoryStatus());
        return "order-input";
    }
}