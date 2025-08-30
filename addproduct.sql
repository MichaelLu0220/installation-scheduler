-- ==========================================
-- 修正版資料插入 - 使用A/B/C (char(1)格式)
-- ==========================================

-- 1. 新增分批到貨計劃 (A=氮氣管, B=水管, C=真空管)
INSERT INTO inbound_plans(material, arrival_date, qty) VALUES
-- A材料 (氮氣管) 到貨計劃
('A','2025-08-20', 60),
('A','2025-09-10', 60),
('A','2025-09-30', 50),
-- B材料 (水管) 到貨計劃
('B','2025-08-15', 40),
('B','2025-09-05', 40),
('B','2025-09-25', 40),
-- C材料 (真空管) 到貨計劃
('C','2025-08-25', 30),
('C','2025-09-15', 30),
('C','2025-09-30', 30);

-- 2. 新增測試訂單
INSERT INTO orders(machine_name, due_date, eta_date, status) VALUES 
('M1','2025-10-15', '2025-09-28', 'ON_TIME'),
('M2','2025-12-01', '2025-11-10', 'ON_TIME');

-- 3. 新增材料需求
INSERT INTO order_materials(order_id, material, qty_needed) VALUES
-- M1的材料需求 (A=氮氣管80, B=水管40, C=真空管20)
((SELECT id FROM orders WHERE machine_name = 'M1'), 'A', 80),
((SELECT id FROM orders WHERE machine_name = 'M1'), 'B', 40),
((SELECT id FROM orders WHERE machine_name = 'M1'), 'C', 20),
-- M2的材料需求 (A=氮氣管60, B=水管40, C=真空管20)
((SELECT id FROM orders WHERE machine_name = 'M2'), 'A', 60),
((SELECT id FROM orders WHERE machine_name = 'M2'), 'B', 40),
((SELECT id FROM orders WHERE machine_name = 'M2'), 'C', 20);

-- 4. 驗證結果
SELECT 'Orders' as table_name, COUNT(*) as count FROM orders
UNION ALL
SELECT 'Materials', COUNT(*) FROM order_materials
UNION ALL  
SELECT 'Inventory', COUNT(*) FROM inventory
UNION ALL
SELECT 'Inbound Plans', COUNT(*) FROM inbound_plans;

-- 5. 查看實際資料內容
SELECT '=== 庫存狀況 ===' as info;
SELECT material, qty_on_hand FROM inventory;

SELECT '=== 訂單資料 ===' as info;
SELECT machine_name, due_date, eta_date, status FROM orders;

SELECT '=== 材料需求明細 ===' as info;
SELECT o.machine_name, om.material, om.qty_needed 
FROM order_materials om 
JOIN orders o ON om.order_id = o.id
ORDER BY o.machine_name, om.material;