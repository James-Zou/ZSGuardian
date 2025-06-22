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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebDriverFactory {
    
    @Autowired
    private ChromeDriverManager driverManager;
    
    private WebDriver webDriver;
    
    public synchronized WebDriver getDriver() {
        if (!driverManager.isDriverConfigured()) {
            throw new IllegalStateException(driverManager.getSetupInstructions());
        }

        if (!driverManager.isChromeInstalled()) {
            throw new IllegalStateException("请先安装Chrome浏览器");
        }

        if (webDriver == null) {
            String driverPath = System.getProperty("webdriver.chrome.driver");
            if (driverPath == null) {
                driverPath = driverManager.getDefaultDriverPath();
                System.setProperty("webdriver.chrome.driver", driverPath);
            }

            ChromeOptions options = new ChromeOptions();
            try {
                webDriver = new ChromeDriver(options);
            } catch (Exception e) {
                log.error("创建ChromeDriver失败", e);
                throw new IllegalStateException("创建ChromeDriver失败，请确保环境配置正确: " + e.getMessage());
            }
        }
        
        return webDriver;
    }
    
    public void quitDriver() {
        if (webDriver != null) {
            try {
                webDriver.quit();
            } catch (Exception e) {
                log.error("关闭WebDriver失败", e);
            } finally {
                webDriver = null;
            }
        }
    }
} 