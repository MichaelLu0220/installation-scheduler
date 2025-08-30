INSERT INTO inventory(material, qty_on_hand) VALUES ('A',30),('B',20),('C',10);

INSERT INTO inbound_plans(material, arrival_date, qty) VALUES
('A','2025-08-20',60),('A','2025-09-10',60),('A','2025-09-30',50),
('B','2025-08-15',40),('B','2025-09-05',40),('B','2025-09-25',40),
('C','2025-08-25',30),('C','2025-09-15',30),('C','2025-09-30',30);

INSERT INTO worker_capacity(work_date, hours_total) VALUES
('2025-08-15',24),('2025-08-16',24),('2025-08-17',24),('2025-08-18',24),('2025-08-19',24),
('2025-08-20',24),('2025-08-21',24),('2025-08-22',24),('2025-08-23',24),('2025-08-24',24),
('2025-08-25',24),('2025-08-26',24),('2025-08-27',24),('2025-08-28',24),('2025-08-29',24),
('2025-08-30',24),('2025-08-31',24),('2025-09-01',24),('2025-09-02',24),('2025-09-03',24),
('2025-09-04',24),('2025-09-05',24),('2025-09-06',24),('2025-09-07',24),('2025-09-08',24),
('2025-09-09',24),('2025-09-10',24),('2025-09-11',24),('2025-09-12',24),('2025-09-13',24);

INSERT INTO orders(machine_name, due_date) VALUES ('M1','2025-10-15'),('M2','2025-12-01');

INSERT INTO order_materials(order_id, material, qty_needed) SELECT id,'A',80 FROM orders WHERE machine_name='M1';
INSERT INTO order_materials(order_id, material, qty_needed) SELECT id,'B',40 FROM orders WHERE machine_name='M1';
INSERT INTO order_materials(order_id, material, qty_needed) SELECT id,'C',20 FROM orders WHERE machine_name='M1';

INSERT INTO order_materials(order_id, material, qty_needed) SELECT id,'A',60 FROM orders WHERE machine_name='M2';
INSERT INTO order_materials(order_id, material, qty_needed) SELECT id,'B',40 FROM orders WHERE machine_name='M2';
INSERT INTO order_materials(order_id, material, qty_needed) SELECT id,'C',20 FROM orders WHERE machine_name='M2';
