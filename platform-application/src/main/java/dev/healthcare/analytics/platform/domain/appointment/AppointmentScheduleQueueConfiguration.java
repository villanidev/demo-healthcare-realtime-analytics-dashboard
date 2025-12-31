package dev.healthcare.analytics.platform.domain.appointment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Configuration
public class AppointmentScheduleQueueConfiguration {

    @Bean
    public BlockingQueue<AppointmentApplicationService.ScheduleCommand> appointmentScheduleQueue(
            @Value("${platform.appointment-queue.capacity:5000}") int capacity) {
        return new ArrayBlockingQueue<>(capacity);
    }
}
