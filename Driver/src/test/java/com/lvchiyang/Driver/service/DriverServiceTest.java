package com.lvchiyang.Driver.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
        // 初始化DriverService
        driverService = new DriverService(restTemplate, messagingTemplate);

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
    void testLogin() {
        // 模拟RestTemplate响应
        ResponseEntity<Driver> response = ResponseEntity.ok(testDriver);
        when(restTemplate.postForEntity(contains("/driver/login"), any(Driver.class), eq(Driver.class)))
                .thenReturn(response);

        // 调用测试方法
        Driver result = driverService.login("测试司机");

        // 验证结果
        assertNotNull(result);
        assertEquals("测试司机", result.getName());
        assertEquals("闲逛", result.getStatus());

        // 验证消息通知
        verify(messagingTemplate).convertAndSend("/topic/driver", testDriver);
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

    @Test
    void testRandomMove() {
        // 设置当前驱动状态
        driverService.driver = testDriver;

        // 模拟RestTemplate响应
        when(restTemplate.postForEntity(
                contains("/driver/测试司机/location"),
                any(Location.class),
                eq(Void.class))).thenReturn(ResponseEntity.ok(null));

        // 调用测试方法
        driverService.randomMove();

        // 验证结果 - 位置应该在地图范围内
        Location newLocation = testDriver.getCurrentLocation();
        assertTrue(newLocation.getX() >= 0 && newLocation.getX() <= 9);
        assertTrue(newLocation.getY() >= 0 && newLocation.getY() <= 9);

        // 验证更新位置方法被调用
        verify(restTemplate).postForEntity(
                contains("/driver/测试司机/location"),
                any(Location.class),
                eq(Void.class));

        // 验证消息通知
        verify(messagingTemplate).convertAndSend("/topic/driver", testDriver);
    }

    @Test
    void testTargetMove() {
        // 设置当前驱动状态和目标位置
        driverService.driver = testDriver;
        driverService.targetLocation = new Location(1, 0);
        driverService.currentOrderId = "ORDER-001";
        driverService.order = testOrder;

        // 模拟位置更新响应
        when(restTemplate.postForEntity(
                contains("/driver/测试司机/location"),
                any(Location.class),
                eq(Void.class))).thenReturn(ResponseEntity.ok(null));

        // 模拟订单状态更新响应
        when(restTemplate.postForEntity(
                contains("/driver/orders/ORDER-001/status"),
                any(Location.class),
                eq(Location.class))).thenReturn(ResponseEntity.ok(new Location(1, 0)));

        // 调用测试方法
        driverService.targetMove();

        // 验证结果 - 位置应该向目标移动一步
        assertEquals(1, testDriver.getCurrentLocation().getX());
        assertEquals(0, testDriver.getCurrentLocation().getY());

        // 验证更新位置方法被调用
        verify(restTemplate).postForEntity(
                contains("/driver/测试司机/location"),
                any(Location.class),
                eq(Void.class));

        // 验证到达目标位置后的行为
        verify(restTemplate).postForEntity(
                contains("/driver/orders/"),
                any(Location.class),
                eq(Location.class));

        // 验证消息通知
        verify(messagingTemplate).convertAndSend("/topic/driver", testDriver);
    }

    @Test
    void testClearOrderData() {
        // 设置当前状态
        driverService.driver = testDriver;
        driverService.targetLocation = new Location(5, 5);
        driverService.currentOrderId = "ORDER-001";
        driverService.order = testOrder;

        // 调用测试方法
        driverService.clearOrderData();

        // 验证结果
        assertNull(driverService.targetLocation);
        assertNull(driverService.currentOrderId);
        assertNotNull(driverService.order);
        assertEquals("无订单", driverService.order.getStatus());

        // 验证消息通知
        verify(messagingTemplate).convertAndSend("/topic/order", driverService.order);
    }

    // 辅助方法，用于匹配字符串中包含特定子串的参数
    private static String contains(String substring) {
        return argThat(str -> str != null && str.contains(substring));
    }

    // 辅助方法，匹配特定类型的参数
    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}