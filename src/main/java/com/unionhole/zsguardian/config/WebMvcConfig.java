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

package com.unionhole.zsguardian.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.beans.factory.annotation.Autowired;
import com.unionhole.zsguardian.interceptor.LoginInterceptor;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Value("${file.upload.path}")
    private String uploadPath;



    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/templates/");  // 视图文件的位置
        resolver.setSuffix(".html");        // 视图文件的后缀
        return resolver;
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(viewResolver());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 处理静态资源
        registry.addResourceHandler("/static/**", "/css/**", "/js/**", "/images/**", "/lib/**")
               .addResourceLocations("classpath:/static/", "classpath:/static/css/", 
                                   "classpath:/static/js/", "classpath:/static/images/", "classpath:/static/lib/")
               .setCachePeriod(3600)
               .resourceChain(true)
               .addResolver(new PathResourceResolver());

        // 处理SPA应用的路由
        registry.addResourceHandler("/**")
               .addResourceLocations("classpath:/static/")
               .resourceChain(true)
               .addResolver(new PathResourceResolver() {
                   @Override
                   protected Resource getResource(String resourcePath, Resource location) throws IOException {
                       Resource requestedResource = location.createRelative(resourcePath);
                       
                       if (requestedResource.exists() && requestedResource.isReadable()) {
                           return requestedResource;
                       }
                       
                       // 如果是登录页面，直接返回
                       if (resourcePath.equals("login.html")) {
                           return new ClassPathResource("/static/login.html");
                       }
                       
                       // API请求不重定向
                       if (resourcePath.startsWith("api/")) {
                           return null;
                       }
                       
                       // 静态资源不重定向
                       if (resourcePath.startsWith("lib/") || 
                           resourcePath.startsWith("css/") || 
                           resourcePath.startsWith("js/") || 
                           resourcePath.startsWith("images/") ||
                           resourcePath.startsWith("static/")) {
                           return null;
                       }
                       
                       // 未登录用户重定向到登录页
                       return new ClassPathResource("/static/login.html");
                   }
               });

        // 处理文件下载
        registry.addResourceHandler("/api/download/**")
               .addResourceLocations("file:" + uploadPath + "/")
               .setCachePeriod(3600)
               .resourceChain(true)
               .addResolver(new PathResourceResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
               .addPathPatterns("/**")
               .excludePathPatterns(
                   "/login",
                   "/login.html",
                   "/api/login",
                   "/api/logout",
                   "/codeguardian",
                   "/static/**",
                   "/css/**",
                   "/js/**",
                   "/lib/**",
                   "/images/**",
                   "/*.ico"
               );
    }
} 