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
package com.unionhole.zsguardian.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书工具类
 */
@Slf4j
@Component
public class FeishuUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static RestTemplate restTemplate;

    private static  Map<String,String> TOKEN_RESULT = new HashMap<>();

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        FeishuUtil.restTemplate = restTemplate;
    }

    /**
     * 获取access_token
     * token未失效，则直接取当前值
     * token失效，则调用接口重新获取
     * @return
     */
    public static String getAccessToken(String appId,String appSecret) {

        String token=TOKEN_RESULT.get("token");
        String expireTime=TOKEN_RESULT.get("expireTime");
        try {
            if ("".equals(token)|| token==null|| expireTime==null || "".equals(expireTime)){
                token=getToken(appId,appSecret);
            }else {
                if(Long.valueOf(expireTime)<=System.currentTimeMillis()){

                    token=getToken(appId,appSecret);
                }
            }
        } catch (Exception e) {
            log.error("获取token错误：{}",e);
        }

        return token;
    }


    private static String getToken(String appId,String appSecret) throws Exception {
        String url="https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
        Map<String,String> headerMap=new HashMap<>();
        headerMap.put("Content-Type","application/json; charset=utf-8");
        Map<String,String> jsonMap=new HashMap<>();
        jsonMap.put("app_id",appId);
        jsonMap.put("app_secret",appSecret);
        String jsonStr = objectMapper.writeValueAsString(jsonMap);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(jsonStr, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        Map<String, Object> responseStrResultMap = objectMapper.readValue(response.getBody(),
            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        String token = String.valueOf(responseStrResultMap.get("tenant_access_token"));
        String expire = String.valueOf(responseStrResultMap.get("expire"));
        long expireTime=System.currentTimeMillis();
        if(expire!=null){
            expireTime=expireTime+Long.valueOf(expire)*1000;
        }

        TOKEN_RESULT.put("token",token);
        TOKEN_RESULT.put("expireTime",String.valueOf(expireTime));

        return token;
    }


}
