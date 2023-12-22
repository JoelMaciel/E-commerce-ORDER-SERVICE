package com.joelmaciel.orderservice.domain.service.impl;

import com.joelmaciel.orderservice.api.openfeign.client.PaymentService;
import com.joelmaciel.orderservice.api.openfeign.client.ProductService;
import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequestDTO;
import com.joelmaciel.orderservice.api.openfeign.response.TransactionDetailsDTO;
import com.joelmaciel.orderservice.domain.OrderRepository;
import com.joelmaciel.orderservice.domain.entity.Order;
import com.joelmaciel.orderservice.domain.enums.OrderStatus;
import com.joelmaciel.orderservice.domain.exception.OrderNotFoundException;
import com.joelmaciel.orderservice.domain.exception.PaymentServiceException;
import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import com.joelmaciel.orderservice.domain.model.PaymentMode;
import com.joelmaciel.orderservice.domain.model.ProductDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {
    public static final String API_PRODUCTS = "http://PRODUCT-SERVICE/api/products/";
    public static final String API_PAYMENTS = "http://PAYMENT-SERVICE/api/payments/orders/";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID TRANSACTIONAL_ID = UUID.randomUUID();

    @Mock
    private ProductService productService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    OrderServiceImpl orderService;


    @DisplayName("Given Criteria, When findAllOrders is called, Then return a Paged List of Orders")
    @Test
    void givenCriteria_whenFindAllOrdersCalled_thenReturnPagedOrders() {
        PageRequest pageable = PageRequest.of(0, 10);
        UUID productId = UUID.randomUUID();
        String status = "PLACED";

        PageImpl<Order> mockPage = new PageImpl<>(List.of(getMockOrder()));

        when(orderRepository.findByProductId(productId, pageable)).thenReturn(mockPage);
        when(orderRepository.findByStatus(status, pageable)).thenReturn(mockPage);
        when(orderRepository.findAll(pageable)).thenReturn(mockPage);

        Page<OrderDTO> resultWhitProductId = orderService.findAllOrders(pageable, productId, null);
        assertFalse(resultWhitProductId.getContent().isEmpty());

        Page<OrderDTO> resultWhitStatus = orderService.findAllOrders(pageable, null, status);
        assertFalse(resultWhitStatus.getContent().isEmpty());

        Page<OrderDTO> resultNoCriteria = orderService.findAllOrders(pageable, null, null);
        assertFalse(resultNoCriteria.getContent().isEmpty());

    }

    @DisplayName("Given Repository Failure, When findAllOrders is called, Then handle exceptions")
    @Test
    void givenRepositoryFailure_whenFindAllOrdersCalled_thenHandleExceptions() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID productId = UUID.randomUUID();
        String status = OrderStatus.PLACED.toString();

        when(orderRepository.findByProductId(productId, pageable)).thenThrow(new RuntimeException("Database error"));
        when(orderRepository.findByStatus(status, pageable)).thenThrow(new RuntimeException("Database error"));
        when(orderRepository.findAll(pageable)).thenThrow(new RuntimeException("Database error"));

        Exception exceptionForProductId = assertThrows(RuntimeException.class, () -> {
            orderService.findAllOrders(pageable, productId, null);
        });
        assertEquals("Database error", exceptionForProductId.getMessage());

        Exception exceptionForStatus = assertThrows(RuntimeException.class, () -> {
            orderService.findAllOrders(pageable, null, status);
        });
        assertEquals("Database error", exceptionForStatus.getMessage());

        Exception exceptionNoCriteria = assertThrows(RuntimeException.class, () -> {
            orderService.findAllOrders(pageable, null, null);
        });
        assertEquals("Database error", exceptionNoCriteria.getMessage());
    }



    @DisplayName("Given Valid OrderId, getOrderDetails should return complete Order Details with Product and Transaction Information")
    @Test
    void givenValidOrderId_getOrderDetailsShouldReturnCompleteOrderDetails() {
        Order order = getMockOrder();
        UUID orderId = order.getOrderId();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        when(restTemplate.getForObject(API_PRODUCTS + order.getProductId(), ProductDTO.class))
                .thenReturn(getMockProductDTO());

        when(restTemplate.getForObject(API_PAYMENTS + order.getOrderId(), TransactionDetailsDTO.class))
                .thenReturn(getMockTransactionalDetailsDTO());

        OrderDTO orderDTO = orderService.getOrderDetails(orderId);

        verify(orderRepository, times(1)).findById(orderId);

        verify(restTemplate, times(1)).getForObject(
                API_PRODUCTS + order.getProductId(), ProductDTO.class);

        verify(restTemplate, times(1)).getForObject(
                API_PAYMENTS + order.getOrderId(), TransactionDetailsDTO.class);

        assertNotNull(orderDTO);
        assertEquals(order.getOrderId(), orderDTO.getOrderId());
        assertEquals(getMockProductDTO().getName(), orderDTO.getProductDetails().getName());
        assertEquals(getMockProductDTO().getPrice(), orderDTO.getProductDetails().getPrice());
        assertEquals(
                getMockTransactionalDetailsDTO().getTransactionId(),
                orderDTO.getTransactionDetails().getTransactionId());

        assertEquals(getMockTransactionalDetailsDTO().getStatus(), orderDTO.getTransactionDetails().getStatus());

        BigDecimal expectedAmount = getMockProductDTO().getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        assertEquals(expectedAmount, orderDTO.getAmount());
    }

    @DisplayName("Given null OrderId, getOrderDetails should throw OrderNotFoundException")
    @Test
    void givenNullOrderId_getOrderDetailsShouldThrowOrderNotFoundException() {
        String expectedMessage = String.format("There is no order with this id %s saved in the database", ORDER_ID);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderService.getOrderDetails(ORDER_ID));

        assertEquals(expectedMessage, exception.getMessage());
        verify(orderRepository, times(1)).findById(ORDER_ID);

    }


    @DisplayName("Given Valid OrderRequest, savePlaceOrder should successfully process and save the order")
    @Test
    void givenValidOrderRequest_savePlaceOrderShouldSuccessfullyProcessAndSaveOrder() {
        OrderRequest orderRequest = getMockOrderRequest();
        Order mockOrder = OrderRequest.toEntity(orderRequest);
        ProductDTO mockProductDTO = getMockProductDTO();

        when(restTemplate.getForObject(API_PRODUCTS + PRODUCT_ID, ProductDTO.class))
                .thenReturn(mockProductDTO);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(productService.reduceQuantity(PRODUCT_ID, orderRequest.getQuantity()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(paymentService.savePayment(any(PaymentRequestDTO.class))).thenReturn(getMockTransactionalDetailsDTO());

        OrderDTO result = orderService.savePlaceOrder(orderRequest);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(productService, times(1)).reduceQuantity(PRODUCT_ID, orderRequest.getQuantity());
        verify(paymentService, times(1)).savePayment(any(PaymentRequestDTO.class));

        assertNotNull(result);
        assertEquals(mockOrder.getOrderId(), result.getOrderId());

        BigDecimal expectedAmount = mockProductDTO.getPrice().multiply(new BigDecimal(orderRequest.getQuantity()));
        assertEquals(expectedAmount, result.getAmount());
    }

    @DisplayName("Given OrderRequest, savePlaceOrder should handle Payment Failed")
    @Test
    void givenOrderRequest_savePlaceOrderShouldHandleProductServiceFailure() {
        OrderRequest orderRequest = getMockOrderRequest();
        Order mockOrder = OrderRequest.toEntity(orderRequest);
        ProductDTO mockProductDTO = getMockProductDTO();

        when(restTemplate.getForObject(API_PRODUCTS + PRODUCT_ID, ProductDTO.class))
                .thenReturn(mockProductDTO);
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(productService.reduceQuantity(PRODUCT_ID, orderRequest.getQuantity()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(paymentService.savePayment(any(PaymentRequestDTO.class))).thenThrow(PaymentServiceException.class);

        OrderDTO result = orderService.savePlaceOrder(orderRequest);

        verify(orderRepository, times(1)).save(any(Order.class));
        verify(productService, times(1)).reduceQuantity(PRODUCT_ID, orderRequest.getQuantity());
        verify(paymentService, times(1)).savePayment(any(PaymentRequestDTO.class));

        assertEquals(mockOrder.getOrderId(), result.getOrderId());
    }

    @DisplayName("Given Valid OrderId, findOrderById should successfully fetch the Order from the Repository")
    @Test
    void givenValidOrderId_findOrderByIdShouldSuccessfullyFetchFromRepository() {
        UUID orderId = ORDER_ID;
        Order orderExpected = getMockOrder();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderExpected));

        Order actualOrder = orderService.findOrderById(orderId);

        assertNotNull(actualOrder);
        assertEquals(orderExpected.getOrderId(), actualOrder.getOrderId());
        assertEquals(orderExpected.getStatus(), actualOrder.getStatus());
        assertEquals(orderExpected.getAmount(), actualOrder.getAmount());
        assertEquals(orderExpected.getProductId(), actualOrder.getProductId());
    }

    @DisplayName("Given Invalid OrderId, findOrderById should throw OrderNotFoundException")
    @Test
    void givenInvalidOrderId_findOrderByIdShouldThrowOrderNotFoundException() {
        UUID invalidOrderId = UUID.randomUUID();

        when(orderRepository.findById(invalidOrderId)).thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> orderService.findOrderById(invalidOrderId));

        String expectedMessage = String.format("There is no order with this id %s saved in the database", invalidOrderId);
        assertEquals(expectedMessage, exception.getMessage());
    }


    private OrderRequest getMockOrderRequest() {
        return OrderRequest.builder()
                .productId(PRODUCT_ID)
                .quantity(2)
                .paymentMode(PaymentMode.APPLE_PAY)
                .build();
    }

    private TransactionDetailsDTO getMockTransactionalDetailsDTO() {
        return TransactionDetailsDTO.builder()
                .transactionId(TRANSACTIONAL_ID)
                .paymentDate(Instant.now())
                .paymentMode(PaymentMode.CASH.toString())
                .amount(new BigDecimal(30000))
                .orderId(ORDER_ID)
                .status("ACCEPTED")
                .build();
    }

    private ProductDTO getMockProductDTO() {
        return ProductDTO.builder()
                .productId(PRODUCT_ID)
                .name("iPhone")
                .price(new BigDecimal(3000))
                .quantity(10)
                .build();
    }

    private Order getMockOrder() {
        return Order.builder()
                .orderId(ORDER_ID)
                .status(OrderStatus.PLACED)
                .orderDate(Instant.now())
                .amount(new BigDecimal(30000))
                .quantity(10)
                .productId(PRODUCT_ID)
                .build();
    }
}