package com.laan.orderservice.service;

import com.laan.orderservice.enums.OrderStatus;
import com.laan.orderservice.request.OrderRequest;
import com.laan.orderservice.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse addOrder(OrderRequest orderRequest);

    List<OrderResponse> getOrders();

    OrderResponse getOrder(Long id);

    OrderResponse updateOrder(Long id, OrderRequest orderRequest);

    OrderResponse updateOrderStatus(Long id, OrderStatus orderStatus);

    void deleteOrder(Long id);

}
