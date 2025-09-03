package com.example.installation.baw;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "baw")
public class BAWConfig {
    private String baseUrl;
    private String username;
    private String password;
    private boolean enabled = false; // ✅ 新增：預設關閉BAW功能
    
    @Bean
    public RestTemplate bawRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // ✅ 新增：設定連線逾時
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "Installation-Scheduling-Demo/1.0");
            return execution.execute(request, body);
        });
        return restTemplate;
    }
    
    // ✅ 新增：配置驗證方法
    public boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.trim().isEmpty() 
               && username != null && !username.trim().isEmpty()
               && password != null && !password.trim().isEmpty();
    }
    
    // Getters and Setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}