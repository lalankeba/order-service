package com.laan.orderservice.controller;

import com.laan.orderservice.enums.OrderStatus;
import com.laan.orderservice.request.OrderRequest;
import com.laan.orderservice.response.OrderResponse;
import com.laan.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Controller for order processing
 *
 * @author Lalanke Athauda
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Object> getOrders() {
        LOGGER.info("get all orders");
        List<OrderResponse> orderResponses = orderService.getOrders();
        LOGGER.info("sent get all orders response");
        return new ResponseEntity<>(orderResponses, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Object> addOrder(@Valid @RequestBody OrderRequest orderRequest) {
        LOGGER.info("adding new order: {}", orderRequest);
        OrderResponse orderResponse = orderService.addOrder(orderRequest);
        LOGGER.info("sent add new order response");
        return new ResponseEntity<>(orderResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getOrder(@PathVariable("id") Long id) {
        LOGGER.info("get order for id: {}", id);
        OrderResponse orderResponse = orderService.getOrder(id);
        LOGGER.info("sent get order response");
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateOrder(@PathVariable("id") Long id, @Valid @RequestBody OrderRequest orderRequest) {
        LOGGER.info("update order for id: {}", id);
        OrderResponse orderResponse = orderService.updateOrder(id, orderRequest);
        LOGGER.info("sent update order response");
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteOrder(@PathVariable("id") Long id) {
        LOGGER.info("delete order for id: {}", id);
        orderService.deleteOrder(id);
        LOGGER.info("order deleted for id: {}", id);
        return new ResponseEntity<>("Successfully deleted", HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<Object> updateOrderStatus(@PathVariable("id") Long id, @PathVariable OrderStatus status) {
        LOGGER.info("update order for id: {} with status: {}", id, status);
        OrderResponse orderResponse = orderService.updateOrderStatus(id, status);
        LOGGER.info("sent update order response");
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

}
