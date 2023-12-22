CREATE TABLE orders (
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    quantity INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30),
    amount DECIMAL(19, 2),
    PRIMARY KEY (order_id)
);
