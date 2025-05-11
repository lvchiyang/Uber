package com.lvchiyang.Passenger.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lvchiyang.Passenger.model.Location;
import com.lvchiyang.Passenger.model.Order;
import com.lvchiyang.Passenger.model.Passenger;

@Service
public class PassengerService {

    // 修正平台URL，直接访问平台服务
    private String platformUrl = "http://localhost:8080/api/ride";

    private final RestTemplate restTemplate;
    public Passenger passenger = new Passenger();
    public Location driverLocation = new Location(0, 0);
    public Order order = new Order();
    // public String status = ""; // 等待司机接单、司机正在赶来接您、司机已接到您，正在前往目的地、已到达目的地
    public boolean getDriverLocation = false;

    public PassengerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    private SimpMessagingTemplate template;

    public void SendPassengerInfo(Passenger passenger) {
        try {
            System.out.println("发送乘客信息更新: " + passenger);
            template.convertAndSend("/topic/passenger", passenger);
        } catch (Exception e) {
            System.err.println("发送乘客信息失败: " + e.getMessage());
        }
    }

    public void sendOrderStatus(Order order) {
        try {
            System.out.println("发送订单状态更新: " + order.status);
            template.convertAndSend("/topic/order", order);
        } catch (Exception e) {
            System.err.println("发送订单状态失败: " + e.getMessage());
        }
    }

    public void sendDriverLocation(Location location) {
        try {
            System.out.println("发送司机位置更新: " + location);
            template.convertAndSend("/topic/location", location);
        } catch (Exception e) {
            System.err.println("发送司机位置失败: " + e.getMessage());
        }
    }

    /*
     * 测试连接
     */
    public ResponseEntity<String> test() {
        ResponseEntity<String> response = restTemplate.getForEntity(platformUrl + "/passenger/check-connection",
                String.class);
        return ResponseEntity.ok(response.getBody());

    }

    /**
     * 乘客登录
     * 
     * @param name 乘客姓名
     * @return 乘客信息
     */
    public Passenger login(String name) {
        try {

            // 向平台注册
            ResponseEntity<Passenger> response = restTemplate.postForEntity(
                    platformUrl + "/passenger/login",
                    Map.of("name", name),
                    Passenger.class);

            if (response.getBody() != null) {
                passenger = response.getBody();
                // template.convertAndSend("/passenger", passenger);

            }
            return passenger;
        } catch (Exception e) {
            System.err.println("乘客登录失败: " + e.getMessage());
            throw new RuntimeException("登录服务暂时不可用，请稍后再试", e);
        }
    }

    /**
     * 账户充值
     * 
     * @param amount 充值金额
     * @return 更新后的乘客信息
     */
    public Passenger recharge(Double amount) {
        try {
            // 发送充值请求
            ResponseEntity<Passenger> response = restTemplate.postForEntity(
                    platformUrl + "/passenger/" + passenger.getName() + "/recharge",
                    Map.of("amount", amount),
                    Passenger.class);

            if (response.getBody() != null) {
                passenger = response.getBody();
                // template.convertAndSend("/passenger", passenger);
            }
            return passenger;
        } catch (Exception e) {
            System.err.println("充值失败: " + e.getMessage());
            throw new RuntimeException("充值服务暂时不可用，请稍后再试", e);
        }
    }

    /**
     * 创建订单
     * 
     * @param passengerName 乘客名称
     * @param startLocation 起始位置
     * @param endLocation   目的地位置
     * @return 创建的订单
     */
    public Order createOrder(String passengerName, Location startLocation, Location endLocation) {
        try {
            order.setStatus("等待司机接单");
            order.setPassengerName(passengerName);
            order.setStartLocation(startLocation);
            order.setEndLocation(endLocation);
            sendOrderStatus(order);

            System.out.println("发送订单创建请求: " + order);
            System.out.println("请求地址: " + platformUrl + "/passenger/create/order");

            ResponseEntity<Order> response = restTemplate.postForEntity(
                    platformUrl + "/passenger/create/order",
                    order,
                    Order.class);

            if (response.getBody() != null) {
                order = response.getBody();
                System.out.println("收到订单响应: " + order.getOrderId());
                sendOrderStatus(order);
            }
            return order;
        } catch (Exception e) {
            System.err.println("创建订单失败: " + e.getMessage());
            e.printStackTrace();

            // 恢复订单状态，确保UI显示错误
            order.setStatus("订单创建失败");
            sendOrderStatus(order);
            return order;
        }
    }

    /**
     * 处理订单状态更新
     * 
     * @param orderStatus 订单状态信息
     */
    public void handleOrderStatusUpdate(Order order) {
        this.order = order;
        String orderStatus = order.getStatus();

        // 更新乘客当前订单状态，用于驱动前端界面显示
        System.out.println("订单状态更新: " + orderStatus);

        switch (orderStatus) {
            case "司机接单途中" -> {
                System.out.println(order.status);
                getDriverLocation = true;
                sendOrderStatus(order);
            }
            case "司机送客途中" -> {
                System.out.println(order.status);
                sendOrderStatus(order);
            }
            case "订单完成" -> {
                System.out.println(order.status);
                getDriverLocation = false;
                sendOrderStatus(order);
            }
        }
    }

    /**
     * 处理订单取消通知
     * 
     * @param orderId 被取消的订单ID
     */
    public void handleOrderCancel(String orderId) {
        // 检查是否是当前订单
        if (this.order != null && orderId.equals(this.order.getOrderId())) {
            System.out.println("订单已被取消: " + orderId);

            // 重置订单和状态
            getDriverLocation = false;

            // 通知前端
            Order canceledOrder = new Order();
            canceledOrder.setOrderId(orderId);
            canceledOrder.setStatus("已取消");
            sendOrderStatus(canceledOrder);

            // 清空当前订单信息
            this.order = new Order();
        }
    }

    /**
     * 请求司机位置信息
     * 
     */
    @Scheduled(fixedRate = 2000) // 每2秒执行一次
    public void updateDriverLocation() {
        if (getDriverLocation) {
            try {
                // 修复请求格式，使用正确的JSON格式
                Map<String, String> requestBody = Map.of("driverName", order.getDriverName());

                ResponseEntity<Location> response = restTemplate.postForEntity(
                        platformUrl + "/passenger/driver/location",
                        requestBody,
                        Location.class);
                System.out.println("请求司机位置响应: " + response.getBody().getX() + "," + response.getBody().getY());

                if (response.getBody() != null) {
                    driverLocation = response.getBody();
                    sendDriverLocation(driverLocation);
                }
            } catch (Exception e) {
                System.err.println("获取司机位置失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}