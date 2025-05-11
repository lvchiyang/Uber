package com.lvchiyang.platform.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.lvchiyang.platform.model.Driver;
import com.lvchiyang.platform.model.Location;
import com.lvchiyang.platform.model.Order;
import com.lvchiyang.platform.model.Passenger;
import com.lvchiyang.platform.repository.DriverRepository;
import com.lvchiyang.platform.repository.OrderRepository;
import com.lvchiyang.platform.repository.PassengerRepository;

@Service
@Transactional
public class RideService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    private String DriverServiceUrl = "http://localhost:8080/driver/";
    private String PassengerServiceUrl = "http://localhost:8080/passenger/";

    /**
     * 创建新订单
     * 
     * @param passengerId   乘客ID
     * @param startLocation 起始位置
     * @param endLocation   目的地位置
     * @return 创建成功的订单对象
     */
    public Order createOrder(Order order) {
        Passenger passenger = passengerRepository.findByName(order.getPassengerName())
                .orElseThrow(() -> new RuntimeException("乘客不存在"));

        if (passenger == null) {
            order.setStatus("乘客未登录");
            return order;
        }

        // 计算订单价格
        BigDecimal price = calculatePrice(order.getStartLocation(), order.getEndLocation());

        if (passenger.getBalance().compareTo(price) < 0) {
            order.setStatus("乘客余额不足");
            return order;
        }

        order.setPrice(price);

        // 生成6位数字订单号
        int orderNumber = 100000 + (int) (Math.random() * 900000); // 生成100000-999999之间的随机数
        order.setOrderId(String.valueOf(orderNumber));

        order.setStatus("等待安排司机");
        order.setCreateTime(LocalDateTime.now());
        order.setPassengerName(passenger.getName());

        // 设置临时司机名称，避免数据库非空约束错误
        order.setDriverName("未分配");
        System.out.println("订单 " + order.getOrderId());
        System.out.println("乘客 " + order.getPassengerName());
        System.out.println("状态 " + order.getStatus());

        // 保存订单到数据库
        orderRepository.save(order);

        // 异步分配司机
        assignDriverAsync(order);

        return order;
    }

    /**
     * 异步分配司机
     * 
     * @param order 要分配司机的订单
     */
    @Async
    public void assignDriverAsync(Order order) {
        assignNearestDriver(order);
    }

    /**
     * 为订单分配最近的空闲司机
     * 
     * @param order 要分配司机的订单
     * @return 是否成功分配
     */
    private Order assignNearestDriver(Order order) {
        // 获取所有在线空闲且距离最近的司机
        List<Driver> availableDrivers = driverRepository.findByStatusIn(List.of("闲逛")).stream()
                .filter(driver -> driver.getCurrentLocation() != null)
                .toList();

        if (availableDrivers.isEmpty()) {
            System.out.println("没有可用的司机，订单将保持未分配状态");
            return order;
        }

        // 计算并选择距离起点最近的司机
        Driver nearestDriver = availableDrivers.stream()
                .min(Comparator.comparingInt(driver -> calculateDistance(
                        driver.getCurrentLocation(), order.getStartLocation())))
                .orElse(null);

        if (nearestDriver == null) {
            System.out.println("没有找到合适的司机，订单将保持未分配状态");
            return order;
        }

        // 分配订单给司机
        order.setDriverName(nearestDriver.getName());
        order.setStatus("司机接单途中");
        orderRepository.save(order);

        try {
            // 通知乘客和司机服务
            restTemplate.postForEntity(PassengerServiceUrl + "/order/status", order, Void.class);
            restTemplate.postForEntity(DriverServiceUrl + "/order/receive", order, Void.class);
            System.out.println("订单 " + order.getOrderId() + " 已分配给司机 " + nearestDriver.getName());
        } catch (Exception e) {
            System.err.println("通知服务失败: " + e.getMessage());
            // 异常不影响订单分配结果
        }

        return order;
    }

    /**
     * 尝试为司机分配未完成的订单
     * 
     * @param driverName 司机名称
     * @return 分配的订单，如果没有可分配的订单则返回null
     */
    public Driver assignOrderToDriver(Driver driver) {

        if (!driver.getStatus().equals("闲逛")) {
            throw new IllegalStateException("司机必须在线才能分配订单");
        }

        // 获取第一个未分配的订单 - 修改筛选条件以包含"未分配"司机的订单
        List<Order> unassignedOrders = orderRepository.findAll().stream()
                .filter(order -> order.getDriverName() == null || "未分配".equals(order.getDriverName()))
                .filter(order -> "等待安排司机".equals(order.getStatus()))
                .toList();

        // 添加诊断日志
        System.out.println("检索到 " + unassignedOrders.size() + " 个待分配的订单");

        if (unassignedOrders.isEmpty()) {
            System.out.println("没有找到待分配的订单，司机 " + driver.getName() + " 将继续闲逛");
            return driver;
        }

        // 选择距离司机最近的订单
        Order nearestOrder = unassignedOrders.stream()
                .min(Comparator.comparingInt(
                        order -> calculateDistance(driver.getCurrentLocation(), order.getStartLocation())))
                .orElse(null);

        // 分配订单给司机
        nearestOrder.setDriverName(driver.getName());
        nearestOrder.setStatus("司机接单途中");
        driver.setStatus("接单");
        driverRepository.save(driver);
        orderRepository.save(nearestOrder);
        restTemplate.postForEntity(PassengerServiceUrl + "/order/status", nearestOrder, Void.class);

        restTemplate.postForEntity(DriverServiceUrl + "/order/receive", nearestOrder, Void.class);

        System.out.println("订单 " + nearestOrder.getOrderId() + " 已分配给上线司机 " + driver.getName());
        return driver;

    }

    public Location updateOrderStatus(String orderId, Location DriverLocation) {

        // 获取订单
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("订单未找到"));

        // 检查司机位置是否等于起始位置
        if (DriverLocation.equals(order.getStartLocation())) {
            order.setStatus("司机送客途中");
            orderRepository.save(order);
            restTemplate.postForEntity(PassengerServiceUrl + "/order/status", order, Void.class);
            return order.getEndLocation();
        }

        // 检查司机位置是否等于结束位置
        if (DriverLocation.equals(order.getEndLocation())) {
            order.setStatus("订单完成");
            restTemplate.postForEntity(PassengerServiceUrl + "/order/status", order, Void.class);
            completeOrder(order);
        }
        return null;

    }

    /**
     * 完成订单
     * 司机到达目的地后完成订单，更新订单状态，结算费用
     * 
     * @param orderId 订单ID
     * @return 更新后的订单对象
     */
    @Transactional
    public void completeOrder(Order order) {
        // 更新订单状态
        order.setStatus("订单完成");
        order.setCompleteTime(LocalDateTime.now());

        // 更新司机信息
        Driver driver = driverRepository.findByName(order.getDriverName())
                .orElseThrow(() -> new RuntimeException("司机不存在"));
        driver.setStatus("闲逛");
        driver.setCompletedOrders(driver.getCompletedOrders() + 1);
        driver.setTotalEarnings(driver.getTotalEarnings().add(order.getPrice()));
        driverRepository.save(driver);

        restTemplate.postForEntity(DriverServiceUrl + "/account/update", driver, Void.class);

        // 更新乘客信息
        Passenger passenger = passengerRepository.findByName(order.getPassengerName())
                .orElseThrow(() -> new RuntimeException("乘客不存在"));
        passenger.setCompletedOrders(passenger.getCompletedOrders() + 1);
        passenger.setTotalSpending(passenger.getTotalSpending().add(order.getPrice()));
        passenger.setBalance(passenger.getBalance().subtract(order.getPrice()));
        passengerRepository.save(passenger);

        restTemplate.postForEntity(PassengerServiceUrl + "/order/complete", passenger, Void.class);

        assignOrderToDriver(driver);
    }

    /**
     * 计算两个位置之间的曼哈顿距离
     */
    private int calculateDistance(Location loc1, Location loc2) {
        return Math.abs(loc1.getX() - loc2.getX()) + Math.abs(loc1.getY() - loc2.getY());
    }

    /**
     * 计算订单价格
     * 根据起始点和终点位置计算订单价格
     * 
     * @param start 起始位置
     * @param end   终点位置
     * @return 计算得出的订单价格
     */
    public BigDecimal calculatePrice(Location start, Location end) {
        int distance = Math.abs(end.getX() - start.getX()) +
                Math.abs(end.getY() - start.getY());
        System.out.println("订单金额：" + distance);
        return new BigDecimal(distance);
    }

    /**
     * 取消订单
     * 
     * @param orderId 要取消的订单ID
     * @return 是否成功取消
     */
    public boolean cancelOrder(String orderId) {
        try {
            // 获取订单
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));

            // 检查订单状态，只有未完成的订单才能取消
            if ("订单完成".equals(order.getStatus())) {
                return false;
            }

            // 删除订单
            orderRepository.deleteById(orderId);

            // 如果订单已分配司机，通知司机
            if (order.getDriverName() != null && !"未分配".equals(order.getDriverName())) {
                try {
                    // 获取司机信息
                    Driver driver = driverRepository.findByName(order.getDriverName())
                            .orElse(null);

                    if (driver != null) {
                        // 将司机状态改为闲逛
                        driver.setStatus("闲逛");
                        driverRepository.save(driver);

                        // 通知司机服务
                        restTemplate.postForEntity(DriverServiceUrl + "/order/cancel", orderId, Void.class);
                    }
                } catch (Exception e) {
                    System.err.println("通知司机取消订单失败: " + e.getMessage());
                }
            }

            // 通知乘客
            try {
                restTemplate.postForEntity(PassengerServiceUrl + "/order/cancel", orderId, Void.class);
            } catch (Exception e) {
                System.err.println("通知乘客取消订单失败: " + e.getMessage());
            }

            System.out.println("订单 " + orderId + " 已取消");
            return true;
        } catch (Exception e) {
            System.err.println("取消订单失败: " + e.getMessage());
            return false;
        }
    }

}