package com.laan.orderservice.service.impl;

import com.laan.orderservice.converter.OrderConverter;
import com.laan.orderservice.entity.OrderEntity;
import com.laan.orderservice.entity.OrderProductEntity;
import com.laan.orderservice.entity.ProductEntity;
import com.laan.orderservice.entity.UserEntity;
import com.laan.orderservice.enums.OrderStatus;
import com.laan.orderservice.repository.OrderProductRepository;
import com.laan.orderservice.repository.OrderRepository;
import com.laan.orderservice.repository.ProductRepository;
import com.laan.orderservice.repository.UserRepository;
import com.laan.orderservice.request.OrderRequest;
import com.laan.orderservice.request.ProductRequest;
import com.laan.orderservice.response.OrderResponse;
import com.laan.orderservice.service.OrderService;
import com.laan.orderservice.validator.OrderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.jms.Queue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Order processing service
 *
 * @author Lalanke Athauda
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderValidator orderValidator;

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final OrderRepository orderRepository;

    private final OrderProductRepository orderProductRepository;

    private final OrderConverter orderConverter;

    private final JmsTemplate jmsTemplate;

    private final Queue queue;

    @Autowired
    public OrderServiceImpl(OrderValidator orderValidator, UserRepository userRepository, ProductRepository productRepository,
                            OrderRepository orderRepository, OrderProductRepository orderProductRepository, OrderConverter orderConverter,
                            JmsTemplate jmsTemplate, Queue queue) {
        this.orderValidator = orderValidator;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderProductRepository = orderProductRepository;
        this.orderConverter = orderConverter;
        this.jmsTemplate = jmsTemplate;
        this.queue = queue;
    }

    /**
     * creates a new order
     * @param orderRequest new order request
     * @return OrderResponse saved order details
     */
    @Override
    public OrderResponse addOrder(OrderRequest orderRequest) {
        LOGGER.info("validates new order");
        orderValidator.validateNewOrderRequest(orderRequest);

        UserEntity userEntity = userRepository.findById(orderRequest.getUserId()).get();

        List<ProductRequest> productRequests = orderRequest.getProducts();

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserEntity(userEntity);
        orderEntity.setStatus(OrderStatus.PENDING);

        List<OrderProductEntity> orderProductEntities = createOrderProductEntities(productRequests, orderEntity);

        orderEntity.setTotalPrice(calculateOrderTotalPrice(orderProductEntities));
        orderEntity.setCreatedTimestamp(new Date());

        LOGGER.info("saves new order");
        OrderEntity savedOrderEntity = orderRepository.save(orderEntity);
        orderProductRepository.saveAll(orderProductEntities);

        return orderConverter.convertEntityToResponse(savedOrderEntity, orderProductEntities);
    }

    /**
     * Retrieves all orders
     * @return List of all orders
     */
    @Override
    public List<OrderResponse> getOrders() {
        List<OrderEntity> orderEntities = orderRepository.findAll();
        return orderConverter.convertEntitiesToResponses(orderEntities);
    }

    /**
     * Retrieves the order specified by the id
     * @param id order id
     * @return Order of specified id
     */
    @Override
    public OrderResponse getOrder(Long id) {
        LOGGER.info("validates order with existing data");
        orderValidator.validateOrderId(id);
        OrderEntity orderEntity = orderRepository.findById(id).get();
        List<OrderProductEntity> orderProductEntities = orderProductRepository.findAllByOrderEntity(orderEntity).get();
        return orderConverter.convertEntityToResponse(orderEntity, orderProductEntities);
    }

    /**
     * Updates the existing order
     * @param id existing order id
     * @param orderRequest Order details to be updated
     * @return OrderResponse with updated order details
     */
    @Override
    public OrderResponse updateOrder(Long id, OrderRequest orderRequest) {
        LOGGER.info("validates order with modified data");
        orderValidator.validateOrderRequest(id, orderRequest);

        OrderEntity orderEntity = orderRepository.findById(id).get();
        List<OrderProductEntity> orderProductEntities = new ArrayList<>();

        List<ProductRequest> productRequests = orderRequest.getProducts();
        for (ProductRequest productRequest : productRequests) {
            ProductEntity productEntity = productRepository.findById(productRequest.getId()).get();

            // select order-product table by order id
            Optional<OrderProductEntity> optionalExistingOrderProductEntity = orderProductRepository.findByOrderEntityAndProductEntity(orderEntity, productEntity);
            if (optionalExistingOrderProductEntity.isPresent()) { // same product in existing order
                // needs to update only if quantities different from earlier order
                OrderProductEntity existingOrderProductEntity = optionalExistingOrderProductEntity.get();
                Integer difference = productRequest.getQuantity() - existingOrderProductEntity.getQuantity();
                if (difference != 0) { // new value is different from the earlier order
                    // update new value in existing one
                    Integer quantity = productRequest.getQuantity();
                    BigDecimal unitPrice = productEntity.getUnitPrice();
                    BigDecimal price = calculateOrderProductPrice(unitPrice, quantity);

                    existingOrderProductEntity.setQuantity(productRequest.getQuantity());
                    existingOrderProductEntity.setPrice(price);

                    // update the stocks
                    productEntity.setQuantity(productEntity.getQuantity() - difference);
                    productRepository.save(productEntity);
                }
                orderProductEntities.add(existingOrderProductEntity);
            } else { // new product for existing order
                // add new items and update stocks
                OrderProductEntity orderProductEntity = createOrderProductEntity(productRequest, productEntity, orderEntity);

                // update product with deducted quantity
                productEntity.setQuantity(productEntity.getQuantity() - orderProductEntity.getQuantity());
                productRepository.save(productEntity);

                orderProductEntities.add(orderProductEntity);
            }
        }

        orderEntity.setTotalPrice(calculateOrderTotalPrice(orderProductEntities));
        orderEntity.setCreatedTimestamp(new Date());

        LOGGER.info("saves order with modified data");
        OrderEntity savedOrderEntity = orderRepository.save(orderEntity);
        orderProductRepository.saveAll(orderProductEntities);

        return orderConverter.convertEntityToResponse(savedOrderEntity, orderProductEntities);
    }

    /**
     * Deletes an existing order
     * @param id order id needs to be deleted
     */
    @Override
    public void deleteOrder(Long id) {
        LOGGER.info("validates the order details before deleting");
        orderValidator.validateOrderId(id);
        orderValidator.validateOrderIsInDeletableState(id);

        OrderEntity orderEntity = orderRepository.findById(id).get();

        Optional<List<OrderProductEntity>> optionalOrderProductEntities = orderProductRepository.findAllByOrderEntity(orderEntity);

        if (optionalOrderProductEntities.isPresent()) {
            List<OrderProductEntity> orderProductEntities = optionalOrderProductEntities.get();

            for (OrderProductEntity orderProductEntity : orderProductEntities) {
                ProductEntity productEntity = orderProductEntity.getProductEntity();

                // update product quantity
                productEntity.setQuantity(productEntity.getQuantity() + orderProductEntity.getQuantity());
                productRepository.save(productEntity);

                // delete order-product
                orderProductRepository.delete(orderProductEntity);
            }
        }

        // delete order
        orderRepository.delete(orderEntity);
    }

    /**
     * Updates the status of the order
     * @param id valid order id
     * @param orderStatus new status to be updated
     * @return OrderResponse with updated status
     */
    public OrderResponse updateOrderStatus(Long id, OrderStatus orderStatus) {
        LOGGER.info("validates the order with new status");
        // validates the order and status
        orderValidator.validateOrderId(id);
        orderValidator.validateOrderStatus(id, orderStatus);

        // updates the order with appropriate status
        OrderEntity orderEntity = orderRepository.findById(id).get();
        orderEntity.setStatus(orderStatus);
        orderRepository.save(orderEntity);

        // put status change to event-queue
        if (orderEntity.getStatus() == OrderStatus.PROCESSING || orderEntity.getStatus() == OrderStatus.COMPLETED) {
            LOGGER.info("adding status updated order details to queue");
            jmsTemplate.convertAndSend(queue, orderEntity);
        }

        return orderConverter.convertEntityToResponse(orderEntity);
    }

    private List<OrderProductEntity> createOrderProductEntities(List<ProductRequest> productRequests,
                                                                OrderEntity orderEntity) {
        List<OrderProductEntity> orderProductEntities = new ArrayList<>();
        for (ProductRequest productRequest : productRequests) {
            ProductEntity productEntity = productRepository.findById(productRequest.getId()).get();
            OrderProductEntity orderProductEntity = createOrderProductEntity(productRequest, productEntity, orderEntity);

            orderProductEntities.add(orderProductEntity);

            // update product with deducted quantity
            productEntity.setQuantity(productEntity.getQuantity() - orderProductEntity.getQuantity());
            productRepository.save(productEntity);
        }
        return orderProductEntities;
    }

    private OrderProductEntity createOrderProductEntity(ProductRequest productRequest, ProductEntity productEntity, OrderEntity orderEntity) {
        Integer quantity = productRequest.getQuantity();
        BigDecimal unitPrice = productEntity.getUnitPrice();
        BigDecimal price = calculateOrderProductPrice(unitPrice, quantity);

        OrderProductEntity orderProductEntity = new OrderProductEntity();
        orderProductEntity.setOrderEntity(orderEntity);
        orderProductEntity.setProductEntity(productEntity);
        orderProductEntity.setQuantity(quantity);
        orderProductEntity.setUnitPrice(unitPrice);
        orderProductEntity.setPrice(price);
        orderProductEntity.setCreatedTimestamp(new Date());

        return orderProductEntity;
    }

    private BigDecimal calculateOrderProductPrice(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal calculateOrderTotalPrice(List<OrderProductEntity> orderProductEntities) {
        BigDecimal totalPrice = new BigDecimal("0");
        for (OrderProductEntity orderProductEntity : orderProductEntities) {
            totalPrice = totalPrice.add(orderProductEntity.getPrice());
        }
        return totalPrice;
    }
}
