package com.example.installation.baw;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "baw")
public class BAWConfig {
    private String baseUrl;
    private String username;
    private String password;
    private String processAppId;
    private String defaultBpdId;
    private boolean enabled = true;
    private boolean sslInsecure = true; // 是否跳過 SSL 驗證（只給 DEV）
    
    // 超時設定
    private int connectionTimeout = 30000; // 30秒
    private int readTimeout = 60000; // 60秒

    @Bean
    public RestTemplate bawRestTemplate() throws Exception {
        RestTemplate rt;

        if (sslInsecure) {
            // ⚠️ DEV ONLY: 信任所有憑證 + 關閉主機名驗證
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            // 設定超時
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(connectionTimeout);
            factory.setReadTimeout(readTimeout);
            rt = new RestTemplate(factory);
        } else {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(connectionTimeout);
            factory.setReadTimeout(readTimeout);
            rt = new RestTemplate(factory);
        }

        // 可以加上共用 Header
        rt.setInterceptors(List.of((req, body, ex) -> {
            req.getHeaders().set("X-Requested-By", "installation-scheduler");
            req.getHeaders().set("User-Agent", "Installation-Scheduling-Demo/1.0");
            return ex.execute(req, body);
        }));

        return rt;
    }
    
    /**
     * 配置驗證方法
     */
    public boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.trim().isEmpty() 
               && username != null && !username.trim().isEmpty()
               && password != null && !password.trim().isEmpty()
               && processAppId != null && !processAppId.trim().isEmpty();
    }
    
    /**
     * 取得完整的 BAW REST API Base URL
     */
    public String getRestApiBaseUrl() {
        return baseUrl + "/rest/bpm/wle/v1";
    }
    
    /**
     * 建立 Basic Auth 字串的輔助方法
     */
    public String createBasicAuthHeader() {
        if (username == null || password == null) {
            throw new IllegalStateException("BAW username or password not configured");
        }
        String auth = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder()
            .encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // === Getters and Setters ===
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getProcessAppId() { return processAppId; }
    public void setProcessAppId(String processAppId) { this.processAppId = processAppId; }
    
    public String getDefaultBpdId() { return defaultBpdId; }
    public void setDefaultBpdId(String defaultBpdId) { this.defaultBpdId = defaultBpdId; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isSslInsecure() { return sslInsecure; }
    public void setSslInsecure(boolean sslInsecure) { this.sslInsecure = sslInsecure; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
}