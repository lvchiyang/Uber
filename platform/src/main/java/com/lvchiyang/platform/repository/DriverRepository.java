package com.lvchiyang.platform.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lvchiyang.platform.model.Driver;

@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {
    // 查找在线司机（状态为闲逛或接单的司机）
    List<Driver> findByStatusIn(List<String> statuses);

    // 通过名称查找司机
    Optional<Driver> findByName(String name);

    // 查找所有司机并按状态排序
    @Query("SELECT d FROM Driver d ORDER BY d.status")
    List<Driver> findAllOrderByStatus();
}