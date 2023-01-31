package com.laan.orderservice.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
public class ProductRequest {

    @NotNull(message = "Product id is mandatory")
    private Long id;

    @NotNull(message = "Quantity is mandatory")
    @Min(value = 0, message = "quantity should be positive")
    private Integer quantity;
}
