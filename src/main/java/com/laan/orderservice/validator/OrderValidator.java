package com.laan.orderservice.validator;

import com.laan.orderservice.entity.OrderEntity;
import com.laan.orderservice.entity.OrderProductEntity;
import com.laan.orderservice.entity.ProductEntity;
import com.laan.orderservice.entity.UserEntity;
import com.laan.orderservice.enums.OrderStatus;
import com.laan.orderservice.exception.*;
import com.laan.orderservice.repository.OrderProductRepository;
import com.laan.orderservice.repository.OrderRepository;
import com.laan.orderservice.repository.ProductRepository;
import com.laan.orderservice.repository.UserRepository;
import com.laan.orderservice.request.OrderRequest;
import com.laan.orderservice.request.ProductRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class to validate order details
 *
 * @author Lalanke Athauda
 */
@Component
public class OrderValidator {

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final OrderRepository orderRepository;

    private final OrderProductRepository orderProductRepository;

    @Autowired
    public OrderValidator(UserRepository userRepository, ProductRepository productRepository, OrderRepository orderRepository,
                          OrderProductRepository orderProductRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderProductRepository = orderProductRepository;
    }

    public void validateNewOrderRequest(OrderRequest orderRequest) {
        validateUserById(orderRequest.getUserId());
        List<ProductRequest> productRequests = orderRequest.getProducts();
        for (ProductRequest productRequest : productRequests) {
            // check product ids are actually available
            Optional<ProductEntity> optionalProductEntity = productRepository.findById(productRequest.getId());
            if (optionalProductEntity.isEmpty()) {
                throw new ProductNotFoundException("Product cannot be found for the id: " + productRequest.getId());
            }

            // check valid quantities
            if (productRequest.getQuantity() < 1) {
                throw new QuantityMismatchException("Quantity: " + productRequest.getQuantity() + " must be a positive value for product id: " + productRequest.getId());
            }

            // check the stocks for availability
            ProductEntity productEntity = optionalProductEntity.get();
            if (productEntity.getQuantity() < productRequest.getQuantity()) {
                throw new QuantityMismatchException("Cannot supply " + productRequest.getQuantity() + " product/s for product id: " + productRequest.getId() + ". Only " + productEntity.getQuantity() + " available.");
            }
        }
    }

    public void validateOrderId(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Order cannot be found for id: " + id);
        }
    }

    public void validateOrderStatus(Long id, OrderStatus newOrderStatus) {
        OrderEntity orderEntity = orderRepository.findById(id).get();
        OrderStatus currentStatus = orderEntity.getStatus();
        if (newOrderStatus == OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("Order status cannot be turned to " + OrderStatus.PENDING);
        } else if (newOrderStatus == OrderStatus.PROCESSING) {
            if (currentStatus != OrderStatus.PENDING) {
                throw new InvalidOrderStatusException("Order cannot be turned to " + OrderStatus.PROCESSING);
            }
        } else if (newOrderStatus == OrderStatus.COMPLETED) {
            if (currentStatus != OrderStatus.PROCESSING) {
                throw new InvalidOrderStatusException("Order cannot be turned to " + OrderStatus.COMPLETED);
            }
        }
    }

    private void validateOrderVersion(Long id, OrderRequest orderRequest) {
        if (!orderRepository.findById(id).get().getVersion().equals(orderRequest.getVersion())) {
            throw new OrderNotFoundException("Order cannot be found due to version mismatch");
        }
    }

    public void validateOrderIsInDeletableState(Long id) {
        OrderEntity orderEntity = orderRepository.findById(id).get();
        if (orderEntity.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotFoundException("Order is not in a deletable state. Only pending orders can be deleted.");
        }
    }

    public void validateOrderRequest(Long id, OrderRequest orderRequest) {
        // validate order id
        validateOrderId(id);
        validateOrderVersion(id, orderRequest);

        // validate user with existing users and make sure the order is changing only by its owning user
        validateUserById(orderRequest.getUserId());
        validateOrderUserById(orderRequest.getUserId(), id);

        // status must be in pending or processing state to be updated
        OrderEntity orderEntity = orderRepository.findById(id).get();
        if (orderEntity.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("Order cannot be updated since it's in " + orderEntity.getStatus() + " state.");
        }

        // checking availability of stocks
        List<ProductRequest> productRequests = orderRequest.getProducts();
        for (ProductRequest productRequest : productRequests) {
            // checks product ids in request, can be found in products
            Optional<ProductEntity> optionalProductEntity = productRepository.findById(productRequest.getId());
            if (optionalProductEntity.isEmpty()) {
                throw new ProductNotFoundException("Product cannot be found for the id: " + productRequest.getId());
            }

            // check valid quantities
            if (productRequest.getQuantity() < 0) {
                throw new QuantityMismatchException("Quantity: " + productRequest.getQuantity() + " must be a positive value for product id: " + productRequest.getId());
            }

            ProductEntity productEntity = optionalProductEntity.get();

            // select order-product table by order id
            Optional<OrderProductEntity> optionalExistingOrderProductEntity = orderProductRepository.findByOrderEntityAndProductEntity(orderEntity, productEntity);
            if (optionalExistingOrderProductEntity.isPresent()) { // same product in existing order
                // needs to check quantities only if quantities different from earlier order
                OrderProductEntity existingOrderProductEntity = optionalExistingOrderProductEntity.get();
                Integer difference = productRequest.getQuantity() - existingOrderProductEntity.getQuantity();
                if (difference > 0) { // new value is greater than earlier one
                    if (productEntity.getQuantity() - difference < 0) {
                        throw new QuantityMismatchException("Cannot provide " + difference + " more product/s for id: " + productRequest.getId());
                    }
                }
            } else { // new product for existing order
                // needs to check quantity is available in the stock
                if (productEntity.getQuantity() - productRequest.getQuantity() < 0) {
                    throw new QuantityMismatchException("Cannot supply " + productRequest.getQuantity() + " product/s for product id: " + productRequest.getId());
                }
            }
        }
    }

    private void validateUserById(Long id) {
        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);
        if (optionalUserEntity.isEmpty()) {
            throw new UserNotFoundException("User cannot be found for the id: " + id);
        }
    }

    private void validateOrderUserById(Long userId, Long orderId) {
        Optional<OrderEntity> optionalOrderEntity = orderRepository.findById(orderId);
        if (optionalOrderEntity.isPresent()) {
            Long existingUserId = optionalOrderEntity.get().getUserEntity().getId();
            if (!Objects.equals(userId, existingUserId)) {
                throw new UserNotFoundException("User id: " + userId + " cannot be found in the existing order with id: " + orderId);
            }
        }
    }

}
