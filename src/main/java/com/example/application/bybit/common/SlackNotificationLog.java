package com.example.application.bybit.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlackNotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx", nullable = false)
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "notification_idx", referencedColumnName = "idx")
    public SlackNotification slackNotification;

    private String requestPath;
    private String methodName;

    @Lob
    private String message;

    @Lob
    private String data;

    @CreationTimestamp
    private LocalDateTime createDate;

}
