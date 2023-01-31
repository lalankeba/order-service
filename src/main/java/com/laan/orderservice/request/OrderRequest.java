package com.laan.orderservice.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Contains details of new Order or update order requests
 *
 * @author Lalanke Athauda
 */
@Getter
@Setter
@ToString
public class OrderRequest {

    @NotNull(message = "user id is mandatory")
    private Long userId;

    private Long version;

    @NotEmpty(message = "Order should have at least one product")
    @Valid private List<ProductRequest> products;

}
