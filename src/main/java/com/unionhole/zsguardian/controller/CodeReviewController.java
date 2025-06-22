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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unionhole.zsguardian.entity.CodeReviewRecord;
import com.unionhole.zsguardian.service.CodeReviewRecordService;
import java.util.Map;
import java.util.HashMap;
import org.springframework.ui.Model;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;

@Controller
@Slf4j
public class CodeReviewController {

    @Autowired
    private CodeReviewRecordService codeReviewRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    // API 接口
    @GetMapping("/api/review/{id}")
    @ResponseBody
    public ResponseEntity<?> getReviewDetails(@PathVariable Long id) {
        try {
            CodeReviewRecord record = codeReviewRecordService.getReviewRecord(id);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            
            // 解析 JSON 数据
            String reviewDataJson = record.getReviewData();
            JsonNode rootNode = objectMapper.readTree(reviewDataJson);
            
            // 提取统计信息，添加空指针判断
            int criticalIssues = rootNode.has("criticalIssues") ? rootNode.get("criticalIssues").asInt() : 0;
            int majorIssues = rootNode.has("majorIssues") ? rootNode.get("majorIssues").asInt() : 0;
            int minorIssues = rootNode.has("minorIssues") ? rootNode.get("minorIssues").asInt() : 0;
            
            response.put("totalIssues", criticalIssues + majorIssues + minorIssues);
            response.put("criticalIssues", criticalIssues);
            response.put("totalFiles", rootNode.get("changedFiles").asInt());
            
            // 添加仓库和分支信息
            response.put("repositoryName", record.getRepository());
            response.put("branchName", record.getBranch());
            
            // 提取所有提交记录
            List<Map<String, String>> commits = new ArrayList<>();
            JsonNode commitAnalyses = rootNode.get("commitAnalyses");
            if (commitAnalyses != null && commitAnalyses.isArray()) {
                for (JsonNode commit : commitAnalyses) {
                    Map<String, String> commitInfo = new HashMap<>();
                    commitInfo.put("author", commit.get("author").asText());
                    commitInfo.put("commitId", commit.get("commitId").asText());
                    commitInfo.put("commitTitle", commit.get("commitTitle").asText());
                    commitInfo.put("timestamp", commit.get("timestamp").asText());
                    commits.add(commitInfo);
                }
            }
            response.put("commits", commits);
            
            List<Map<String, Object>> analysisResults = new ArrayList<>();
            JsonNode commitAnalysesNode = rootNode.get("commitAnalyses");
            
            if (commitAnalysesNode != null && commitAnalysesNode.isArray()) {
                for (JsonNode commit : commitAnalysesNode) {
                    JsonNode fileAnalyses = commit.get("fileAnalyses");
                    if (fileAnalyses != null && fileAnalyses.isArray()) {
                        for (JsonNode file : fileAnalyses) {
                            JsonNode issues = file.get("analysis").get("issues");
                            if (issues != null && issues.isArray()) {
                                Map<String, Object> result = new HashMap<>();
                                for (JsonNode issue : issues) {
                                    String type = issue.get("type").asText();
                                    switch (type) {
                                        case "评估级别":
                                            result.put("level", issue.get("level").asText());
                                            break;
                                        case "影响范围":
                                            result.put("impact", issue.get("content").asText());
                                            break;
                                        case "优化建议":
                                            result.put("suggestion", issue.get("content").asText());
                                            break;
                                        case "改造后代码":
                                            result.put("improvedCode", issue.get("content").asText());
                                            break;
                                    }
                                }
                                result.put("filePath", file.get("filePath").asText());
                                analysisResults.add(result);
                            }
                        }
                    }
                }
            }
            
            response.put("reviewData", analysisResults);
            response.put("reviewTime", record.getCreatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取代码审查详情失败: id=" + id, e);
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "获取代码审查详情失败：" + e.getMessage()));
        }
    }

    // 导出报告
    @GetMapping("/api/review/{id}/export")
    public ResponseEntity<?> exportReport(@PathVariable Long id) {
        try {
            byte[] reportData =null;
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportData);
        } catch (Exception e) {
            log.error("导出报告失败: id=" + id, e);
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "导出报告失败：" + e.getMessage()));
        }
    }

    // 保持原有的页面路由
    @GetMapping("/code-review-details/{id}")
    public String showReviewDetails(@PathVariable Long id, Model model) {
        model.addAttribute("reviewId", id);
        return "code-review-details";
    }
} 