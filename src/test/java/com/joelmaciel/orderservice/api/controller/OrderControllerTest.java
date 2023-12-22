package com.joelmaciel.orderservice.api.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.joelmaciel.orderservice.OrderServiceConfig;
import com.joelmaciel.orderservice.api.openfeign.response.TransactionDetailsDTO;
import com.joelmaciel.orderservice.domain.OrderRepository;
import com.joelmaciel.orderservice.domain.entity.Order;
import com.joelmaciel.orderservice.domain.enums.OrderStatus;
import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import com.joelmaciel.orderservice.domain.model.PaymentMode;
import com.joelmaciel.orderservice.domain.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.util.StreamUtils.copyToString;


@SpringBootTest({"server.port=0"})
@EnableConfigurationProperties
@AutoConfigureMockMvc
@ContextConfiguration(classes = {OrderServiceConfig.class})
class OrderControllerTest {

    public static final String PRODUCT_ID = "0c288c86-c1f3-4211-b59f-f1c52a4a5636";
    public static final String ORDER_ID = "2231c9a2-2a3b-4376-aed4-aa777e6af85a";
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MockMvc mockMvc;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration
                    .wireMockConfig()
                    .port(8080))
            .build();

    @BeforeEach
    void setUp() throws IOException {
        getProductDetailsResponse();
        doPayment();
        getPaymentDetails();
        reduceQuantity();
    }


    @DisplayName("GivenAuthorizedUser_WhenPlacingOrder_ThenOrderIsCreatedSuccessfully")
    @Test
    void givenAuthorizedUser_whenPlacingOrder_thenOrderIsCreatedSuccessfully() throws Exception {
        OrderRequest orderRequest = gettMockOrderRequest();
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/placeOrders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Customer")))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(orderRequest))
                ).andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        String responseContent = mvcResult.getResponse().getContentAsString();
        JsonNode rootNode = objectMapper.readTree(responseContent);
        String orderId = rootNode.path("orderId").asText();

        Optional<Order> orderOptional = orderRepository.findById(UUID.fromString(orderId));
        assertTrue(orderOptional.isPresent());

        Order order = orderOptional.get();
        assertEquals(UUID.fromString(orderId), order.getOrderId());
        assertEquals(OrderStatus.PLACED, order.getStatus());
        assertEquals(orderRequest.getQuantity(), order.getQuantity());
    }

    @DisplayName("GivenNotAuthorizedUser_WhenPlacingOrder_ThenThrow403")
    @Test
    void givenNotAuthorizedUser_WhenPlacingOrderWhitWrongAccess_thenThrow403() throws Exception {
        OrderRequest orderRequest = gettMockOrderRequest();
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/orders/placeOrders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Admin")))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(orderRequest))
                ).andExpect(MockMvcResultMatchers.status().isForbidden())
                .andReturn();
    }

    @DisplayName("Given Invalid OrderId, When GetOrder Then Throw Not Found")
    @Test
    void givenOrderIdInvalid_WhenGetOrder_ThenThrowNotFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/orders/" + ORDER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("Admin")))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();
    }


    private String getOrderResponse(Order order) throws IOException {
        TransactionDetailsDTO transactionDetailsDTO = objectMapper.readValue(
                copyToString(
                        OrderControllerTest.class.getClassLoader()
                                .getResourceAsStream("/mock/GetPayment.json"), defaultCharset()
                ), TransactionDetailsDTO.class
        );
        transactionDetailsDTO.setStatus("SUCCESS");

        OrderDTO.ProductDetails productDetails = objectMapper.readValue(
                copyToString(
                        OrderControllerTest.class.getClassLoader()
                                .getResourceAsStream("mock/GetProduct.json"),
                        defaultCharset()), OrderDTO.ProductDetails.class
        );

        OrderDTO orderDTO = OrderDTO.builder()
                .orderId(order.getOrderId())
                .transactionDetails(transactionDetailsDTO)
                .productDetails(productDetails)
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .amount(order.getAmount())
                .build();

        return objectMapper.writeValueAsString(orderDTO);
    }

    private void reduceQuantity() {
        wireMockServer.stubFor(put(urlMatching("/api/products/reduceQuantity/.*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }

    private void getPaymentDetails() throws IOException {
        wireMockServer.stubFor(get(urlMatching("/api/payments/.*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content_type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                                copyToString(
                                        OrderControllerTest.class
                                                .getClassLoader()
                                                .getResourceAsStream("mock/GetPayment.json"),
                                        defaultCharset()
                                )
                        )));
    }

    private void doPayment() {
        wireMockServer.stubFor(post(urlEqualTo("/api/payments"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }

    private void getProductDetailsResponse() throws IOException {
        wireMockServer.stubFor(get("/api/products/" + PRODUCT_ID)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(copyToString(
                                OrderControllerTest.class
                                        .getClassLoader()
                                        .getResourceAsStream("mock/GetProduct.json"),
                                defaultCharset()
                        ))));
    }

    private OrderRequest gettMockOrderRequest() {
        return OrderRequest.builder()
                .productId(UUID.fromString(PRODUCT_ID))
                .quantity(2)
                .paymentMode(PaymentMode.CASH)
                .build();
    }


    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false);
}