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

import com.unionhole.zsguardian.dto.UserDTO;
import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.service.UserService;
import com.unionhole.zsguardian.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserManageController {
    @Autowired
    private UserService userService;

    @Value("${admin.user.id}")
    private String  adminUserId;

    // 检查是否是管理员
    private void checkAdmin(String currentUserId) {
        if (!adminUserId.equals(currentUserId)) {
            throw new RuntimeException("无权限执行此操作");
        }
    }

    // 获取用户列表
    @GetMapping
    public ResponseEntity<Page<UserDTO>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        
        User currentUser = UserHolder.getUser();
        checkAdmin(String.valueOf(currentUser.getId()));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> users;
        
        if (StringUtils.hasText(search)) {
            users = userService.searchUsers(search, pageable);
        } else {
            users = userService.findAll(pageable);
        }
        
        return ResponseEntity.ok(users.map(UserDTO::fromUser));
    }

    // 启用/禁用用户
    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Boolean enabled) {
        User currentUser = UserHolder.getUser();
        checkAdmin(String.valueOf(currentUser.getId()));
        
        userService.updateUserStatus(userId, enabled);
        return ResponseEntity.ok().build();
    }

    // 重置用户密码
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        User currentUser = UserHolder.getUser();
        checkAdmin(String.valueOf(currentUser.getId()));
        
        String newPassword = body.get("password");
        userService.updatePassword(userId, newPassword);
        return ResponseEntity.ok().build();
    }
} 