package com.lvchiyang.Driver.controller;

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
import org.springframework.web.client.RestTemplate;

import com.lvchiyang.Driver.model.Driver;
import com.lvchiyang.Driver.model.Location;
import com.lvchiyang.Driver.model.Order;
import com.lvchiyang.Driver.service.DriverService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "司机服务接口", description = "包含司机登录、订单处理、位置更新等功能")
@RestController
@RequestMapping("/driver")
@CrossOrigin(origins = "*")
public class DriverController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DriverService driverService;

    private String platformUrl = "http://localhost:8080/api/ride";

    @Operation(summary = "测试连接", description = "测试连接")
    @ApiResponse(responseCode = "200", description = "测试连接")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("测试连接");
        ResponseEntity<String> response = restTemplate.getForEntity(platformUrl + "/driver/check-connection",
                String.class);
        return ResponseEntity.ok(response.getBody());
    }

    @Operation(summary = "司机登录", description = "司机登录并开始接单")
    @PostMapping("/login")
    public ResponseEntity<Driver> login(@RequestBody Driver driver) {
        if (driver.getName() == null || driver.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        Driver result = driverService.login(driver.getName());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "接收订单", description = "接收平台分配的订单")
    @ApiResponse(responseCode = "200", description = "订单接收成功")
    @PostMapping("/order/receive")
    public ResponseEntity<Void> receiveOrder(
            @Parameter(description = "订单信息") @RequestBody Order order) {
        System.out.println("接收到新订单: " + order.getOrderId());
        driverService.targetLocation = order.getStartLocation();
        driverService.order = order;
        driverService.currentOrderId = order.getOrderId(); // 设置当前订单ID
        driverService.driver.setStatus("接单");
        System.out.println("司机接单，前往起点: " + order.getStartLocation());
        driverService.SendOrderInfo();
        driverService.SendDriverInfo();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "处理订单取消", description = "处理平台取消的订单")
    @ApiResponse(responseCode = "200", description = "订单取消处理成功")
    @PostMapping("/order/cancel")
    public ResponseEntity<Void> handleOrderCancel(
            @Parameter(description = "要取消的订单ID") @RequestBody String orderId) {
        System.out.println("收到订单取消通知: " + orderId);

        // 检查是否是当前订单
        if (driverService.currentOrderId != null && driverService.currentOrderId.equals(orderId)) {
            // 设置司机状态为闲逛
            if (driverService.driver != null) {
                driverService.driver.setStatus("闲逛");
                driverService.SendDriverInfo();
            }

            // 清除当前订单信息
            driverService.clearOrderData();
            System.out.println("司机已处理订单取消");
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "更新账户信息", description = "接收订单佣金和账户更新信息")
    @ApiResponse(responseCode = "200", description = "账户信息更新成功")
    @PostMapping("/account/update")
    public ResponseEntity<Void> receiveAccountUpdate(
            @Parameter(description = "账户更新信息") @RequestBody Driver driver) {
        driverService.driver = driver;
        driverService.SendDriverInfo();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "司机下线", description = "司机下线，停止接单")
    @PostMapping("/{name}/offline")
    public ResponseEntity<Void> offline(@PathVariable String name) {
        restTemplate.postForEntity(
                platformUrl + "/driver/" + name + "/offline",
                null,
                Void.class);

        if (driverService.driver != null) {
            driverService.driver.setStatus("下线");
            driverService.SendDriverInfo();
        } else {
            Driver tempDriver = new Driver(name);
            tempDriver.setStatus("下线");
            driverService.driver = tempDriver;
            driverService.SendDriverInfo();
        }

        return ResponseEntity.ok().build();
    }
}