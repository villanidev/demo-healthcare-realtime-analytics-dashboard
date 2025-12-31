package dev.healthcare.analytics.platform.domain.appointment;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AppointmentScheduleQueueWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentScheduleQueueWorker.class);

    private final BlockingQueue<AppointmentApplicationService.ScheduleCommand> queue;
    private final AppointmentApplicationService appointmentApplicationService;
    private final boolean queueEnabled;
    private final int workerCount;

    private ExecutorService executorService;

    public AppointmentScheduleQueueWorker(
            BlockingQueue<AppointmentApplicationService.ScheduleCommand> queue,
            AppointmentApplicationService appointmentApplicationService,
            @Value("${platform.appointment-queue.enabled:false}") boolean queueEnabled,
            @Value("${platform.appointment-queue.worker-count:1}") int workerCount) {
        this.queue = queue;
        this.appointmentApplicationService = appointmentApplicationService;
        this.queueEnabled = queueEnabled;
        this.workerCount = workerCount;
    }

    @PostConstruct
    public void start() {
        if (!queueEnabled) {
            LOGGER.info("Appointment schedule queue is disabled; workers will not start");
            return;
        }

        this.executorService = Executors.newFixedThreadPool(workerCount);
        for (int i = 0; i < workerCount; i++) {
            final int workerIndex = i;
            executorService.submit(() -> workerLoop(workerIndex));
        }
        LOGGER.info("Started {} appointment schedule queue worker(s)", workerCount);
    }

    private void workerLoop(int workerIndex) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AppointmentApplicationService.ScheduleCommand command = queue.take();
                try {
                    appointmentApplicationService.schedule(command);
                } catch (Exception ex) {
                    LOGGER.warn("Worker {} failed to process schedule command {}", workerIndex, command, ex);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("Appointment schedule worker {} stopped", workerIndex);
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
