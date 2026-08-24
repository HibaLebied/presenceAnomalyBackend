package com.company.attendance.synchronization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SynchronizationStartupRecovery {

    private final SynchronizationHistoryService synchronizationHistoryService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverRunningSynchronizations() {
        log.info("Recovering synchronization jobs left in RUNNING status before startup");
        synchronizationHistoryService.recoverRunningSynchronizations(LocalDateTime.now());
    }
}
