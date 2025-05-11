package com.lvchiyang.Driver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lvchiyang.Driver.model.Driver;
import com.lvchiyang.Driver.model.Location;
import com.lvchiyang.Driver.model.Order;

@Service
public class DriverService {
    private final RestTemplate restTemplate;
    public Driver driver;
    public Location targetLocation;
    public Order order;
    public String currentOrderId;
    private String platformUrl = "http://localhost:8080/api/ride";

    public DriverService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    private SimpMessagingTemplate template;

    public void SendDriverInfo() { // 向前端发送账户信息
        template.convertAndSend("/topic/driver", driver);
    }

    public void SendOrderInfo() { // 平台安排的订单，向前端发送订单信息
        template.convertAndSend("/topic/order", order);
    }

    /*
     * 测试连接
     */
    public ResponseEntity<String> test() {
        ResponseEntity<String> response = restTemplate.getForEntity(platformUrl + "/driver/check-connection",
                String.class);
        return ResponseEntity.ok(response.getBody());

    }

    // 司机登录（包含注册和上线）
    public Driver login(String name) {
        // 创建司机信息
        driver = new Driver(name);
        driver.setStatus("闲逛");
        driver.setCurrentLocation(new Location(0, 0));

        System.out.println("司机 " + name + " 登录系统，位置: (0, 0)");

        try {
            // 向平台发送登录请求
            ResponseEntity<Driver> response = restTemplate.postForEntity(
                    platformUrl + "/driver/login",
                    driver,
                    Driver.class);

            if (response.getBody() != null) {
                driver = response.getBody();
                System.out.println("司机 " + driver.getName() + " 登录成功，状态: " + driver.getStatus());
                SendDriverInfo();
            }

            return driver;
        } catch (Exception e) {
            System.err.println("司机登录失败: " + e.getMessage());
            e.printStackTrace();
            return driver;
        }
    }

    // 更新位置
    public void updateLocation(String driverName, Location location) {
        if (driver == null || !driver.getName().equals(driverName)) {
            return;
        }

        driver.setCurrentLocation(location);

        // 向平台报告新位置
        restTemplate.postForEntity(
                platformUrl + "/driver/" + driverName + "/location",
                location,
                Void.class);
    }

    // 向目标位置移动
    @Scheduled(fixedRate = 2000) // 每2秒执行一次
    public void orderMove() {
        // 如果司机未登录，直接返回
        if (driver == null) {
            return;
        }

        if ("下线".equals(driver.getStatus())) {
            SendDriverInfo();
            return;
        }
        if ("闲逛".equals(driver.getStatus())) {
            randomMove();
            return;
        }
        if ("接单".equals(driver.getStatus())) {
            targetMove();
        }
    }

    // 闲逛模式
    public void randomMove() {
        // 确保司机已登录
        if (driver == null) {
            return;
        }

        Location current = driver.getCurrentLocation();
        // 随机选择一个相邻的位置
        int newX = current.getX() + (int) (Math.random() * 3) - 1; // -1, 0, or 1
        int newY = current.getY() + (int) (Math.random() * 3) - 1; // -1, 0, or 1

        // 确保不超出地图边界
        newX = Math.max(0, Math.min(9, newX));
        newY = Math.max(0, Math.min(9, newY));

        current.setX(newX);
        current.setY(newY);

        // 向平台报告新位置
        updateLocation(driver.getName(), current);
        SendDriverInfo();
    }

    // 接单模式
    public void targetMove() {
        // 确保司机已登录且有目标位置
        if (driver == null || targetLocation == null) {
            return;
        }

        // 计算向目标移动一步
        Location current = driver.getCurrentLocation();

        // 先向x方向移动
        if (current.getX() != targetLocation.getX()) {
            int newX = moveTowards(current.getX(), targetLocation.getX());
            current.setX(newX);
        }

        // 然后向y方向移动
        if (current.getY() != targetLocation.getY()) {
            int newY = moveTowards(current.getY(), targetLocation.getY());
            current.setY(newY);
        }

        System.out.println("当前位置：" + current.getX() + "," + current.getY() + " 目标位置：" + targetLocation.getX()
                + "," + targetLocation.getY());
        driver.setCurrentLocation(current);
        updateLocation(driver.getName(), current);
        SendDriverInfo();

        // 如果到达目标位置
        if (driver.getCurrentLocation().equals(targetLocation)) {
            ResponseEntity<Location> response = restTemplate.postForEntity(
                    platformUrl + "/driver/orders/" + currentOrderId + "/status",
                    driver.getCurrentLocation(),
                    Location.class);

            if (driver.getCurrentLocation().equals(order.getEndLocation())) {
                order.setStatus("订单完成");
                driver.setStatus("闲逛");

                // 发送最后一次订单状态更新
                SendOrderInfo();
                System.out.println("订单已完成，司机状态设置为闲逛");

                // 清除订单数据
                clearOrderData();
            } else {
                order.setStatus("司机送客途中");
                targetLocation = order.getEndLocation();
                SendOrderInfo();
                System.out.println("已接到乘客，前往目的地: " + order.getEndLocation());
            }
        }
    }

    // 计算下一步的位置
    private int moveTowards(int current, int target) {
        if (current < target)
            return current + 1;
        if (current > target)
            return current - 1;
        return current;
    }

    /**
     * 清除订单相关数据
     * 在订单完成或取消后调用
     */
    public void clearOrderData() {
        // 保存最后一次的司机状态
        String driverStatus = driver.getStatus();
        Location driverLocation = driver.getCurrentLocation();

        // 清除订单相关数据
        this.targetLocation = null;
        this.currentOrderId = null;

        // 创建一个空订单对象
        Order emptyOrder = new Order();
        emptyOrder.setStatus("无订单");
        this.order = emptyOrder;

        // 发送更新信息到前端
        SendOrderInfo();
    }
}