-- ==========================================
-- �ץ�����ƴ��J - �ϥ�A/B/C (char(1)�榡)
-- ==========================================

-- 1. �s�W�����f�p�� (A=����, B=����, C=�u�ź�)
INSERT INTO inbound_plans(material, arrival_date, qty) VALUES
-- A���� (����) ��f�p��
('A','2025-08-20', 60),
('A','2025-09-10', 60),
('A','2025-09-30', 50),
-- B���� (����) ��f�p��
('B','2025-08-15', 40),
('B','2025-09-05', 40),
('B','2025-09-25', 40),
-- C���� (�u�ź�) ��f�p��
('C','2025-08-25', 30),
('C','2025-09-15', 30),
('C','2025-09-30', 30);

-- 2. �s�W���խq��
INSERT INTO orders(machine_name, due_date, eta_date, status) VALUES 
('M1','2025-10-15', '2025-09-28', 'ON_TIME'),
('M2','2025-12-01', '2025-11-10', 'ON_TIME');

-- 3. �s�W���ƻݨD
INSERT INTO order_materials(order_id, material, qty_needed) VALUES
-- M1�����ƻݨD (A=����80, B=����40, C=�u�ź�20)
((SELECT id FROM orders WHERE machine_name = 'M1'), 'A', 80),
((SELECT id FROM orders WHERE machine_name = 'M1'), 'B', 40),
((SELECT id FROM orders WHERE machine_name = 'M1'), 'C', 20),
-- M2�����ƻݨD (A=����60, B=����40, C=�u�ź�20)
((SELECT id FROM orders WHERE machine_name = 'M2'), 'A', 60),
((SELECT id FROM orders WHERE machine_name = 'M2'), 'B', 40),
((SELECT id FROM orders WHERE machine_name = 'M2'), 'C', 20);

-- 4. ���ҵ��G
SELECT 'Orders' as table_name, COUNT(*) as count FROM orders
UNION ALL
SELECT 'Materials', COUNT(*) FROM order_materials
UNION ALL  
SELECT 'Inventory', COUNT(*) FROM inventory
UNION ALL
SELECT 'Inbound Plans', COUNT(*) FROM inbound_plans;

-- 5. �d�ݹ�ڸ�Ƥ��e
SELECT '=== �w�s���p ===' as info;
SELECT material, qty_on_hand FROM inventory;

SELECT '=== �q���� ===' as info;
SELECT machine_name, due_date, eta_date, status FROM orders;

SELECT '=== ���ƻݨD���� ===' as info;
SELECT o.machine_name, om.material, om.qty_needed 
FROM order_materials om 
JOIN orders o ON om.order_id = o.id
ORDER BY o.machine_name, om.material;