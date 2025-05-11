package com.lvchiyang.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lvchiyang.platform.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    // 查询活动订单
    List<Order> findByStatusIn(List<String> statuses);

}