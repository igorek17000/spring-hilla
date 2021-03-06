package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.BongBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BongBaseRepository extends JpaRepository<BongBase, Integer> {

    Optional<BongBase> findByMinuteBong(Integer minuteBong);

}
