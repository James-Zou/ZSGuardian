/*
 * ZSGuardian - A super checking system
 * Copyright (C) 2024 James Zou
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.unionhole.zsguardian.controller;

import com.unionhole.zsguardian.dto.StatisticsDTO;
import com.unionhole.zsguardian.service.BugService;
import com.unionhole.zsguardian.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private TestCaseService testCaseService;
    
    @Autowired
    private BugService bugService;

    @GetMapping
    public ResponseEntity<StatisticsDTO> getStatistics() {
        StatisticsDTO statistics = new StatisticsDTO();
        
        // 获取测试用例统计
        statistics.setTotalCases(testCaseService.countTotalCases());
        statistics.setRunningCases(testCaseService.countCasesByStatus("IN_PROGRESS"));
        statistics.setCompletedCases(testCaseService.countCasesByStatus("COMPLETED"));
        
        // 获取Bug统计
        statistics.setPendingBugs(bugService.countPendingBugs());
        
        return ResponseEntity.ok(statistics);
    }
} 