package com.company.attendance.controller;

import com.company.attendance.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterService filterService;

    @GetMapping("/years")
    public List<Integer> findAvailableYears() {
        return filterService.findAvailableYears();
    }
}
