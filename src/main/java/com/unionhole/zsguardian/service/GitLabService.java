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
import org.gitlab4j.api.GitLabApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GitLabService {
    
    @Value("${gitlab.api.tokens.default}")
    private String defaultGitlabToken;
    
    @Value("${gitlab.api.project.tokens}")
    private String projectTokens;

    @Value("${gitlab.api.url:https://git.james.com/api/v4}")
    private String gitlabApiUrl;

    @Value("${gitlab.domain:https://git.james.com}")
    private String gitlabDomain;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<Integer, String> tokenMap=new HashMap<>();
    
    private GitLabApi createGitLabApi() {
        GitLabApi gitLabApi = new GitLabApi(gitlabDomain, defaultGitlabToken);
        
        // 设置更长的连接和读取超时时间
        gitLabApi.setRequestTimeout(60000,360000);  // 60秒
        return gitLabApi;
    }

    private String getTokenForProject(Integer projectId) {
        if (tokenMap.size() != 0) {
            return tokenMap.get(projectId);
        }
        if (projectTokens != null && !projectTokens.isEmpty()) {
            String[] tokens = projectTokens.split(";");
            for (String token : tokens) {
                String[] parts = token.split(":");
                if (parts.length == 2) {
                    try {
                        Integer id = Integer.parseInt(parts[0]);
                        tokenMap.put(id, parts[1]);
                    } catch (NumberFormatException e) {
                        log.error("Invalid project ID in gitlab.api.project.tokens: " + parts[0]);
                    }
                }
            }
        }
        return tokenMap.get(projectId);
    }



    public List<Map<String, Object>> getDiffInformation(Integer projectId, String commitId) {
        int maxRetries = 5;
        
        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("PRIVATE-TOKEN", getTokenForProject(projectId));
                String url = gitlabApiUrl + "/projects/" + projectId + "/repository/commits/" + commitId + "/diff";
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                log.info("getDiffInformation response is {}", objectMapper.writeValueAsString(response));
                return response.getBody();
                
            } catch (Exception e) {
                log.warn("获取差异信息失败 (尝试 {}/{}): commitId={} - {}", 
                    retryCount + 1, maxRetries, commitId, e.getMessage());
                
                if (retryCount >= maxRetries - 1) {
                    log.error("获取差异信息最终失败: commitId={}", commitId, e);
                    return null;
                }
                
                // 使用指数退避策略
                long waitTime = (long) (Math.pow(2, retryCount + 1) * 1000);
                
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        
        return null;
    }


    

} 