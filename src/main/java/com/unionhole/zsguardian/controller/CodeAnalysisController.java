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

import com.unionhole.zsguardian.service.CodeGuardianService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analyze")
public class CodeAnalysisController {

    @Autowired
    private CodeGuardianService codeGuardianService;

    @PostMapping("/code")
    public ResponseEntity<?> analyzeCode(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String code = (String) request.get("code");
            String fileName = (String) request.get("fileName");
            List<Map<String, Object>> testCaseAnalysis = (List<Map<String, Object>>) request.get("testCaseAnalysis");
            
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("代码内容不能为空");
            }

            // 将测试用例分析结果添加到代码中作为注释
            StringBuilder codeWithContext = new StringBuilder();
            if (testCaseAnalysis != null && !testCaseAnalysis.isEmpty()) {
                codeWithContext.append("/* 测试用例分析上下文:\n");
                for (Map<String, Object> analysis : testCaseAnalysis) {
                    codeWithContext.append(" * ").append(analysis.get("content")).append("\n");
                }
                codeWithContext.append(" */\n\n");
            }
            codeWithContext.append(code);

            // 使用现有的 analyzeCode 方法
            Map<String, Object> analysis = codeGuardianService.analyzeCode(codeWithContext.toString(), fileName);
            
            response.put("success", true);
            response.put("analysis", analysis);
            
        } catch (Exception e) {
            log.error("代码分析失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
} 