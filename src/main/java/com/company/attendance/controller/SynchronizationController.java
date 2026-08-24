package com.company.attendance.controller;

import com.company.attendance.dto.SynchronizationStatusDTO;
import com.company.attendance.synchronization.SynchronizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/synchronization")
@RequiredArgsConstructor
public class SynchronizationController {

    private final SynchronizationService synchronizationService;

    @PostMapping("/run")
    public ResponseEntity<SynchronizationStatusDTO> synchronize() {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(synchronizationService.startSynchronization());
    }

    @GetMapping("/{id}")
    public SynchronizationStatusDTO getSynchronizationStatus(@PathVariable("id") Long synchronizationId) {
        return synchronizationService.getSynchronizationStatus(synchronizationId);
    }
}

