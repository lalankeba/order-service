package com.laan.orderservice.converter;

import com.laan.orderservice.entity.OrderEntity;
import com.laan.orderservice.entity.OrderProductEntity;
import com.laan.orderservice.response.OrderResponse;
import com.laan.orderservice.response.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to convert dto objects to entities and vice versa
 *
 * @author Lalanke Athauda
 */
@Component
public class OrderConverter {

    public OrderResponse convertEntityToResponse(OrderEntity orderEntity) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(orderEntity.getId());
        orderResponse.setVersion(orderEntity.getVersion());
        orderResponse.setStatus(orderEntity.getStatus());
        orderResponse.setTotalPrice(orderEntity.getTotalPrice());

        return orderResponse;
    }

    public OrderResponse convertEntityToResponse(OrderEntity orderEntity, List<OrderProductEntity> orderProductEntities) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(orderEntity.getId());
        orderResponse.setVersion(orderEntity.getVersion());
        orderResponse.setStatus(orderEntity.getStatus());
        orderResponse.setTotalPrice(orderEntity.getTotalPrice());
        orderResponse.setProducts(convertProductEntitiesToResponses(orderProductEntities));

        return orderResponse;
    }

    private ProductResponse convertProductEntityToResponse(OrderProductEntity orderProductEntity) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(orderProductEntity.getProductEntity().getId());
        productResponse.setQuantity(orderProductEntity.getQuantity());
        return productResponse;
    }

    private List<ProductResponse> convertProductEntitiesToResponses(List<OrderProductEntity> orderProductEntities) {
        List<ProductResponse> productResponses = new ArrayList<>();
        for (OrderProductEntity orderProductEntity : orderProductEntities) {
            ProductResponse productResponse = convertProductEntityToResponse(orderProductEntity);
            productResponses.add(productResponse);
        }
        return productResponses;
    }

    public List<OrderResponse> convertEntitiesToResponses(List<OrderEntity> orderEntities) {
        List<OrderResponse> orderResponses = new ArrayList<>();
        for (OrderEntity orderEntity : orderEntities) {
            OrderResponse orderResponse = convertEntityToResponse(orderEntity);
            orderResponses.add(orderResponse);
        }
        return orderResponses;
    }
}
