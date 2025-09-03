package com.example.installation.web;

import com.example.installation.baw.BAWService;
import com.example.installation.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/baw")
public class BAWController {
    private final BAWService bawService;
    private final JobService jobService;
    
    public BAWController(BAWService bawService, JobService jobService) {
        this.bawService = bawService;
        this.jobService = jobService;
    }
    
    /**
     * 啟動 BAW 流程
     */
    @PostMapping("/start/{jobId}")
    public ResponseEntity<Map<String, Object>> startProcess(@PathVariable String jobId) {
        var job = jobService.find(jobId);
        if (job.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = bawService.startProcess(job.get());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 查詢流程狀態
     */
    @GetMapping("/status/{instanceId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String instanceId) {
        Map<String, Object> result = bawService.getProcessStatus(instanceId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * BAW 完成後的 Webhook
     */
    @PostMapping("/webhook/completed")
    public ResponseEntity<String> processCompleted(@RequestBody Map<String, Object> data) {
        System.out.println("🔔 收到 BAW 完成通知: " + data);
        
        // 在這裡更新你的訂單狀態
        String orderId = (String) data.get("orderId");
        String status = (String) data.get("status");
        
        // TODO: 更新資料庫中的訂單狀態
        
        return ResponseEntity.ok("OK");
    }
}