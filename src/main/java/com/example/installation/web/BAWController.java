package com.example.installation.web;

import com.example.installation.baw.BAWService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/baw")
public class BAWController {

    private final BAWService bawService;

    public BAWController(BAWService bawService) {
        this.bawService = bawService;
    }

    /**
     * 測試用端點：啟動流程
     */
    @PostMapping("/start")
    public ResponseEntity<?> startProcess(@RequestBody Map<String, Object> request) {
        try {
            // 從 request 取出流程參數
            String bpdId = (String) request.getOrDefault("bpdId", "25.3d550ad9-4f35-48dc-8815-c8e612eec419");
            Map<String, Object> params = (Map<String, Object>) request.get("params");

            String piid = bawService.startProcessRaw(bpdId, params);

            return ResponseEntity.ok(Map.of("piid", piid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
