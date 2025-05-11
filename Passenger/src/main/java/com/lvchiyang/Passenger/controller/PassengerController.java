package com.lvchiyang.Passenger.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lvchiyang.Passenger.model.Location;
import com.lvchiyang.Passenger.model.Order;
import com.lvchiyang.Passenger.model.Passenger;
import com.lvchiyang.Passenger.service.PassengerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "乘客服务接口", description = "包含乘客登录、订单创建、账户管理等功能")
@RestController
@RequestMapping("/passenger")
@CrossOrigin(origins = "*")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    @Operation(summary = "测试连接", description = "测试连接")
    @ApiResponse(responseCode = "200", description = "测试连接")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("测试连接");
        return passengerService.test();
    }

    @Operation(summary = "乘客登录", description = "乘客账号登录系统")
    @ApiResponse(responseCode = "200", description = "登录成功，返回乘客信息")
    @PostMapping("/{name}/login")
    public ResponseEntity<Passenger> login(@PathVariable String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        Passenger passenger = passengerService.login(name);
        return ResponseEntity.ok(passenger);
    }

    @Operation(summary = "账户充值", description = "为乘客账户充值")
    @ApiResponse(responseCode = "200", description = "充值请求已受理")
    @PostMapping("/{name}/recharge")
    public ResponseEntity<Passenger> recharge(@PathVariable String name, @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("充值金额必须大于0");
        }
        Passenger passenger = passengerService.recharge(amount);
        System.out.println("用户充值" + amount + "元");
        return ResponseEntity.ok(passenger);
    }

    @Operation(summary = "创建订单", description = "创建新的打车订单")
    @ApiResponse(responseCode = "200", description = "订单创建成功")
    @PostMapping("/{name}/creater")
    public ResponseEntity<Order> createOrder(@PathVariable String name, @RequestBody Map<String, Object> request) {
        Map<String, Object> startLocationMap = (Map<String, Object>) request.get("startLocation");
        Map<String, Object> endLocationMap = (Map<String, Object>) request.get("endLocation");

        Location startLocation = new Location(
                ((Number) startLocationMap.get("x")).intValue(),
                ((Number) startLocationMap.get("y")).intValue());

        Location endLocation = new Location(
                ((Number) endLocationMap.get("x")).intValue(),
                ((Number) endLocationMap.get("y")).intValue());

        Order order = passengerService.createOrder(name, startLocation, endLocation);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "接收订单状态更新", description = "接收平台发送的订单状态更新信息")
    @ApiResponse(responseCode = "200", description = "订单状态更新成功")
    @PostMapping("/order/status")
    public ResponseEntity<Void> receiveOrderStatus(
            @Parameter(description = "订单状态信息") @RequestBody Order order) {
        passengerService.handleOrderStatusUpdate(order);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "接收订单取消通知", description = "接收平台发送的订单取消通知")
    @ApiResponse(responseCode = "200", description = "订单取消通知处理成功")
    @PostMapping("/order/cancel")
    public ResponseEntity<Void> receiveOrderCancel(
            @Parameter(description = "要取消的订单ID") @RequestBody String orderId) {
        passengerService.handleOrderCancel(orderId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "接收订单结束后平台发送的账单信息，更新本地账户")
    @ApiResponse(responseCode = "200", description = "账单信息处理成功")
    @PostMapping("/order/complete")
    public ResponseEntity<Void> receiveOrderComplete(
            @Parameter(description = "订单账单信息") @RequestBody Passenger passenger) {
        passengerService.passenger = passenger;
        passengerService.SendPassengerInfo(passenger);
        return ResponseEntity.ok().build();
    }
}