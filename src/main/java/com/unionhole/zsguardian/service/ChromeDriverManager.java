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

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.lang.Process;
import java.lang.ProcessBuilder;

@Service
@Slf4j
public class ChromeDriverManager {
    
    private String configuredDriverPath;

    public String getConfiguredDriverPath() {
        return configuredDriverPath != null ? configuredDriverPath : getDefaultDriverPath();
    }

    public void setDriverPath(String path) {
        this.configuredDriverPath = path;
        System.setProperty("webdriver.chrome.driver", path);
    }


    public boolean isDriverConfigured() {
        String driverPath = configuredDriverPath != null ? configuredDriverPath : getDefaultDriverPath();
        return new File(driverPath).exists();
    }
    
    public boolean isChromeInstalled() {
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            if (osName.contains("win")) {
                return new File("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe").exists() ||
                       new File("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe").exists();
            } else if (osName.contains("mac")) {
                return new File("/Applications/Google Chrome.app").exists();
            } else {
                // Linux
                ProcessBuilder pb = new ProcessBuilder("which", "google-chrome");
                Process p = pb.start();
                return p.waitFor() == 0;
            }
        } catch (Exception e) {
            log.error("检查Chrome安装状态失败", e);
            return false;
        }
    }
    
    public String getDefaultDriverPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "C:/webdriver/chromedriver.exe";
        } else if (osName.contains("mac")) {
            return "/usr/local/bin/chromedriver";
        } else {
            return "/usr/bin/chromedriver";
        }
    }
    
    public String getSetupInstructions() {
        StringBuilder sb = new StringBuilder();
        sb.append("环境配置说明：\n\n");
        
        // Chrome浏览器检查
        if (!isChromeInstalled()) {
            sb.append("1. 请安装Chrome浏览器\n");
            sb.append("   下载地址：https://www.google.com/chrome/\n\n");
        }
        
        // ChromeDriver配置说明
        sb.append("2. 配置ChromeDriver：\n");
        sb.append("   a) 查看Chrome版本：在浏览器地址栏输入 chrome://version\n");
        sb.append("   b) 下载对应版本ChromeDriver：https://chromedriver.chromium.org/downloads\n");
        sb.append("   c) 将ChromeDriver放置在以下位置之一：\n");
        sb.append("      - ").append(getDefaultDriverPath()).append(" (推荐)\n");
        sb.append("      - 或通过系统属性指定：-Dwebdriver.chrome.driver=/path/to/chromedriver\n\n");
        
        return sb.toString();
    }
} 