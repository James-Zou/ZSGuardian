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
package com.unionhole.zsguardian.interceptor;

import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.service.UserService;
import com.unionhole.zsguardian.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    
    @Autowired
    private UserService userService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行静态资源
        if (isStaticResource(request.getRequestURI())) {
            return true;
        }
        
        // 放行登录相关接口
        if (isLoginPath(request.getRequestURI())) {
            return true;
        }
        
        // 先从Cookie中获取用户信息
        String userId = null;
        String username = null;
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("userId".equals(cookie.getName())) {
                    userId = cookie.getValue();
                } else if ("userName".equals(cookie.getName())) {
                    username = cookie.getValue();
                }
            }
        }
        
        // 如果Cookie中没有，则从header中获取
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(username)) {
            userId = request.getHeader("userId");
            username = request.getHeader("userName");
        }
        
        // 检查是否是AJAX请求
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = "XMLHttpRequest".equals(requestedWith);
        
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(username)) {
            if (isAjax) {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            } else {
                response.sendRedirect("/login.html");
            }
            return false;
        }
        
        // 验证用户信息
        User user = userService.findById(Long.parseLong(userId));
        if (user == null || !username.equals(user.getUsername())) {
            if (isAjax) {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":401,\"message\":\"用户信息无效\"}");
            } else {
                response.sendRedirect("/login.html");
            }
            return false;
        }
        
        // 设置用户信息到ThreadLocal
        UserHolder.setUser(user);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理ThreadLocal
        UserHolder.removeUser();
    }
    
    private boolean isStaticResource(String path) {
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.startsWith("/static/") ||
               path.startsWith("/lib/") ||
               path.endsWith(".html") ||
               path.endsWith(".ico");
    }
    
    private boolean isLoginPath(String path) {
        return path.equals("/api/login") || 
               path.equals("/api/logout") ||
               path.contains("/api/check/") ||
               path.equals("/api/register")||
               path.equals("/api/users/current");
    }
} 