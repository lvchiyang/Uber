package com.lvchiyang.platform.controller;

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

import com.lvchiyang.platform.model.Driver;
import com.lvchiyang.platform.model.Location;
import com.lvchiyang.platform.repository.DriverRepository;
import com.lvchiyang.platform.repository.OrderRepository;
import com.lvchiyang.platform.service.DriverService;
import com.lvchiyang.platform.service.RideService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "司机API接口", description = "提供司机端应用需要的所有接口功能")
@RestController
@RequestMapping("/driver")
@CrossOrigin(origins = "*")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private RideService rideService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Operation(summary = "检查连接状态", description = "检查乘客端与平台的连接状态，返回当前服务器时间")
    @ApiResponse(responseCode = "200", description = "连接正常")
    @GetMapping("/check-connection")
    public ResponseEntity<Map<String, String>> checkConnection() {
        // 获取当前服务器时间
        String serverTime = java.time.LocalDateTime.now().toString();
        return ResponseEntity.ok(Map.of("status", "connected", "serverTime", serverTime));
    }

    @Operation(summary = "司机登录", description = "司机登录系统，返回司机信息")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @PostMapping("/login")
    public ResponseEntity<Driver> driverLogin(@RequestBody Driver driver) {
        if (driver.getName() == null || driver.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }

        // 登录或注册司机
        driver = driverService.loginDriver(driver);

        return ResponseEntity.ok(driver);
    }

    @Operation(summary = "更新司机位置", description = "更新司机当前位置信息")
    @ApiResponse(responseCode = "200", description = "位置更新成功")
    @PostMapping("/{driverName}/location")
    public ResponseEntity<Void> updateDriverLocation(
            @Parameter(description = "司机名称") @PathVariable String driverName,
            @Parameter(description = "位置信息") @RequestBody Location location) {
        Driver driver = driverRepository.findByName(driverName).orElseThrow();
        driver.setCurrentLocation(location);
        driverRepository.save(driver);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "司机下线", description = "司机停止接单")
    @ApiResponse(responseCode = "200", description = "司机下线成功")
    @PostMapping("/{driverName}/offline")
    public ResponseEntity<Void> driverOffline(@PathVariable String driverName) {
        Driver driver = driverRepository.findByName(driverName).orElseThrow();
        driver.setStatus("下线");
        driverRepository.save(driver);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "更新订单状态", description = "更新订单状态（接到乘客/到达目的地）")
    @ApiResponse(responseCode = "200", description = "状态更新成功")
    @PostMapping("/orders/{orderId}/status")
    public ResponseEntity<Location> updateOrderStatus(
            @Parameter(description = "订单ID") @PathVariable String orderId,
            @Parameter(description = "新状态") @RequestBody Location location) {
        Location newLocation = rideService.updateOrderStatus(orderId, location);
        return ResponseEntity.ok(newLocation);
    }
}