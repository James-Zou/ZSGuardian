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

import com.unionhole.zsguardian.jmeter.TestPlanBuilder;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;

@Configuration
@Slf4j
public class JMeterConfig {

    private final Environment environment;

    public JMeterConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Lazy
    public StandardJMeterEngine jmeterEngine() {
        try {
            // 设置JMeter Home目录
            String jmeterHome = System.getProperty("user.dir");
            JMeterUtils.setJMeterHome(jmeterHome);
            
            // 创建并设置系统属性文件
            File propsFile = File.createTempFile("jmeter", ".properties");
            Properties props = new Properties();
            props.store(new FileOutputStream(propsFile), "Temporary JMeter Properties");
            JMeterUtils.loadJMeterProperties(propsFile.getAbsolutePath());
            
            // 初始化属性
            Properties jmeterProps = new Properties();
            jmeterProps.setProperty("jmeter.save.saveservice.output_format", environment.getProperty("jmeter.save.saveservice.output_format", "xml"));
            jmeterProps.setProperty("jmeter.save.saveservice.response_data", environment.getProperty("jmeter.save.saveservice.response_data", "true"));
            jmeterProps.setProperty("jmeter.save.saveservice.samplerData", environment.getProperty("jmeter.save.saveservice.samplerData", "true"));
            jmeterProps.setProperty("jmeter.save.saveservice.requestHeaders", environment.getProperty("jmeter.save.saveservice.requestHeaders", "true"));
            jmeterProps.setProperty("jmeter.save.saveservice.responseHeaders", environment.getProperty("jmeter.save.saveservice.responseHeaders", "true"));
            jmeterProps.setProperty("jmeter.save.saveservice.encoding", environment.getProperty("jmeter.save.saveservice.encoding", "UTF-8"));
            jmeterProps.setProperty("sampleresult.default.encoding", environment.getProperty("sampleresult.default.encoding", "UTF-8"));
            jmeterProps.setProperty("httpclient4.retrycount", environment.getProperty("httpclient4.retrycount", "0"));
            jmeterProps.setProperty("hc.parameters.file", environment.getProperty("hc.parameters.file", "hc.parameters"));
            jmeterProps.setProperty("jmeter.save.saveservice.timestamp_format", environment.getProperty("jmeter.save.saveservice.timestamp_format", "yyyy-MM-dd HH:mm:ss"));

            // 将属性设置到JMeterUtils
            JMeterUtils.getJMeterProperties().putAll(jmeterProps);
            JMeterUtils.initLocale();
            
            return new StandardJMeterEngine();
        } catch (Exception e) {
            log.error("初始化JMeter引擎失败", e);
            throw new RuntimeException("JMeter配置错误: " + e.getMessage());
        }
    }

    @Bean
    public TestPlanBuilder testPlanBuilder() {
        return new TestPlanBuilder();
    }
} 