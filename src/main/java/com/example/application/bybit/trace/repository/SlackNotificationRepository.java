package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.BongBase;
import com.example.application.bybit.trace.entity.SlackNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlackNotificationRepository extends JpaRepository<SlackNotification, Integer> {
}
