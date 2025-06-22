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

import com.unionhole.zsguardian.entity.CodeReviewRecord;
import com.unionhole.zsguardian.utils.FeishuUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

@Service
@Slf4j
public class FeiShuService {

    @Value("${feishu.appId}")
    private  String appId;

    @Value("${feishu.appSecret}")
    private  String appSecret;

    @Value("${feishu.chatId}")
    private String chatId;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CodeReviewRecordService codeReviewRecordService;

    public void sendCodeReviewMessage(Map<String, Object> content) {
        try {
            // 构造卡片内容
            Map<String, Object> card = new HashMap<>();
            card.put("config", Collections.singletonMap("wide_screen_mode", true));

            // 构造卡片头部
            Map<String, Object> header = new HashMap<>();
            Map<String, Object> title = new HashMap<>();
            title.put("content", "AI代码审查通知");
            title.put("tag", "plain_text");
            header.put("title", title);
            header.put("template", "blue");
            card.put("header", header);

            List<Map<String, Object>> elements = new ArrayList<>();

            // 获取最新提交的作者和时间
            String author = "未知";
            String commitTime = "未知";
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commitAnalyses = (List<Map<String, Object>>) content.get("commitAnalyses");
            if (commitAnalyses != null && !commitAnalyses.isEmpty()) {
                Map<String, Object> latestCommit = commitAnalyses.get(0);
                author = Optional.ofNullable(latestCommit.get("author"))
                        .map(Object::toString)
                        .orElse("未知");
                commitTime = Optional.ofNullable(latestCommit.get("commitTime"))
                        .map(Object::toString)
                        .orElse("未知");
            }

            // 添加基本信息
            Map<String, Object> basicInfo = new HashMap<>();
            basicInfo.put("tag", "div");
            Map<String, Object> textMap = new HashMap<>();
            textMap.put("tag", "lark_md");
            textMap.put("content", String.format(
                    "**代码仓库**: %s\n" +
                            "**分支**: %s\n" +
                            "**提交数**: %d\n" +
                            "**作者**: %s\n" +
                            "**提交时间**: %s",
                    Optional.ofNullable(content.get("repository")).orElse("未知"),
                    Optional.ofNullable(content.get("branch")).orElse("未知"),
                    Optional.ofNullable(content.get("totalCommits")).orElse(0),
                    author,
                    commitTime
            ));
            basicInfo.put("text", textMap);
            elements.add(basicInfo);

            // 添加分隔线
            elements.add(Collections.singletonMap("tag", "hr"));

            // 计算统计信息
            int changedFiles = 0;
            int criticalIssues = 0;
            int majorIssues = 0;
            int minorIssues = 0;
            int infoIssues = 0;

            if (commitAnalyses != null) {
                for (Map<String, Object> commit : commitAnalyses) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> fileAnalyses = (List<Map<String, Object>>) commit.get("fileAnalyses");
                    if (fileAnalyses != null) {
                        changedFiles += fileAnalyses.size();

                        for (Map<String, Object> file : fileAnalyses) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> analysis = (Map<String, Object>) file.get("analysis");
                            if (analysis != null) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> issues = (List<Map<String, Object>>) analysis.get("issues");
                                if (issues != null) {
                                    for (Map<String, Object> issue : issues) {
                                        String type = (String) issue.get("type");
                                        // 只有当 type 为"评估级别"时才统计 level
                                        if ("评估级别".equals(type)) {
                                            String level = (String) issue.get("level");
                                            switch (level) {
                                                case "CRITICAL": criticalIssues++; break;
                                                case "MAJOR": majorIssues++; break;
                                                case "MINOR": minorIssues++; break;
                                                case "INFO": infoIssues++; break;
                                                default: break;  // 移除默认情况下增加 infoIssues 的逻辑
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 更新统计信息到content
            content.put("changedFiles", changedFiles);
            content.put("criticalIssues", criticalIssues);
            content.put("majorIssues", majorIssues);
            content.put("minorIssues", minorIssues);
            content.put("infoIssues", infoIssues);

            // 添加统计信息
            Map<String, Object> statsInfo = new HashMap<>();
            statsInfo.put("tag", "div");
            Map<String, Object> statsText = new HashMap<>();
            statsText.put("tag", "lark_md");
            statsText.put("content", String.format("**审查统计**\n- 变更文件数：%d\n- <font color='#1890FF'>MAJOR级别问题：%d</font>\n- <font color='#1890FF'>MINOR级别问题：%d</font>\n",
                    changedFiles, majorIssues, minorIssues));
            statsInfo.put("text", statsText);
            elements.add(statsInfo);

            // 保存记录并获取ID
            CodeReviewRecord record = codeReviewRecordService.saveReviewRecord(content);

            // 添加按钮
            Map<String, Object> actions = new HashMap<>();
            actions.put("tag", "action");

            Map<String, Object> button = new HashMap<>();
            button.put("tag", "button");
            Map<String, Object> buttonText = new HashMap<>();
            buttonText.put("tag", "plain_text");
            buttonText.put("content", "查看详细报告");
            button.put("text", buttonText);
            button.put("type", "primary");
            button.put("url", String.format("%s/code-review-details/%d", baseUrl, record.getId()));

            actions.put("actions", Collections.singletonList(button));
            elements.add(actions);

            card.put("elements", elements);

            // 将卡片内容转换为JSON字符串
            String cardJson = objectMapper.writeValueAsString(card);

            // 构造符合飞书API要求的消息格式
            Map<String, Object> message = new HashMap<>();
            message.put("receive_id", chatId);
            message.put("msg_type", "interactive");
            message.put("content", cardJson);
            message.put("uuid", UUID.randomUUID().toString());  // 添加uuid字段

            String jsonContent = objectMapper.writeValueAsString(message);
            log.info("发送的消息内容: {}", jsonContent);

            String response = sendMessage(jsonContent);

            if (!response.contains("\"code\":0")) {
                log.error("飞书消息发送失败: {}", response);
                throw new RuntimeException("发送飞书消息失败");
            }
        } catch (Exception e) {
            log.error("构造或发送飞书消息失败", e);
            throw new RuntimeException("发送飞书消息失败", e);
        }
    }

    private String sendMessage(String jsonContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getAccessToken());

        HttpEntity<String> request = new HttpEntity<>(jsonContent, headers);

        String url = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id";
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return response.getBody();
    }

    private String getAccessToken() {
        return FeishuUtil.getAccessToken(appId, appSecret);
    }










}