package com.lvchiyang.platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lvchiyang.platform.model.Passenger;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, String> {
    // 通过名称查找乘客
    Optional<Passenger> findByName(String name);
}