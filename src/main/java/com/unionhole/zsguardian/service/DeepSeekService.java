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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DeepSeekService {

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-coder}")
    private String model;

    @Value("${deepseek.temperature:0.7}")
    private double temperature;

    @Value("${deepseek.max-tokens:2000}")
    private int maxTokens;

    @Autowired
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private XMindService xmindService;

    public DeepSeekService() {
        this.objectMapper = new ObjectMapper();
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的代码分析专家，请从影响范围、优化建议和示例代码三个方面进行分析。回答要具体且实用。");
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        
        return requestBody;
    }

    private List<Map<String, String>> callDeepSeekAPI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = buildRequestBody(prompt);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Calling DeepSeek API with prompt: {}", prompt);
            log.info("Calling DeepSeek API: {}", apiUrl);
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            log.info("API call successful");

            String aiResponse = extractAIResponse(response);
            return parseAIResponse(aiResponse);

        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            List<Map<String, String>> errorResult = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("level", "error");
            error.put("message", "AI分析失败: " + e.getMessage());
            errorResult.add(error);
            return errorResult;
        }
    }

    private String extractAIResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                String content = message.get("content");
                
                // 移除 Markdown 代码块标记
                content = cleanMarkdownCodeBlocks(content);
                
                return content;
            }
            throw new RuntimeException("AI响应格式不正确");
        } catch (Exception e) {
            log.error("解析AI响应失败", e);
            throw new RuntimeException("无法解析AI响应");
        }
    }

    private String cleanMarkdownCodeBlocks(String content) {
        if (content == null) {
            return "";
        }
        
        // 移除 Markdown 代码块标记
        content = content.replaceAll("```json\\s*", "");
        content = content.replaceAll("```\\s*", "");
        
        // 移除可能的 Markdown 标题标记
        content = content.replaceAll("#+\\s*", "");
        
        // 移除可能的 Markdown 列表标记
        content = content.replaceAll("[-*+]\\s*", "");
        
        // 移除可能的 Markdown 引用标记
        content = content.replaceAll(">\\s*", "");
        
        // 清理可能的前后空白字符
        content = content.trim();
        
        return content;
    }

    private List<Map<String, String>> parseAIResponse(String aiResponse) {
        try {
            Map<String, Object> fullResponse = objectMapper.readValue(aiResponse, new TypeReference<Map<String, Object>>() {});
            List<Map<String, String>> results = new ArrayList<>();
            
            // 添加评估级别
            Map<String, String> level = new HashMap<>();
            level.put("type", "评估级别");
            level.put("level", fullResponse.getOrDefault("level", "INFO").toString());
            results.add(level);
            
            // 添加影响范围分析
            Map<String, String> impact = new HashMap<>();
            impact.put("type", "影响范围");
            impact.put("content", fullResponse.getOrDefault("impactAnalysis", "无影响范围分析").toString());
            results.add(impact);
            
            // 添加优化建议
            Map<String, String> optimization = new HashMap<>();
            optimization.put("type", "优化建议");
            optimization.put("content", fullResponse.getOrDefault("optimizationSuggestions", "无优化建议").toString());
            results.add(optimization);
            
            // 添加改造后代码
            Map<String, String> example = new HashMap<>();
            example.put("type", "改造后代码");
            example.put("content", fullResponse.getOrDefault("exampleCode", "无示例代码").toString());
            results.add(example);
            
            return results;
            
        } catch (Exception e) {
            log.error("解析AI响应JSON失败", e);
            List<Map<String, String>> errorResult = new ArrayList<>();
            Map<String, String> error = new HashMap<>();
            error.put("type", "错误");
            error.put("content", "AI响应解析失败: " + e.getMessage());
            errorResult.add(error);
            return errorResult;
        }
    }

    public List<Map<String, String>> analyzeCode(String code) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下代码，并从四个维度给出分析结果：\n\n");
        prompt.append("1. 评估级别：根据问题的严重程度，给出以下评级：\n");
        prompt.append("   - CRITICAL: 严重问题，需要立即修复\n");
        prompt.append("   - MAJOR: 重要问题，建议尽快修复\n");
        prompt.append("   - MINOR: 次要问题，可以在后续迭代中改进\n");
        prompt.append("   - INFO: 建议性改进\n");
        prompt.append("2. 影响范围：分析代码修改可能影响的功能和模块\n");
        prompt.append("3. 优化建议：提供具体的优化和改进建议\n");
        prompt.append("4. 改造后代码：给出优化后的完整代码示例\n\n");
        prompt.append("代码：\n");
        prompt.append(code);
        prompt.append("\n\n请按照以下JSON格式返回分析结果：\n");
        prompt.append("{\n");
        prompt.append("  \"level\": \"CRITICAL|MAJOR|MINOR|INFO\",\n");
        prompt.append("  \"impactAnalysis\": \"影响范围分析...\",\n");
        prompt.append("  \"optimizationSuggestions\": \"优化建议...\",\n");
        prompt.append("  \"exampleCode\": \"示例代码...\"\n");
        prompt.append("}");

        return callDeepSeekAPI(prompt.toString());
    }


    public Map<String, Object> correctAnalysis(Map<String, Object> parseResult) {
        try {
            // 构建 AI 提示
            StringBuilder prompt = new StringBuilder();
            prompt.append("作为一个测试专家，请分析以下测试设计的影响范围和测试用例，并提供改进建议和优化后的测试用例大纲。\n\n");

            // 添加影响范围
            @SuppressWarnings("unchecked")
            List<String> impactAreas = (List<String>) parseResult.get("impactAreas");
            prompt.append("影响范围：\n");
            for (String area : impactAreas) {
                prompt.append("- ").append(area).append("\n");
            }

            // 添加测试用例
            @SuppressWarnings("unchecked")
            Map<String, Object> categories = (Map<String, Object>) parseResult.get("categories");
            @SuppressWarnings("unchecked")
            Map<String, List<String>> testCasesByCategory =
                    (Map<String, List<String>>) categories.get("testCasesByCategory");

            prompt.append("\n测试用例：\n");
            for (Map.Entry<String, List<String>> entry : testCasesByCategory.entrySet()) {
                prompt.append(entry.getKey()).append(":\n");
                for (String testCase : entry.getValue()) {
                    prompt.append("- ").append(testCase).append("\n");
                }
                prompt.append("\n");
            }

            prompt.append("\n请提供以下内容：\n");
            prompt.append("1. 影响范围的完整性和准确性分析\n");
            prompt.append("2. 测试用例的覆盖率和质量分析\n");
            prompt.append("3. 测试设计的整体改进建议\n");
            prompt.append("4. 优化后的完整测试用例大纲\n");
            prompt.append("5. 总字数不超过3000字\n");
            prompt.append("\n请以JSON格式返回，包含以下字段：\n");
            prompt.append("{\n");
            prompt.append("  \"impactAreaSuggestions\": [\"建议1\", \"建议2\"],\n");
            prompt.append("  \"testCaseSuggestions\": {\"影响范围1\": [\"建议1\", \"建议2\"]},\n");
            prompt.append("  \"generalSuggestions\": [\"整体建议1\", \"整体建议2\"],\n");
            prompt.append("  \"improvedOutline\": {\n");
            prompt.append("    \"rootTopic\": \"测试用例（优化版）\",\n");
            prompt.append("    \"topics\": [{\n");
            prompt.append("      \"title\": \"影响范围1\",\n");
            prompt.append("      \"subtopics\": [{\n");
            prompt.append("        \"title\": \"测试场景1\",\n");
            prompt.append("        \"testCases\": [\"测试用例1\", \"测试用例2\"]\n");
            prompt.append("      }]\n");
            prompt.append("    }]\n");
            prompt.append("  }\n");
            prompt.append("}\n");

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt.toString());
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 3000);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) messageObj.get("content");

                    // 清理 Markdown 代码块标记
                    content = cleanMarkdownCodeBlock(content);

                    // 解析 JSON 响应
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> aiSuggestions = mapper.readValue(content, Map.class);

                    try {
                        // 生成改进后的 XMind 文件，添加错误处理
                        String improvedFileName = xmindService.generateImprovedXMind(parseResult, aiSuggestions);
                        if (improvedFileName == null) {
                            log.error("生成 XMind 文件失败");
                            throw new RuntimeException("生成 XMind 文件失败");
                        }

                        // 验证生成的文件是否为有效的 XMind 文件
                        if (!xmindService.validateXMindFile(improvedFileName)) {
                            log.error("生成的 XMind 文件格式无效");
                            throw new RuntimeException("生成的 XMind 文件格式无效");
                        }

                        // 构建返回结果
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("aiCorrection", aiSuggestions);
                        result.put("improvedXMindFile", improvedFileName);
                        return result;
                    } catch (Exception e) {
                        log.error("XMind 文件处理失败", e);
                        throw new RuntimeException("XMind 文件处理失败: " + e.getMessage());
                    }
                }
            }

            throw new RuntimeException("AI 服务响应异常");

        } catch (Exception e) {
            log.error("AI 校正分析失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "AI 校正分析失败: " + e.getMessage());
            return error;
        }
    }

    private String cleanMarkdownCodeBlock(String content) {
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring("```json".length());
        } else if (content.startsWith("```")) {
            content = content.substring("```".length());
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - "```".length());
        }
        return content.trim();
    }





    public String generateTestOutline(String requirement) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", buildPrompt(requirement));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", Arrays.asList(message));
            
            log.info("Request body: {}", new ObjectMapper().writeValueAsString(requestBody));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            log.info("Response body: {}", response.getBody());
            
            if (response.getBody() != null && response.getBody().get("choices") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
                    return formatOutline((String) messageObj.get("content"));
                }
            }
            
            throw new RuntimeException("Failed to generate test outline: Empty response");
        } catch (Exception e) {
            log.error("Failed to generate test outline", e);
            throw new RuntimeException("Failed to generate test outline: " + e.getMessage());
        }
    }
    
    private String buildPrompt(String requirementDescription) {
        return String.format(
            "作为一个测试专家，请基于以下需求描述生成结构化的测试用例树。请遵循以下规则：\n\n" +
            "需求描述：\n%s\n\n" +
            "生成规则：\n" +
            "1. 总用例数量控制在1000条以内\n" +
            "2. 每个文本字段长度限制：\n" +
            "   - 标题：最多50个字符\n" +
            "   - 描述：最多200个字符\n" +
            "   - 前置条件：最多200个字符\n" +
            "   - 测试步骤：最多500个字符\n" +
            "   - 预期结果：最多200个字符\n" +
            "3. 优先级分配规则：\n" +
            "   - HIGH: 核心功能、重要业务流程（约20%%）\n" +
            "   - MEDIUM: 一般功能、常规场景（约50%%）\n" +
            "   - LOW: 边缘场景、低频功能（约30%%）\n\n" +
            "请按以下分类组织测试用例：\n" +
            "1. 功能测试\n" +
            "   - 基础功能验证\n" +
            "   - 业务流程验证\n" +
            "   - 数据处理验证\n" +
            "2. 异常测试\n" +
            "   - 输入验证\n" +
            "   - 错误处理\n" +
            "   - 边界条件\n" +
            "3. 性能测试\n" +
            "   - 响应时间\n" +
            "   - 并发处理\n" +
            "4. 安全测试\n" +
            "   - 权限控制\n" +
            "   - 数据安全\n\n" +
            "返回格式要求：\n" +
            "请直接返回JSON数组，每个节点包含以下字段：\n" +
            "{\n" +
            "    \"id\": 数字类型的唯一标识（从1开始递增）,\n" +
            "    \"title\": \"节点标题（≤50字符）\",\n" +
            "    \"type\": \"folder或case\",\n" +
            "    \"description\": \"描述（≤200字符）\",\n" +
            "    \"children\": [],\n" +
            "    \"priority\": \"HIGH/MEDIUM/LOW\",\n" +
            "    \"precondition\": \"前置条件（≤200字符）\",\n" +
            "    \"steps\": \"测试步骤（≤500字符）\",\n" +
            "    \"expectedResult\": \"预期结果（≤200字符）\",\n" +
            "    \"parentId\": null\n" +
            "}\n\n" +
            "注意事项：\n" +
            "1. 直接返回JSON数组，不要包含任何其他文字说明\n" +
            "2. id必须是数字类型，不能使用字符串\n" +
            "3. 所有文本内容必须是中文\n" +
            "4. 测试步骤应当简洁明了，每个步骤使用数字编号\n" +
            "5. 优先生成关键功能的测试用例\n" +
            "6. 避免重复或冗余的测试用例\n" +
            "7. folder类型的节点不需要填写priority、precondition、steps和expectedResult字段\n" +
            "8. 确保JSON格式正确，可以直接被解析\n" +
            "9. 每个case节点必须包含完整的测试信息\n" +
            "10. 树形结构要清晰，folder节点用于组织分类，case节点包含具体测试用例\n",
            requirementDescription);
    }

    private String formatOutline(String rawOutline) {
        try {
            // 清理 Markdown 语法
            String cleanedContent = cleanMarkdownCodeBlocks(rawOutline);
            
            // 提取 JSON 部分
            int jsonStart = cleanedContent.indexOf("[");
            int jsonEnd = cleanedContent.lastIndexOf("]") + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonPart = cleanedContent.substring(jsonStart, jsonEnd);
                // 格式化 JSON
                JsonNode node = objectMapper.readTree(jsonPart);
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            }
            throw new RuntimeException("No valid JSON array found in response");
        } catch (Exception e) {
            log.error("Failed to format outline", e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }
} 