package com.company.attendance.synchronization;

import com.company.attendance.dto.SynchronizationStatusDTO;

public interface SynchronizationService {

    SynchronizationStatusDTO startSynchronization();

    SynchronizationStatusDTO getSynchronizationStatus(Long synchronizationId);
}
