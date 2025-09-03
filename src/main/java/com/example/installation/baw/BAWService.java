package com.example.installation.baw;

import com.example.installation.model.InstallationJob;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class BAWService {
    private final RestTemplate restTemplate;
    private final BAWConfig config;
    
    public BAWService(RestTemplate bawRestTemplate, BAWConfig config) {
        this.restTemplate = bawRestTemplate;
        this.config = config;
    }
    
    /**
     * 啟動 BAW 流程
     */
    public Map<String, Object> startProcess(InstallationJob job) {
        try {
            String url = config.getBaseUrl() + "/rest/bpm/wle/v1/process";
            
            // 準備請求資料
            Map<String, Object> request = new HashMap<>();
            request.put("processAppName", "InstallSchedulingDemo");
            request.put("processName", "InstallationSchedulingProcess");
            request.put("params", buildJobData(job));
            
            // 設定 Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(config.getUsername(), config.getPassword());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            // 呼叫 BAW
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = response.getBody();
                System.out.println("✅ BAW 流程已啟動: " + result.get("piid"));
                return result;
            } else {
                throw new RuntimeException("BAW 調用失敗: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.err.println("❌ BAW 調用錯誤: " + e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * 查詢流程狀態
     */
    public Map<String, Object> getProcessStatus(String instanceId) {
        try {
            String url = config.getBaseUrl() + "/rest/bpm/wle/v1/process/" + instanceId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(config.getUsername(), config.getPassword());
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            return response.getBody();
            
        } catch (Exception e) {
            System.err.println("❌ 查詢狀態失敗: " + e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
    
    private Map<String, Object> buildJobData(InstallationJob job) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", job.getJobId());
        data.put("customerName", job.getCustomerName());
        data.put("address", job.getAddress());
        data.put("dueDate", job.getSlaDue());
        data.put("priority", job.getPriority());
        
        // 轉換材料清單
        List<Map<String, Object>> materials = new ArrayList<>();
        for (var item : job.getBom()) {
            Map<String, Object> material = new HashMap<>();
            material.put("name", item.getName());
            material.put("qty", item.getQty());
            materials.add(material);
        }
        data.put("materials", materials);
        
        return data;
    }
}