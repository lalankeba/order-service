package com.laan.orderservice.repository;

import com.laan.orderservice.entity.OrderEntity;
import com.laan.orderservice.entity.OrderProductEntity;
import com.laan.orderservice.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderProductRepository extends JpaRepository<OrderProductEntity, Long> {

    Optional<OrderProductEntity> findByOrderEntityAndProductEntity(OrderEntity orderEntity, ProductEntity productEntity);

    Optional<List<OrderProductEntity>> findAllByOrderEntity(OrderEntity orderEntity);
}
