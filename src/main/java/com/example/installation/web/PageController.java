package com.example.installation.web;

import com.example.installation.service.JobService;
import com.example.installation.db.DbOrderService;
import com.example.installation.db.DbOrder;
import com.example.installation.db.InventoryStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
        model.addAttribute("inboundPlans", dbOrderService.getUpcomingInboundPlans(10)); // 只顯示最近10個到貨計劃
        model.addAttribute("totalOrders", stats.getTotalOrders());
        model.addAttribute("atRiskOrders", stats.getAtRiskOrders());
        model.addAttribute("onTimeRate", stats.getOnTimeRate());
        model.addAttribute("lowStockMaterials", stats.getLowStockMaterials());
        
        return "dashboard";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam(value = "q", required = false) String q, Model model) {
        // 保持原有的JobService邏輯，但可以考慮之後整合到DbOrderService
        model.addAttribute("jobs", jobService.search(q));
        model.addAttribute("q", q == null ? "" : q);
        return "jobs";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetail(@PathVariable String id, Model model) {
        var job = jobService.find(id).orElse(null);
        model.addAttribute("job", job);
        return "job-detail";
    }

    @GetMapping("/scheduler")
    public String scheduler(Model model) {
        // 從DB載入排程資料
        List<DbOrder> orders = dbOrderService.list();
        model.addAttribute("orders", orders);
        return "scheduler";
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