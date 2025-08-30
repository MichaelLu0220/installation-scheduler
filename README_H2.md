# 智慧安裝排程平台（Tomcat WAR + H2）

## 開發快速啟動（推薦，不用外部 Tomcat）
```
mvn spring-boot:run
```
瀏覽：
- `http://localhost:8080/installation/dashboard`
- `http://localhost:8080/installation/db/orders`
- `http://localhost:8080/installation/h2-console`（JDBC: `jdbc:h2:mem:installdb`, user: `sa`）

## WAR 佈署（外部 Tomcat）
```
mvn clean package
# 丟 target/installation-scheduling-demo-0.1.0.war 到 <TOMCAT_HOME>/webapps/
# 若 Tomcat 模組 Path 是 /installation-scheduling-demo，實際 URL：
# http://localhost:8080/installation-scheduling-demo/installation/dashboard
```

## 資料庫
- `schema.sql`：orders / order_materials / inventory / inbound_plans / worker_capacity / schedule_results
- `data.sql`：初始庫存與到貨、工時、兩筆訂單 M1/M2 與材料

## API
- `GET /installation/api/db/orders`
- `GET /installation/api/jobs`
- `GET /installation/api/jobs/{id}`
