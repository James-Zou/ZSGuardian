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
package com.unionhole.zsguardian.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unionhole.zsguardian.entity.CodeReviewRecord;
import com.unionhole.zsguardian.repository.CodeReviewRepository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;

@Service
@Slf4j
public class CodeReviewRecordService {
    
    @Autowired
    private CodeReviewRepository codeReviewRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    public CodeReviewRecord saveReviewRecord(Map<String, Object> content) {
        try {
            if (content == null) {
                throw new IllegalArgumentException("审查内容不能为空");
            }

            CodeReviewRecord record = new CodeReviewRecord();
            
            // 安全地获取值并设置默认值
            record.setRepository(Optional.ofNullable(content.get("repository"))
                .map(Object::toString)
                .orElse("unknown"));
                
            record.setBranch(Optional.ofNullable(content.get("branch"))
                .map(Object::toString)
                .orElse("unknown"));
                
            record.setTotalCommits(Optional.ofNullable(content.get("totalCommits"))
                .map(val -> {
                    if (val instanceof Integer) {
                        return (Integer) val;
                    }
                    return Integer.parseInt(val.toString());
                })
                .orElse(0));

            record.setReviewData(objectMapper.writeValueAsString(content));

            log.info("准备保存的记录: {}", record);
            return codeReviewRepository.save(record);
            
        } catch (Exception e) {
            log.error("保存代码审查记录失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存代码审查记录失败: " + e.getMessage(), e);
        }
    }

    public CodeReviewRecord getReviewRecord(Long id) {
        return codeReviewRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("未找到指定的代码审查记录"));
    }

    public List<CodeReviewRecord> getReviewRecords(LocalDateTime startDate, 
            LocalDateTime endDate, int page, int pageSize) {
        try {
            PageRequest pageRequest = PageRequest.of(
                page - 1, 
                pageSize, 
                Sort.by(Sort.Direction.DESC, "createdAt")
            );

            return codeReviewRepository.findReviewRecords(
                startDate,
                endDate,
                pageRequest
            );
        } catch (Exception e) {
            log.error("Error fetching review records", e);
            return new ArrayList<>();
        }
    }

    public long countReviewRecords(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return codeReviewRepository.countReviewRecords(
                startDate,
                endDate
            );
        } catch (Exception e) {
            log.error("Error counting review records", e);
            return 0L;
        }
    }

    public Map<String, Object> getDataScreenStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        List<CodeReviewRecord> allRecords = codeReviewRepository.findAll();
        statistics.put("totalReviews", allRecords.size());
        
        Map<String, Integer> issueTypesCount = new HashMap<>();
        Map<String, Integer> severityCount = new HashMap<>();
        Map<String, Integer> repoCount = new HashMap<>();
        Map<LocalDate, Integer> dailyCount = new TreeMap<>();
        int totalIssues = 0;
        
        ObjectMapper mapper = new ObjectMapper();
        
        for (CodeReviewRecord record : allRecords) {
            try {
                if (record.getReviewData() != null) {
                    JsonNode root = mapper.readTree(record.getReviewData());
                    JsonNode commitAnalyses = root.get("commitAnalyses");
                    
                    if (commitAnalyses != null && commitAnalyses.isArray()) {
                        for (JsonNode commit : commitAnalyses) {
                            JsonNode fileAnalyses = commit.get("fileAnalyses");
                            if (fileAnalyses != null && fileAnalyses.isArray()) {
                                for (JsonNode file : fileAnalyses) {
                                    JsonNode analysis = file.get("analysis");
                                    if (analysis != null) {
                                        JsonNode issues = analysis.get("issues");
                                        if (issues != null && issues.isArray()) {
                                            // 统计问题
                                            for (JsonNode issue : issues) {
                                                String type = issue.get("type") != null ? 
                                                    issue.get("type").asText() : "Unknown";
                                                String level = issue.get("level") != null ? 
                                                    issue.get("level").asText() : null;
                                                
                                                if (!type.equals("改造后代码") && !type.equals("影响范围") && !type.equals("优化建议")) {
                                                    issueTypesCount.merge(type, 1, Integer::sum);
                                                    totalIssues++;
                                                }
                                                
                                                if (level != null) {
                                                    severityCount.merge(level, 1, Integer::sum);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 仓库统计
                        String repository = root.get("repository") != null ? 
                            root.get("repository").asText() : "Unknown";
                        String repoName = extractRepoName(repository);
                        repoCount.merge(repoName, 1, Integer::sum);
                        
                        // 日期统计
                        LocalDate date = record.getCreatedAt().toLocalDate();
                        dailyCount.merge(date, 1, Integer::sum);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 设置统计结果
        statistics.put("totalIssues", totalIssues);
        statistics.put("avgIssuesPerReview", String.format("%.2f", 
            allRecords.isEmpty() ? 0 : (double) totalIssues / allRecords.size()));
        
        // 问题类型分布
        statistics.put("issueTypes", convertToChartData(issueTypesCount));
        
        // 每日趋势
        Map<String, Object> dailyTrend = new HashMap<>();
        dailyTrend.put("dates", new ArrayList<>(dailyCount.keySet().stream()
            .map(date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .collect(Collectors.toList())));
        dailyTrend.put("counts", new ArrayList<>(dailyCount.values()));
        statistics.put("dailyTrend", dailyTrend);
        
        // 仓库分布
        statistics.put("repoDistribution", convertToChartData(repoCount));
        
        // 严重程度分布
        Map<String, Object> severityData = new HashMap<>();
        severityData.put("levels", new ArrayList<>(severityCount.keySet()));
        severityData.put("counts", new ArrayList<>(severityCount.values()));
        statistics.put("severity", severityData);
        
        // 计算近期趋势
        statistics.put("recentTrend", calculateRecentTrend(dailyCount));
        
        return statistics;
    }
    
    private String extractRepoName(String repository) {
        if (repository == null || repository.isEmpty()) {
            return "Unknown";
        }
        // 从URL中提取仓库名
        String[] parts = repository.split("/");
        return parts[parts.length - 1].replace(".git", "");
    }
    
    private List<Map<String, Object>> convertToChartData(Map<String, Integer> data) {
        return data.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("value", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }
    
    private double calculateRecentTrend(Map<LocalDate, Integer> dailyCount) {
        if (dailyCount.isEmpty()) {
            return 0.0;
        }
        
        LocalDate today = LocalDate.now();
        int currentWeek = 0;
        int lastWeek = 0;
        
        // 计算最近两周的趋势
        for (Map.Entry<LocalDate, Integer> entry : dailyCount.entrySet()) {
            LocalDate date = entry.getKey();
            int count = entry.getValue();
            
            if (date.isAfter(today.minusDays(7))) {
                currentWeek += count;
            } else if (date.isAfter(today.minusDays(14))) {
                lastWeek += count;
            }
        }
        
        // 计算环比增长率
        if (lastWeek == 0) {
            return currentWeek > 0 ? 100.0 : 0.0;
        }
        
        return ((double)(currentWeek - lastWeek) / lastWeek) * 100;
    }

} 