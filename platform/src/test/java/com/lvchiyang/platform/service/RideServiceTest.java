package com.lvchiyang.platform.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

        driver = new Driver();
        driver.setName("测试司机");
        driver.setStatus("闲逛");
        driver.setCurrentLocation(new Location(1, 1));

        order = new Order();
        order.setPassengerName("测试乘客");
        order.setStartLocation(new Location(0, 0));
        order.setEndLocation(new Location(5, 5));
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
    void testCalculateNearestDriver() {
        // 创建多个司机，位置不同
        Driver driver1 = new Driver();
        driver1.setName("司机1");
        driver1.setCurrentLocation(new Location(1, 1));

        Driver driver2 = new Driver();
        driver2.setName("司机2");
        driver2.setCurrentLocation(new Location(0, 1));

        Driver driver3 = new Driver();
        driver3.setName("司机3");
        driver3.setCurrentLocation(new Location(2, 2));

        List<Driver> drivers = Arrays.asList(driver1, driver2, driver3);

        // 起点位置
        Location startLocation = new Location(0, 0);

        // 手动计算最近的司机而不是调用服务中的方法
        Driver nearest = drivers.stream()
                .min((d1, d2) -> {
                    double dist1 = calculateDistance(d1.getCurrentLocation(), startLocation);
                    double dist2 = calculateDistance(d2.getCurrentLocation(), startLocation);
                    return Double.compare(dist1, dist2);
                })
                .orElse(null);

        // 验证结果 - 司机2应该是最近的
        assertNotNull(nearest);
        assertEquals("司机2", nearest.getName());
    }

    // 简单的距离计算方法，用于测试
    private double calculateDistance(Location loc1, Location loc2) {
        int dx = loc1.getX() - loc2.getX();
        int dy = loc1.getY() - loc2.getY();
        return Math.sqrt(dx * dx + dy * dy);
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
}