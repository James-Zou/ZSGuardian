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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Value("${admin.password}")
    private String adminPassword;  // 从配置文件读取管理员密码的MD5值

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAdmin(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        Map<String, Object> response = new HashMap<>();
        
        if (adminPassword.equals(password)) {
            response.put("success", true);
            response.put("message", "验证成功");
        } else {
            response.put("success", false);
            response.put("message", "密码错误");
        }
        
        return ResponseEntity.ok(response);
    }
} 