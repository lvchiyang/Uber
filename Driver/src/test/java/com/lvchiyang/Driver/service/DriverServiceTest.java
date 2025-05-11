package com.lvchiyang.Driver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestTemplate;

import com.lvchiyang.Driver.model.Driver;
import com.lvchiyang.Driver.model.Location;
import com.lvchiyang.Driver.model.Order;

@ExtendWith(MockitoExtension.class)
public class DriverServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private DriverService driverService;

    private Driver testDriver;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 初始化DriverService，只传入 RestTemplate
        driverService = new DriverService(restTemplate);

        // 初始化测试数据
        testDriver = new Driver("测试司机");
        testDriver.setStatus("闲逛");
        testDriver.setCurrentLocation(new Location(0, 0));

        testOrder = new Order();
        testOrder.setOrderId("ORDER-001");
        testOrder.setPassengerName("测试乘客");
        testOrder.setDriverName("测试司机");
        testOrder.setStartLocation(new Location(5, 5));
        testOrder.setEndLocation(new Location(10, 10));
        testOrder.setStatus("等待安排司机");
    }

    @Test
    void testUpdateLocation() {
        // 设置当前驱动状态
        driverService.driver = testDriver;

        // 创建新位置
        Location newLocation = new Location(1, 1);

        // 模拟RestTemplate响应
        when(restTemplate.postForEntity(
                contains("/driver/测试司机/location"),
                eq(newLocation),
                eq(Void.class))).thenReturn(ResponseEntity.ok(null));

        // 调用测试方法
        driverService.updateLocation("测试司机", newLocation);

        // 验证结果
        assertEquals(newLocation, testDriver.getCurrentLocation());

        // 验证RestTemplate调用
        verify(restTemplate).postForEntity(
                contains("/driver/测试司机/location"),
                eq(newLocation),
                eq(Void.class));
    }

    // 辅助方法，用于匹配字符串中包含特定子串的参数
    private static String contains(String substring) {
        return argThat(str -> str != null && str.contains(substring));
    }
}