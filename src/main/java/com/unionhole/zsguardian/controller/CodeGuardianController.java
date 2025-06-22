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
import com.unionhole.zsguardian.service.GitLabService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/codeguardian")
public class CodeGuardianController {


    @Autowired
    private CodeGuardianService codeGuardianService;

    @Value("${gitlab.webhook.token}")
    private String apiToken;



    @Autowired
    private GitLabService gitLabService;

    @PostMapping
    public ResponseEntity<?> handleGitlabWebhook(
            @RequestHeader("X-Gitlab-Event") String eventType,
            @RequestHeader("X-Gitlab-Token") String token,
            @RequestBody Map<String, Object> payload) {
        try {
            // 验证 webhook token
            if (!isValidToken(token)) {
                return ResponseEntity.status(401).body("Invalid token");
            }

            //只处理 push 事件
            if (!"Push Hook".equals(eventType)) {
                return ResponseEntity.ok().build();
            }

            // 提取提交信息
            List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
            Integer projectId = (Integer) payload.get("project_id");
            if (commits != null && !commits.isEmpty()) {
                // 收集所有commit的分析结果
                List<Map<String, Object>> allCommitAnalyses = new ArrayList<>();

                for (Map<String, Object> commit : commits) {
                    // 获取修改的文件列表
                    List<String> added = (List<String>) commit.get("added");
                    List<String> modified = (List<String>) commit.get("modified");
                    List<String> removed = (List<String>) commit.get("removed");
                    // 构建变更信息
                    Map<String, Object> changes = new HashMap<>();
                    String commitId = (String) commit.get("id");
                    changes.put("commitId", commitId);
                    changes.put("added", added);
                    changes.put("modified", modified);
                    changes.put("removed", removed);
                    changes.put("repository", ((Map<String, Object>) payload.get("repository")).get("git_http_url"));
                    changes.put("projectId", projectId);
                    changes.put("title", commit.get("title"));
                    Map<String,String> authorMap = (Map<String, String>) commit.get("author");
                    changes.put("authorName", authorMap.get("name"));
                    changes.put("authorEmail", authorMap.get("email"));
                    // 获取diff信息并添加到changes中
                    List<Map<String, Object>> diffs = gitLabService.getDiffInformation(projectId, commitId);
                    changes.put("diffs", diffs);

                    // 获取分析结果但不发送消息
                    Map<String, Object> analysis = codeGuardianService.analyzeChanges(changes);
                    if (analysis != null) {
                        allCommitAnalyses.add(analysis);
                    }
                }

                // 在循环外构造并发送汇总消息
                if (!allCommitAnalyses.isEmpty()) {
                    Map<String, Object> pushSummary = new HashMap<>();
                    pushSummary.put("repository", ((Map<String, Object>) payload.get("repository")).get("git_http_url"));
                    pushSummary.put("branch", payload.get("ref"));
                    pushSummary.put("totalCommits", commits.size());
                    pushSummary.put("commitAnalyses", allCommitAnalyses);

                    // 发送汇总消息
                    codeGuardianService.sendPushSummary(pushSummary);
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("处理 GitLab Webhook 失败", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private boolean isValidToken(String token) {
        if(apiToken.equals(token)){
            return true;
        }
        return false; // 临时返回 true，需要替换为实际的 token 验证逻辑
    }



}