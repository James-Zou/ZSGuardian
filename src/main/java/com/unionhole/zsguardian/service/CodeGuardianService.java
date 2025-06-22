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
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CodeGuardianService {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private GitLabService gitLabService;

    @Autowired
    private FeiShuService feiShuService;

    @Autowired
    private CodeAnalyzerService codeAnalyzerService;

    private boolean isListening = false;
    private final Queue<Map<String, Object>> analysisResults = new ConcurrentLinkedQueue<>();

    private static final Set<String> ANALYZABLE_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 后端代码
            ".java", ".kt", ".groovy", ".scala",
            // 前端代码
            ".js", ".jsx", ".ts", ".tsx", ".vue",
            // Web相关
            ".html", ".css", ".scss", ".less"
    ));

    public boolean toggleListener(boolean start) {
        isListening = start;
        return isListening;
    }

    public List<Map<String, Object>> getAnalysisResults() {
        return new ArrayList<>(analysisResults);
    }

    private String extractCommitMessage(Map<String, Object> payload) {
        // 实现从 payload 中提取提交信息的逻辑
        return ""; // 临时返回空字符串
    }

    private List<Map<String, String>> extractCodeChanges(Map<String, Object> payload) {
        // 实现从 payload 中提取代码改动的逻辑
        return new ArrayList<>(); // 临时返回空列表
    }

    private Map<String, Object> analyzeCode(List<Map<String, String>> changes) {
        Map<String, Object> analysis = new HashMap<>();
        // 实现代码分析逻辑
        return analysis;
    }




    // 代码变更的数据结构
    @Data
    @AllArgsConstructor
    class CodeChange {
        private CodeChangeType type;
        private String content;
        private int lineNumber;  // 可选，如果需要在评论中标注具体行号
    }

    enum CodeChangeType {
        ADDED, REMOVED, MODIFIED
    }

    public Map<String, Object> analyzeCode(String code, String fileName) {
        Map<String, Object> analysis = new HashMap<>();
        List<Map<String, String>> issues = new ArrayList<>();

        try {
            // 1. 首先进行代码规范检查
            String fileType = getFileType(fileName);
//            List<Map<String, String>> standardIssues = codeAnalyzerService.analyzeCode(code, fileType);
//            issues.addAll(standardIssues);

            // 2. 如果发现严重问题，可以选择是否继续AI分析
//            boolean hasCriticalIssues = standardIssues.stream()
//                .anyMatch(issue -> "CRITICAL".equals(issue.get("level")));

            // 3. 进行AI分析
            try {
                StringBuilder analysisContext = new StringBuilder();
                analysisContext.append("代码内容:\n").append(code);

                // 将已发现的问题添加到上下文
//                if (!standardIssues.isEmpty()) {
//                    analysisContext.append("\n\n已发现的问题:\n");
//                    for (Map<String, String> issue : standardIssues) {
//                        analysisContext.append("- ").append(issue.get("message")).append("\n");
//                    }
//                }

                List<Map<String, String>> aiAnalysis = deepSeekService.analyzeCode(
                        analysisContext.toString()
                );
                issues.addAll(aiAnalysis);
            } catch (Exception e) {
                log.warn("AI分析失败", e);
                Map<String, String> error = new HashMap<>();
                error.put("type", "错误");
                error.put("level", "LOW");
                error.put("message", "AI分析未完成: " + e.getMessage());
                issues.add(error);
            }

            analysis.put("issues", issues);
            analysis.put("fileType", fileType);
            analysis.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("代码分析失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("type", "错误");
            error.put("level", "HIGH");
            error.put("message", "分析过程发生错误: " + e.getMessage());
            issues.add(error);
            analysis.put("issues", issues);
        }

        return analysis;
    }

    private String getFileType(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "unknown";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "unknown";
    }

    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filePath.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }

    private String determineOverallLevel(Map<String, Long> issueCounts) {
        if (issueCounts.getOrDefault("CRITICAL", 0L) > 0) {
            return "CRITICAL - 存在严重问题，需立即修复";
        } else if (issueCounts.getOrDefault("MAJOR", 0L) > 0) {
            return "MAJOR - 存在重要问题，建议尽快修复";
        } else if (issueCounts.getOrDefault("MINOR", 0L) > 0) {
            return "MINOR - 存在次要问题，可在后续迭代改进";
        } else if (issueCounts.getOrDefault("INFO", 0L) > 0) {
            return "INFO - 存在建议性改进";
        } else {
            return "PASS - 未发现问题";
        }
    }

    // 分析代码但不发送消息
    public Map<String, Object> analyzeChanges(Map<String, Object> changes) {
        List<Map<String, Object>> diffs = (List<Map<String, Object>>) changes.get("diffs");
        String commitId = (String) changes.get("commitId");
        String authorName = (String) changes.get("authorName");
        Integer projectId = (Integer) changes.get("projectId");
        String commitTitle = (String) changes.get("title");

        // 如果没有需要分析的内容，直接返回null
        if (diffs == null || diffs.isEmpty()) {
            return null;
        }

        // 收集所有文件的分析结果
        List<Map<String, Object>> allAnalyses = new ArrayList<>();
        Map<String, Long> totalIssueCounts = new HashMap<>();

        // 按文件路径分组收集 diffs
        Map<String, List<Map<String, Object>>> fileChanges = diffs.stream()
                .collect(Collectors.groupingBy(diff -> (String) diff.get("new_path")));

        // 对每个文件进行分析
        for (Map.Entry<String, List<Map<String, Object>>> entry : fileChanges.entrySet()) {
            String filePath = entry.getKey();
            List<Map<String, Object>> fileDiffs = entry.getValue();

            // 只处理前后端相关的代码文件
            String fileExtension = getFileExtension(filePath);
            if (!ANALYZABLE_EXTENSIONS.contains(fileExtension)) {
                continue;
            }

            // 获取完整的文件内容
//            String fullFileContent = gitLabService.getFileContent(projectId, commitId, filePath);
//            if (fullFileContent == null) {
//                log.error("无法获取文件内容: {}", filePath);
//                continue;
//            }

            // 收集此文件的所有代码变更
            List<String> allFileChanges = new ArrayList<>();
            for (Map<String, Object> diff : fileDiffs) {
                String diffContent = (String) diff.get("diff");
                allFileChanges.add(diffContent);
            }

            // 构造分析用的上下文内容
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("\n\n文件所有代码变更：\n");
            for (String fileChange : allFileChanges) {
                contextBuilder.append(fileChange).append("\n");
            }

            // 进行一次性分析
            Map<String, Object> analysis = analyzeCode(contextBuilder.toString(), filePath);

            // 统计各级别问题数量
            Map<String, Long> fileIssueCounts = ((List<Map<String, String>>) analysis.get("issues"))
                    .stream()
                    .filter(issue -> issue.get("level") != null)
                    .collect(Collectors.groupingBy(
                            issue -> issue.get("level"),
                            Collectors.counting()
                    ));

            // 合并到总问题统计中
            fileIssueCounts.forEach((level, count) ->
                    totalIssueCounts.merge(level, count, Long::sum));

            // 保存文件分析结果
            Map<String, Object> fileAnalysis = new HashMap<>();
            fileAnalysis.put("filePath", filePath);
            fileAnalysis.put("analysis", analysis);
            fileAnalysis.put("issueCounts", fileIssueCounts);
            allAnalyses.add(fileAnalysis);
        }

        // 如果没有可分析的文件，返回null
        if (allAnalyses.isEmpty()) {
            return null;
        }

        // 确保至少有 INFO 级别的计数
        if (totalIssueCounts.isEmpty()) {
            totalIssueCounts.put("INFO", 0L);
        }

        // 确定整体评估级别
        String overallLevel = determineOverallLevel(totalIssueCounts);

        // 构造完整的分析结果
        Map<String, Object> commitAnalysis = new HashMap<>();
        commitAnalysis.put("commitId", commitId);
        commitAnalysis.put("commitTitle", commitTitle);
        commitAnalysis.put("author", authorName);
        commitAnalysis.put("fileAnalyses", allAnalyses);
        commitAnalysis.put("totalIssueCounts", totalIssueCounts);
        commitAnalysis.put("overallLevel", overallLevel);
        commitAnalysis.put("totalFiles", allAnalyses.size());
        commitAnalysis.put("timestamp", System.currentTimeMillis());

        return commitAnalysis;
    }

    // 发送汇总消息
    public void sendPushSummary(Map<String, Object> pushSummary) {
        try {
            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("title", "代码审查报告");
            messageContent.put("repository", pushSummary.get("repository"));
            messageContent.put("branch", pushSummary.get("branch"));
            messageContent.put("totalCommits", pushSummary.get("totalCommits"));
            messageContent.put("commitAnalyses", pushSummary.get("commitAnalyses"));

            feiShuService.sendCodeReviewMessage(messageContent);
        } catch (Exception e) {
            log.error("发送飞书消息失败", e);
        }
    }
}