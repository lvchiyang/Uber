package com.lvchiyang.Passenger.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvchiyang.Passenger.model.Location;
import com.lvchiyang.Passenger.model.Order;
import com.lvchiyang.Passenger.model.Passenger;
import com.lvchiyang.Passenger.service.PassengerService;

@WebMvcTest(PassengerController.class)
public class PassengerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PassengerService passengerService;

    private Passenger testPassenger;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testPassenger = new Passenger();
        testPassenger.setName("测试乘客");
        testPassenger.setBalance(BigDecimal.valueOf(100.0));
        testPassenger.setCompletedOrders(0);
        testPassenger.setTotalSpending(BigDecimal.ZERO);

        testOrder = new Order();
        testOrder.setOrderId("ORDER-001");
        testOrder.setPassengerName("测试乘客");
        testOrder.setStartLocation(new Location(0, 0));
        testOrder.setEndLocation(new Location(5, 5));
        testOrder.setStatus("等待司机接单");
    }

    @Test
    void testTest() throws Exception {
        // 模拟服务响应
        when(passengerService.test()).thenReturn(ResponseEntity.ok("连接正常"));

        // 执行请求并验证结果
        mockMvc.perform(get("/passenger/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("连接正常"));
    }

    @Test
    void testLogin() throws Exception {
        // 模拟服务响应
        when(passengerService.login(anyString())).thenReturn(testPassenger);

        // 执行请求并验证结果
        mockMvc.perform(post("/passenger/测试乘客/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试乘客"))
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void testLogin_EmptyName() throws Exception {
        // 验证空名称异常
        mockMvc.perform(post("/passenger/ /login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRecharge() throws Exception {
        // 模拟服务响应
        when(passengerService.recharge(anyDouble())).thenReturn(testPassenger);

        // 准备请求数据
        Map<String, Double> request = new HashMap<>();
        request.put("amount", 50.0);

        // 执行请求并验证结果
        mockMvc.perform(post("/passenger/测试乘客/recharge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试乘客"))
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void testRecharge_InvalidAmount() throws Exception {
        // 准备无效金额的请求数据
        Map<String, Double> request = new HashMap<>();
        request.put("amount", -10.0);

        // 执行请求并验证结果
        mockMvc.perform(post("/passenger/测试乘客/recharge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder() throws Exception {
        // 模拟服务响应
        when(passengerService.createOrder(anyString(), any(Location.class), any(Location.class)))
                .thenReturn(testOrder);

        // 准备请求数据
        Map<String, Object> request = new HashMap<>();
        Map<String, Integer> startLocation = new HashMap<>();
        startLocation.put("x", 0);
        startLocation.put("y", 0);
        Map<String, Integer> endLocation = new HashMap<>();
        endLocation.put("x", 5);
        endLocation.put("y", 5);
        request.put("startLocation", startLocation);
        request.put("endLocation", endLocation);

        // 执行请求并验证结果
        mockMvc.perform(post("/passenger/测试乘客/creater")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.passengerName").value("测试乘客"))
                .andExpect(jsonPath("$.status").value("等待司机接单"));
    }

    @Test
    void testReceiveOrderStatus() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(post("/passenger/order/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testOrder)))
                .andExpect(status().isOk());
    }

    @Test
    void testReceiveOrderComplete() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(post("/passenger/order/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPassenger)))
                .andExpect(status().isOk());
    }

    @Test
    void testReceiveOrderCancel() throws Exception {
        // 执行请求并验证结果
        mockMvc.perform(post("/passenger/order/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("ORDER-001"))
                .andExpect(status().isOk());
    }
}