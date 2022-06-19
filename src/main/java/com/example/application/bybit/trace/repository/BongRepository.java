package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.Bong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BongRepository extends JpaRepository<Bong, Integer> {
    List<Bong> findByMinuteBong(Integer minuteBong);

}
