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

import com.unionhole.zsguardian.entity.CodeReviewRecord;
import com.unionhole.zsguardian.service.CodeReviewRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/audit")
@Slf4j
public class CodeAuditController {

    @Autowired
    private CodeReviewRecordService codeReviewRecordService;

    @PostMapping("/list")
    public ResponseEntity<?> getAuditList(@RequestBody Map<String, Object> params) {
        try {
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            
            if (params.get("startDate") != null) {
                String startDateStr = (String) params.get("startDate");
                startDate = Instant.parse(startDateStr)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            }
            
            if (params.get("endDate") != null) {
                String endDateStr = (String) params.get("endDate");
                endDate = Instant.parse(endDateStr)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            }

            int page = params.get("page") != null ? (int) params.get("page") : 1;
            int pageSize = params.get("pageSize") != null ? (int) params.get("pageSize") : 10;

            List<CodeReviewRecord> records = codeReviewRecordService.getReviewRecords(
                startDate, endDate, page, pageSize);
            long total = codeReviewRecordService.countReviewRecords(startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("data", records);
            response.put("total", total);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取审核记录列表失败", e);
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "获取审核记录列表失败：" + e.getMessage()));
        }
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getAuditDetail(@PathVariable Long id) {
        try {
            CodeReviewRecord record = codeReviewRecordService.getReviewRecord(id);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("id", record.getId());
            detail.put("auditTime", record.getCreatedAt());

            // 解析 reviewData
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(record.getReviewData());
            
            int criticalIssues = rootNode.has("criticalIssues") ? rootNode.get("criticalIssues").asInt() : 0;
            int majorIssues = rootNode.has("majorIssues") ? rootNode.get("majorIssues").asInt() : 0;
            int minorIssues = rootNode.has("minorIssues") ? rootNode.get("minorIssues").asInt() : 0;
            int totalIssues = criticalIssues + majorIssues + minorIssues;
            
            detail.put("issueCount", totalIssues);
            detail.put("status", totalIssues > 0 ? "error" : "success");

            // 提取问题详情
            List<Map<String, Object>> issues = new ArrayList<>();
            JsonNode commitAnalyses = rootNode.get("commitAnalyses");
            if (commitAnalyses != null && commitAnalyses.isArray()) {
                for (JsonNode commit : commitAnalyses) {
                    JsonNode fileAnalyses = commit.get("fileAnalyses");
                    if (fileAnalyses != null && fileAnalyses.isArray()) {
                        for (JsonNode file : fileAnalyses) {
                            JsonNode analysisIssues = file.get("analysis").get("issues");
                            if (analysisIssues != null && analysisIssues.isArray()) {
                                for (JsonNode issue : analysisIssues) {
                                    Map<String, Object> issueMap = new HashMap<>();
                                    String type = issue.get("type").asText();
                                    issueMap.put("type", type);
                                    issueMap.put("title", type);
                                    issueMap.put("description", issue.get("content").asText());
                                    if (type.equals("改造后代码")) {
                                        issueMap.put("code", issue.get("content").asText());
                                    }
                                    issues.add(issueMap);
                                }
                            }
                        }
                    }
                }
            }
            
            detail.put("issues", issues);
            
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("获取审核记录详情失败: id=" + id, e);
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "获取审核记录详情失败：" + e.getMessage()));
        }
    }
} 