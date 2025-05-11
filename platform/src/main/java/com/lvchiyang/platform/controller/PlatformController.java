package com.lvchiyang.platform.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lvchiyang.platform.model.Driver;
import com.lvchiyang.platform.model.Order;
import com.lvchiyang.platform.model.Passenger;
import com.lvchiyang.platform.repository.DriverRepository;
import com.lvchiyang.platform.repository.OrderRepository;
import com.lvchiyang.platform.repository.PassengerRepository;
import com.lvchiyang.platform.service.RideService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "平台管理API接口", description = "提供平台管理后台需要的所有接口功能")
@RestController
@RequestMapping("/platform")
@CrossOrigin(origins = "*")
public class PlatformController {

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RideService rideService;

    @Operation(summary = "获取在线司机", description = "获取当前在线的司机信息")
    @ApiResponse(responseCode = "200", description = "成功获取在线司机列表")
    @GetMapping("/drivers/online")
    public List<Driver> getOnlineDrivers() {
        return driverRepository.findByStatusIn(List.of("闲逛", "接单"));
    }

    @Operation(summary = "获取活动订单", description = "获取所有正在进行中的订单")
    @ApiResponse(responseCode = "200", description = "成功获取活动订单列表")
    @GetMapping("/orders/active")
    public List<Order> getActiveOrders() {
        return orderRepository.findAll().stream()
                .filter(order -> !("订单完成".equals(order.getStatus()) || "已取消".equals(order.getStatus())))
                .collect(Collectors.toList());
    }

    @Operation(summary = "获取所有乘客", description = "获取平台所有注册乘客信息")
    @ApiResponse(responseCode = "200", description = "成功获取乘客列表")
    @GetMapping("/passengers")
    public List<Passenger> getAllPassengers() {
        return passengerRepository.findAll();
    }

    @Operation(summary = "获取所有司机", description = "获取平台所有注册司机信息")
    @ApiResponse(responseCode = "200", description = "成功获取司机列表")
    @GetMapping("/drivers")
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    @Operation(summary = "获取司机状态统计", description = "获取不同状态的司机数量")
    @ApiResponse(responseCode = "200", description = "成功获取司机状态统计")
    @GetMapping("/drivers/status-count")
    public java.util.Map<String, Long> getDriverStatusCount() {
        List<Driver> drivers = driverRepository.findAll();
        return drivers.stream()
                .collect(Collectors.groupingBy(
                        Driver::getStatus,
                        Collectors.counting()));
    }

    @Operation(summary = "获取所有订单", description = "获取平台所有订单")
    @ApiResponse(responseCode = "200", description = "成功获取订单列表")
    @GetMapping("/orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Operation(summary = "取消订单", description = "取消指定ID的订单")
    @ApiResponse(responseCode = "200", description = "订单取消成功")
    @DeleteMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Boolean> cancelOrder(@PathVariable String orderId) {
        boolean result = rideService.cancelOrder(orderId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "删除订单", description = "从系统中删除指定ID的订单记录")
    @ApiResponse(responseCode = "200", description = "订单删除成功")
    @ApiResponse(responseCode = "404", description = "订单不存在")
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        if (!orderRepository.existsById(orderId)) {
            return ResponseEntity.notFound().build();
        }
        orderRepository.deleteById(orderId);
        return ResponseEntity.ok().build();
    }
}