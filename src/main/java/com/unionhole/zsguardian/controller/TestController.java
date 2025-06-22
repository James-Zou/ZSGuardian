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

import com.unionhole.zsguardian.entity.TestCase;
import com.unionhole.zsguardian.entity.TestResult;
import com.unionhole.zsguardian.service.TestService;
import com.unionhole.zsguardian.service.ChromeDriverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/tests")
@Slf4j
public class TestController {
    
    @Autowired
    private TestService testService;
    
    @Autowired
    private ChromeDriverManager driverManager;
    
    @PostMapping("/save")
    public ResponseEntity<TestCase> saveTestCase(@RequestBody TestCase testCase) {
        return ResponseEntity.ok(testService.saveTestCase(testCase));
    }
    
    @PostMapping("/execute/{id}")
    public ResponseEntity<TestResult> executeTest(@PathVariable Long id) {
        return ResponseEntity.ok(testService.executeTest(id));
    }
    
    @GetMapping("/results")
    public ResponseEntity<List<TestResult>> getTestResults() {
        return ResponseEntity.ok(testService.getAllTestResults());
    }
    
    @GetMapping("/check-environment")
    public ResponseEntity<Map<String, Object>> checkEnvironment() {
        Map<String, Object> result = new HashMap<>();
        
        boolean chromeInstalled = driverManager.isChromeInstalled();
        boolean driverConfigured = driverManager.isDriverConfigured();
        
        result.put("ready", chromeInstalled && driverConfigured);
        result.put("chromeInstalled", chromeInstalled);
        result.put("driverConfigured", driverConfigured);
        result.put("setupInstructions", driverManager.getSetupInstructions());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/driver-config")
    public ResponseEntity<Map<String, String>> getDriverConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("path", driverManager.getConfiguredDriverPath());
        return ResponseEntity.ok(config);
    }

    @PostMapping("/driver-config")
    public ResponseEntity<Void> saveDriverConfig(@RequestBody Map<String, String> config) {
        driverManager.setDriverPath(config.get("path"));
        return ResponseEntity.ok().build();
    }
} 