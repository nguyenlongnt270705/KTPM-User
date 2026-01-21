package com.example.demo.dto;

import com.example.demo.enums.NotificationEnum;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationPayload<T> {

    NotificationEnum type;
    String sessionId;
    String username;
    String title;
    String message;
    Instant timestamp;
    T data;
}
