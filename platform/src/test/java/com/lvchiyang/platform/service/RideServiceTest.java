package com.lvchiyang.platform.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.lvchiyang.platform.model.Driver;
import com.lvchiyang.platform.model.Location;
import com.lvchiyang.platform.model.Order;
import com.lvchiyang.platform.model.Passenger;
import com.lvchiyang.platform.repository.DriverRepository;
import com.lvchiyang.platform.repository.OrderRepository;
import com.lvchiyang.platform.repository.PassengerRepository;

@ExtendWith(MockitoExtension.class)
public class RideServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RideService rideService;

    private Order order;
    private Driver driver;
    private Passenger passenger;

    @BeforeEach
    void setUp() {
        // 设置测试数据
        passenger = new Passenger("测试乘客");
        passenger.setBalance(BigDecimal.valueOf(100.0));

        driver = new Driver("测试司机");
        driver.setStatus("闲逛");
        driver.setCurrentLocation(new Location(1, 1));

        order = new Order();
        order.setPassengerName("测试乘客");
        order.setStartLocation(new Location(0, 0));
        order.setEndLocation(new Location(5, 5));
    }

    @Test
    void testCreateOrder_Success() {
        // 模拟Repository行为
        when(passengerRepository.findByName("测试乘客")).thenReturn(Optional.of(passenger));
        when(driverRepository.findByStatusIn(any())).thenReturn(List.of(driver));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setOrderId("ORDER-001");
            return savedOrder;
        });

        // 调用测试方法
        Order result = rideService.createOrder(order);

        // 验证结果
        assertNotNull(result);
        assertEquals("ORDER-001", result.getOrderId());
        assertEquals("测试司机", result.getDriverName());
        assertNotNull(result.getPrice());
        assertEquals("等待安排司机", result.getStatus());

        // 验证方法调用
        verify(orderRepository).save(any(Order.class));
        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    void testCreateOrder_NoAvailableDrivers() {
        // 模拟没有可用司机的情况
        when(passengerRepository.findByName("测试乘客")).thenReturn(Optional.of(passenger));
        when(driverRepository.findByStatusIn(any())).thenReturn(List.of());

        // 调用测试方法
        Order result = rideService.createOrder(order);

        // 验证结果
        assertNotNull(result);
        assertEquals("无可用司机", result.getStatus());
        assertNull(result.getDriverName());
    }

    @Test
    void testCreateOrder_InsufficientBalance() {
        // 设置乘客余额不足
        passenger.setBalance(BigDecimal.ZERO);

        // 模拟Repository行为
        when(passengerRepository.findByName("测试乘客")).thenReturn(Optional.of(passenger));

        // 调用测试方法
        Order result = rideService.createOrder(order);

        // 验证结果
        assertNotNull(result);
        assertEquals("乘客余额不足", result.getStatus());
    }

    @Test
    void testFindNearestDriver() {
        // 创建多个司机，位置不同
        Driver driver1 = new Driver("司机1");
        driver1.setCurrentLocation(new Location(1, 1));

        Driver driver2 = new Driver("司机2");
        driver2.setCurrentLocation(new Location(0, 1));

        Driver driver3 = new Driver("司机3");
        driver3.setCurrentLocation(new Location(2, 2));

        List<Driver> drivers = Arrays.asList(driver1, driver2, driver3);

        // 起点位置
        Location startLocation = new Location(0, 0);

        // 调用方法找最近的司机
        Driver nearest = rideService.findNearestDriver(drivers, startLocation);

        // 验证结果 - 司机2应该是最近的
        assertEquals("司机2", nearest.getName());
    }

    @Test
    void testUpdateOrderStatus() {
        // 设置测试数据
        Order existingOrder = new Order();
        existingOrder.setOrderId("ORDER-001");
        existingOrder.setPassengerName("测试乘客");
        existingOrder.setDriverName("测试司机");
        existingOrder.setStartLocation(new Location(0, 0));
        existingOrder.setEndLocation(new Location(5, 5));
        existingOrder.setStatus("司机接单途中");

        // 司机已到达起点位置
        Location driverLocation = new Location(0, 0);

        when(orderRepository.findById("ORDER-001")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // 调用测试方法
        Location result = rideService.updateOrderStatus("ORDER-001", driverLocation);

        // 验证结果 - 状态应该变为"司机送客途中"
        assertEquals("司机送客途中", existingOrder.getStatus());
        assertEquals(existingOrder.getEndLocation(), result);

        // 验证方法调用
        verify(orderRepository).findById("ORDER-001");
        verify(orderRepository).save(existingOrder);
    }

    @Test
    void testCancelOrder() {
        // 设置测试数据
        Order existingOrder = new Order();
        existingOrder.setOrderId("ORDER-001");
        existingOrder.setPassengerName("测试乘客");
        existingOrder.setDriverName("测试司机");
        existingOrder.setStatus("司机接单途中");

        Driver orderDriver = new Driver("测试司机");
        orderDriver.setStatus("接单");
        orderDriver.setCurrentOrderId("ORDER-001");

        when(orderRepository.findById("ORDER-001")).thenReturn(Optional.of(existingOrder));
        when(driverRepository.findByName("测试司机")).thenReturn(Optional.of(orderDriver));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // 调用测试方法
        boolean result = rideService.cancelOrder("ORDER-001");

        // 验证结果
        assertTrue(result);
        assertEquals("已取消", existingOrder.getStatus());
        assertEquals("闲逛", orderDriver.getStatus());
        assertNull(orderDriver.getCurrentOrderId());

        // 验证方法调用
        verify(orderRepository).findById("ORDER-001");
        verify(driverRepository).findByName("测试司机");
        verify(orderRepository).save(existingOrder);
        verify(driverRepository).save(orderDriver);
    }
}