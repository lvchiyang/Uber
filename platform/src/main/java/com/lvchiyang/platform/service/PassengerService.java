package com.lvchiyang.platform.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lvchiyang.platform.model.Passenger;
import com.lvchiyang.platform.repository.PassengerRepository;

/**
 * 乘客服务
 * 提供所有与乘客相关的业务操作
 */
@Service
@Transactional
public class PassengerService {

    @Autowired
    private PassengerRepository passengerRepository;

    /**
     * 乘客登录（如果不存在则注册）
     * 
     * @param name 乘客姓名
     * @return 乘客信息
     */
    public Passenger loginPassenger(String name) {
        // 先根据名字查询乘客是否存在
        Passenger passenger = passengerRepository.findByName(name).orElse(null);

        // 如果乘客不存在，创建新乘客
        if (passenger == null) {
            passenger = new Passenger();
            passenger.setName(name);
            passenger.setBalance(BigDecimal.ZERO);
            passenger.setCompletedOrders(0);
            passenger.setTotalSpending(BigDecimal.ZERO);
            passengerRepository.save(passenger);
        }

        return passenger;
    }

    /**
     * 乘客账户充值
     * 
     * @param passengerName 乘客名称
     * @param amount        充值金额
     */
    public Passenger rechargePassenger(String passengerName, BigDecimal amount) {
        Passenger passenger = passengerRepository.findByName(passengerName).orElseThrow();
        passenger.setBalance(passenger.getBalance().add(amount));
        passengerRepository.save(passenger);
        return passenger;
    }

}