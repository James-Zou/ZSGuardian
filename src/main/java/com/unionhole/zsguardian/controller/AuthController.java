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

import com.unionhole.zsguardian.dto.ErrorResponse;
import com.unionhole.zsguardian.dto.LoginResponse;
import com.unionhole.zsguardian.entity.Role;
import com.unionhole.zsguardian.entity.UserRoles;
import com.unionhole.zsguardian.model.LoginRequest;
import com.unionhole.zsguardian.service.RoleService;
import com.unionhole.zsguardian.service.UserRolesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.service.UserService;
import com.unionhole.zsguardian.dto.RegisterRequest;
import java.util.Date;
import java.util.stream.Collectors;

import com.unionhole.zsguardian.dto.UserDTO;

@Slf4j
@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRolesService userRolesService;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            
            // 设置Cookie
            Cookie userIdCookie = new Cookie("userId", user.getId().toString());
            Cookie usernameCookie = new Cookie("userName", user.getUsername());
            
            // 设置Cookie属性
            int maxAge = 30 * 24 * 60 * 60; // 7天
            userIdCookie.setMaxAge(maxAge);
            usernameCookie.setMaxAge(maxAge);
            userIdCookie.setPath("/");
            usernameCookie.setPath("/");
            
            response.addCookie(userIdCookie);
            response.addCookie(usernameCookie);
            
            return ResponseEntity.ok(new LoginResponse(user));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 清除 Cookie
            Cookie userIdCookie = new Cookie("userId", null);
            Cookie usernameCookie = new Cookie("userName", null);
            
            userIdCookie.setMaxAge(0);
            usernameCookie.setMaxAge(0);
            userIdCookie.setPath("/");
            usernameCookie.setPath("/");
            
            response.addCookie(userIdCookie);
            response.addCookie(usernameCookie);
            
            result.put("success", true);
            result.put("message", "退出登录成功");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("退出登录失败", e);
            result.put("success", false);
            result.put("message", "退出登录失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/check/username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@PathVariable String username) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !userService.existsByUsername(username));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/phone/{phone}")
    public ResponseEntity<Map<String, Boolean>> checkPhone(@PathVariable String phone) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !userService.existsByPhone(phone));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/email/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@PathVariable String email) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("available", !userService.existsByEmail(email));
        return ResponseEntity.ok(response);
    }



    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // 再次验证唯一性
            if (userService.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("用户名已被使用"));
            }
            if (userService.existsByPhone(request.getPhone())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("手机号已被注册"));
            }
            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("邮箱已被注册"));
            }

            // 创建新用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword()); // 注意：密码已经在前端进行了MD5加密
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            userService.save(user);
            UserRoles userRoles=new UserRoles();
            userRoles.setRoleId(Long.valueOf(2));
            userRoles.setUserId(user.getId());
            userRolesService.insertUserRole(userRoles);
            return ResponseEntity.ok(new LoginResponse(user));
        } catch (Exception e) {
            log.error("注册失败", e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("注册失败：" + e.getMessage()));
        }
    }

    @GetMapping("/users/current")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // 从 cookie 中获取用户 ID
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                String userId = null;
                for (Cookie cookie : cookies) {
                    if ("userId".equals(cookie.getName())) {
                        userId = cookie.getValue();
                        break;
                    }
                }
                
                if (userId != null) {
                    User user = userService.findById(Long.parseLong(userId));
                    if (user != null) {
                        // 不返回敏感信息
                        UserDTO userDTO = new UserDTO();
                        userDTO.setId(user.getId());
                        userDTO.setUsername(user.getUsername());
                        userDTO.setEmail(user.getEmail());
                        userDTO.setPhone(user.getPhone());
                        userDTO.setEnabled(user.getEnabled());
                        userDTO.setCreateTime(user.getCreateTime());
                        userDTO.setLastLoginTime(user.getLastLoginTime());
                        userDTO.setRoles(user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.toList()));
                        return ResponseEntity.ok(userDTO);
                    }
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
} 