package com.lvchiyang.platform.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lvchiyang.platform.model.Driver;
import com.lvchiyang.platform.model.Location;
import com.lvchiyang.platform.repository.DriverRepository;

/**
 * 司机服务
 * 提供所有与司机相关的业务操作
 */
@Service
@Transactional
public class DriverService {
    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideService rideService;

    /**
     * 司机登录（如果不存在则注册）
     * 
     * @param name 司机姓名
     * @return 司机信息
     */
    public Driver loginDriver(Driver driver) {
        String driverName = driver.getName();
        Location driverLocation = driver.getCurrentLocation();
        // 先根据名字查询司机是否存在
        Driver existingDriver = driverRepository.findByName(driverName).orElse(null);

        // 如果司机不存在，创建新司机
        if (existingDriver == null) {
            existingDriver = new Driver();
            existingDriver.setName(driverName);
            existingDriver.setTotalEarnings(BigDecimal.ZERO);
            existingDriver.setCompletedOrders(0);
        }
        existingDriver.setStatus("闲逛");
        existingDriver.setCurrentLocation(driverLocation);
        driverRepository.save(existingDriver);

        return rideService.assignOrderToDriver(existingDriver);
    }

    /**
     * 增加司机收入
     * 
     * @param driverName 司机名称
     * @param amount     增加的金额
     */
    public void addDriverEarnings(String driverName, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("收入金额必须大于0");
        }

        Driver driver = driverRepository.findByName(driverName).orElseThrow();
        driver.setTotalEarnings(driver.getTotalEarnings().add(amount));
        driverRepository.save(driver);
    }
}