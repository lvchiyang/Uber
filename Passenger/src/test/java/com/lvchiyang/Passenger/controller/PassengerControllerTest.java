package com.lvchiyang.Passenger.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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