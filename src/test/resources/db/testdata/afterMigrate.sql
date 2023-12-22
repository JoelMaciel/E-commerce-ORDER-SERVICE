set foreign_key_checks = 0;

delete from orders;

set foreign_key_checks = 1;



INSERT INTO orders (order_id, product_id, quantity, order_date, status, amount) VALUES
('2231c9a2-2a3b-4376-aed4-aa777e6af85a', '0c288c86-c1f3-4211-b59f-f1c52a4a5636', 10, CURRENT_TIMESTAMP, 'CREATED', 199.99),
('e22974b6-ab03-4f8a-b11d-2c470ee7987b', '4c416789-9995-405a-8183-a3278372679c', 5, CURRENT_TIMESTAMP, 'SHIPPED', 299.99),
('3e8ad87a-3284-4cb7-bc5f-dd7a7bab8c10', '4d86abbe-d29f-455c-8fdc-c3e1102fe90c', 3, CURRENT_TIMESTAMP, 'CANCELLED', 99.99);
