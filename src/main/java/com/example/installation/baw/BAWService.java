package com.example.installation.baw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.installation.model.InstallationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BAWService {

    private static final Logger logger = LoggerFactory.getLogger(BAWService.class);

    private final RestTemplate restTemplate;
    private final BAWConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JWT Token 快取
     */
    private String jwtToken = null;
    private long tokenExpiryTime = 0;

    public BAWService(RestTemplate bawRestTemplate, BAWConfig config) {
        this.restTemplate = bawRestTemplate;
        this.config = config;
    }

    /**
     * 取得有效的 JWT Token
     */
    private String getValidJwtToken() {
        // 檢查現有 token 是否還有效 (提前5分鐘更新)
        if (jwtToken != null && System.currentTimeMillis() < (tokenExpiryTime - 300000)) {
            return jwtToken;
        }

        try {
            logger.info("🔑 取得新的 JWT Token...");
            
            String loginUrl = config.getBaseUrl() + "/ops/system/login";
            logger.info("🌐 登入 URL: {}", loginUrl);
            logger.info("👤 使用帳號: {}", config.getUsername());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            String authHeader = config.createBasicAuthHeader();
            headers.set("Authorization", authHeader);
            
            logger.debug("🔐 Authorization Header: {}", authHeader);

            // ✅ 加入正確的 request body
            Map<String, Object> loginBody = Map.of(
                "refresh_groups", true,
                "requested_lifetime", 7200
            );
            String jsonBody = objectMapper.writeValueAsString(loginBody);
            
            logger.debug("📤 Request Body: {}", jsonBody);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            
            logger.info("📤 發送登入請求...");
            ResponseEntity<String> response = restTemplate.exchange(loginUrl, HttpMethod.POST, request, String.class);
            
            logger.info("📥 收到回應: Status={}", response.getStatusCode());
            logger.debug("📥 Response Headers: {}", response.getHeaders());
            logger.debug("📥 Response Body: {}", response.getBody());
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("JWT 登入失敗: " + response.getStatusCode());
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            jwtToken = jsonNode.path("csrf_token").asText();
            long expiration = jsonNode.path("expiration").asLong();
            tokenExpiryTime = System.currentTimeMillis() + (expiration * 1000);

            logger.info("✅ JWT Token 取得成功，有效期至: {}", new java.util.Date(tokenExpiryTime));
            logger.debug("🎫 Token: {}", jwtToken.substring(0, Math.min(50, jwtToken.length())) + "...");
            
            return jwtToken;

        } catch (Exception e) {
            logger.error("❌ 取得 JWT Token 失敗", e);
            
            // 額外的錯誤診斷
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                var httpError = (org.springframework.web.client.HttpClientErrorException) e;
                logger.error("🔍 HTTP Status Code: {}", httpError.getStatusCode());
                logger.error("🔍 Response Body: {}", httpError.getResponseBodyAsString());
                logger.error("🔍 Response Headers: {}", httpError.getResponseHeaders());
            }
            
            throw new RuntimeException("Failed to get JWT token: " + e.getMessage(), e);
        }
    }

    /**
     * 啟動流程 - 使用 JWT 認證
     */
    public String startProcessRaw(String bpdId, Map<String, Object> params) {
    	
        try {
            // 檢查配置是否完整
            if (!config.isConfigured()) {
                throw new IllegalStateException("BAW 配置不完整，請檢查 application.properties");
            }

            if (bpdId == null || bpdId.isBlank()) {
                throw new IllegalArgumentException("bpdId is required");
            }
            

            // 取得 JWT Token
            String token = getValidJwtToken();

            // 使用配置中的 URL (可能需要調整路徑)
            String url = config.getBaseUrl() + "/rest/bpm/wle/v1/process"
                    + "?action=start"
                    + "&bpdId=" + bpdId
                    + "&processAppId=" + config.getProcessAppId()
                    + "&parts=all";

            // 使用 JWT Token 建立 Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("BPMCSRFToken", token);
            String authHeader = config.createBasicAuthHeader();
            headers.set("Authorization", authHeader);

            // Body = 業務參數 JSON
            String jsonBody = (params == null) ? "{}" : objectMapper.writeValueAsString(params);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            logger.info("🚀 啟動 BAW 流程: URL={}, BPD={}, jsonBody={}", url, bpdId, jsonBody);

            // 呼叫 API
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            logFullResponse("START_PROCESS", response);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("啟動流程失敗: " + response.getStatusCode() + " - " + response.getBody());
            }

            // 解析 PIID
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String piid = jsonNode.path("data").path("piid").asText();
            
            if (piid.isEmpty()) {
                throw new RuntimeException("無法從回應中取得 PIID");
            }

            logger.info("✅ BAW 流程啟動成功: PIID={}", piid);
            return piid;

        } catch (Exception e) {
            logger.error("❌ 啟動 BAW 流程失敗", e);
            throw new RuntimeException("Start process failed: " + e.getMessage(), e);
        }
    }

    /**
     * 啟動安裝排程流程 (高階包裝方法)
     */
    public Map<String, Object> startProcess(InstallationJob job) {
        try {
            if (!config.isEnabled()) {
                logger.warn("⚠️ BAW 功能已停用，返回模擬結果");
                return Map.of(
                    "enabled", false,
                    "message", "BAW integration disabled",
                    "mockPiid", "MOCK-" + System.currentTimeMillis()
                );
            }

            if (!config.isConfigured()) {
                logger.error("❌ BAW 配置不完整");
                return Map.of("error", "BAW 配置不完整，請檢查 application.properties");
            }

            // 準備流程參數
            Map<String, Object> processParams = buildJobData(job);
            
            // 使用配置中的預設值
            String bpdId = config.getDefaultBpdId();
            
            if (bpdId == null || bpdId.isEmpty()) {
                throw new IllegalArgumentException("BPD ID 未設定，請在 application.properties 中設定 baw.default-bpd-id");
            }
            
            
            
            String piid = startProcessRaw(bpdId, processParams);
            
            return Map.of(
                "success", true,
                "piid", piid,
                "jobId", job.getJobId(),
                "bpdId", bpdId,
                "message", "BAW 流程已啟動"
            );
            
        } catch (Exception e) {
            logger.error("❌ BAW 流程啟動失敗: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 查詢流程狀態 - 使用 JWT 認證
     */
    public Map<String, Object> getProcessStatus(String instanceId) {
        try {
            if (!config.isConfigured()) {
                throw new IllegalStateException("BAW 配置不完整");
            }

            String token = getValidJwtToken();
            String url = config.getBaseUrl() + "/rest/bpm/wle/v1/process" + instanceId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("BPMCSRFToken", token);
            headers.set("Authorization", "Bearer " + token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            logFullResponse("GET_PROCESS_STATUS", response);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("查詢狀態失敗: " + response.getStatusCode());
            }

            // 解析回應
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            Map<String, Object> result = objectMapper.convertValue(jsonNode, Map.class);
            
            logger.info("✅ 查詢流程狀態成功: PIID={}", instanceId);
            return result;

        } catch (Exception e) {
            logger.error("❌ 查詢流程狀態失敗: PIID={}", instanceId, e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 轉換工單資料為 BAW 參數格式
     */
    private Map<String, Object> buildJobData(InstallationJob job) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> orders = new ArrayList<>();
        Map<String, Object> order = new java.util.LinkedHashMap<>();
        order.put("orderId", job.getJobId());
        String machineName = "";
        if (job.getJobId() != null && job.getJobId().contains("-")) {
            String[] parts = job.getJobId().split("-");
            if (parts.length >= 2) {
                machineName = parts[1];
            }
        }
        order.put("machineName", machineName);
        String due = job.getSlaDue();
        if (due != null && due.length() >= 10) {
            due = due.substring(0, 10);
        }
        order.put("dueDate", due);
        Map<String, Object> needs = new java.util.LinkedHashMap<>();
        if (job.getBom() != null) {
            for (var item : job.getBom()) {
                needs.put(item.getItemCode(), item.getQty());
            }
        }
        order.put("needs", needs);

        orders.add(order);
        data.put("orders", orders);


        logger.info("📦 BAW 流程參數: {}", data);
        return data;
    }

    /**
     * 印出完整 Response，方便 debug
     */
    private void logFullResponse(String tag, ResponseEntity<String> response) {
        if (logger.isDebugEnabled()) {
            logger.debug("=== {} ===", tag);
            logger.debug("Status: {}", response.getStatusCode());
            logger.debug("Headers: {}", response.getHeaders());
            logger.debug("Body: {}", response.getBody());
        } else {
            logger.info("BAW API {} - Status: {}", tag, response.getStatusCode());
        }
    }
}