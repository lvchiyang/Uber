package com.lvchiyang.platform.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lvchiyang.platform.model.Driver;
import com.lvchiyang.platform.model.Location;
import com.lvchiyang.platform.model.Order;
import com.lvchiyang.platform.model.Passenger;
import com.lvchiyang.platform.repository.DriverRepository;
import com.lvchiyang.platform.repository.PassengerRepository;
import com.lvchiyang.platform.service.PassengerService;
import com.lvchiyang.platform.service.RideService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "乘客API接口", description = "提供乘客端应用需要的所有接口功能")
@RestController
@RequestMapping("/passenger")
@CrossOrigin(origins = "*")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideService rideService;

    @Operation(summary = "检查连接状态", description = "检查乘客端与平台的连接状态，返回当前服务器时间")
    @ApiResponse(responseCode = "200", description = "连接正常")
    @GetMapping("/check-connection")
    public ResponseEntity<Map<String, String>> checkConnection() {
        // 获取当前服务器时间
        String serverTime = java.time.LocalDateTime.now().toString();
        return ResponseEntity.ok(Map.of("status", "connected", "serverTime", serverTime));
    }

    @Operation(summary = "乘客登录", description = "乘客登录系统，返回乘客信息")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @PostMapping("/login")
    public ResponseEntity<Passenger> passengerLogin(
            @Parameter(description = "乘客信息") @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        Passenger passenger = passengerService.loginPassenger(name);
        return ResponseEntity.ok(passenger);
    }

    @Operation(summary = "查询乘客账户", description = "获取乘客账户信息")
    @ApiResponse(responseCode = "200", description = "成功获取账户信息")
    @GetMapping("/{passengerName}")
    public Passenger getPassengerAccount(
            @Parameter(description = "乘客名称") @PathVariable String passengerName) {
        return passengerRepository.findByName(passengerName).orElseThrow();
    }

    @Operation(summary = "乘客充值", description = "为乘客账户充值")
    @ApiResponse(responseCode = "200", description = "充值成功")
    @PostMapping("/{passengerName}/recharge")
    public ResponseEntity<Passenger> rechargePassenger(
            @Parameter(description = "乘客名称") @PathVariable String passengerName,
            @Parameter(description = "充值金额") @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        return ResponseEntity.ok(passengerService.rechargePassenger(passengerName, BigDecimal.valueOf(amount)));
    }

    @Operation(summary = "创建订单", description = "创建新的打车订单")
    @ApiResponse(responseCode = "200", description = "订单创建成功")
    @PostMapping("/create/order")
    public Order createOrder(
            @Parameter(description = "订单信息") @RequestBody Order order) {

        return rideService.createOrder(order);
    }

    @Operation(summary = "请求司机位置", description = "请求司机位置信息")
    @ApiResponse(responseCode = "200", description = "请求成功")
    @PostMapping("/driver/location")
    public Location requestDriverLocation(@RequestBody Map<String, String> request) {
        String driverName = request.get("driverName");
        Driver driver = driverRepository.findByName(driverName).orElseThrow();
        return driver.getCurrentLocation();
    }

}