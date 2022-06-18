package com.example.application.bybit.common;

import com.example.application.bybit.common.SlackNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackNotificationRepository extends JpaRepository<SlackNotification, Integer> {
}
