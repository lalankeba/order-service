package com.laan.orderservice.response;

import com.laan.orderservice.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
public class OrderResponse {

    private Long id;

    private Long version;

    private OrderStatus status;

    private BigDecimal totalPrice;

    private List<ProductResponse> products;

}
