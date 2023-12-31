package com.joelmaciel.orderservice.domain;

import com.joelmaciel.orderservice.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByProductId(UUID productId, Pageable pageable);

    Page<Order> findByStatus(String status, Pageable pageable);

}
