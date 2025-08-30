package com.example.installation.web;

import com.example.installation.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {
    private final JobService jobService;
    
    public PageController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("jobs", jobService.list());
        long atRisk = jobService.list().stream()
            .filter(j -> j.getBom().stream()
                .anyMatch(b -> (b.getReservedQty() == null ? 0 : b.getReservedQty()) < b.getQty()))
            .count();
        long completed = jobService.list().stream()
            .filter(j -> "Completed".equals(j.getStatus()))
            .count();
        int progress = (int) Math.round((completed * 100.0) / Math.max(1, jobService.list().size()));
        
        model.addAttribute("atRisk", atRisk);
        model.addAttribute("progress", progress);
        return "dashboard";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam(value = "q", required = false) String q, Model model) {
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
    public String scheduler() {
        return "scheduler";
    }

    @GetMapping("/workers")
    public String workers() {
        return "workers";
    }
    
    // 新增：訂單輸入頁面
    @GetMapping("/order-input") 
    public String orderInput() {
        return "order-input";
    }
}