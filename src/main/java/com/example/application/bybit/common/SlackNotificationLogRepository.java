package com.example.application.bybit.common;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackNotificationLogRepository extends JpaRepository<SlackNotificationLog, Integer> {
}
