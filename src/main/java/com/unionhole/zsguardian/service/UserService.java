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

import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.repository.UserRepository;
import com.unionhole.zsguardian.dto.UserDTO;
import com.unionhole.zsguardian.utils.UserHolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;




    @Value("${admin.email}")
    private String adminEmail;
    
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void updateUserStatus(Long userId, Boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("用户状态更新 - userId: {}, enabled: {}", userId, enabled);
    }

    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(newPassword);
        userRepository.save(user);
        log.info("用户密码已重置 - userId: {}", userId);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (!user.getEnabled()) {
            throw new RuntimeException(String.format("账号已被禁用，请联系管理员 %s 激活账号", adminEmail));
        }

        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setLastLoginTime(new Date());
        user.setLastLoginIp(getCurrentIp());
        return userRepository.save(user);
    }
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElse(null);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private String getCurrentIp() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> ((ServletRequestAttributes) attributes).getRequest())
                .map(request -> {
                    String ip = request.getHeader("X-Forwarded-For");
                    if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                        ip = request.getHeader("Proxy-Client-IP");
                    }
                    if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                        ip = request.getHeader("WL-Proxy-Client-IP");
                    }
                    if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                        ip = request.getRemoteAddr();
                    }
                    return ip;
                })
                .orElse("unknown");
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsers(String search, Pageable pageable) {
        return userRepository.findByUsernameContainingOrEmailContainingOrPhoneContaining(
            search, search, search, pageable);
    }

    public List<UserDTO> getAvailableExecutors() {
        return userRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }

    public List<User> searchUsers(String query) {
        return userRepository.findByUsernameContaining(query);
    }

    public User getCurrentUser() {
        return userRepository.findById(UserHolder.getUser().getId())
            .orElseThrow(() -> new RuntimeException("Current user not found"));
    }
} 