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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unionhole.zsguardian.entity.TestCase;
import com.unionhole.zsguardian.entity.TestResult;
import com.unionhole.zsguardian.model.APIConfig;
import com.unionhole.zsguardian.model.JMeterTestPlan;
import com.unionhole.zsguardian.model.TestStep;
import com.unionhole.zsguardian.repository.TestCaseRepository;
import com.unionhole.zsguardian.repository.TestResultRepository;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.jorphan.collections.HashTree;
import org.apache.jmeter.threads.ThreadGroup;

import java.time.LocalDateTime;
import java.util.List;
import java.net.URL;
import java.net.MalformedURLException;

@Service
public class TestService {
    
    @Autowired
    private WebDriverFactory webDriverFactory;
    
    @Autowired
    private StandardJMeterEngine jmeterEngine;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TestCaseRepository testCaseRepository;
    
    @Autowired
    private TestResultRepository testResultRepository;
    
    public TestResult executeTest(Long testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
            .orElseThrow(() -> new RuntimeException("Test case not found"));
            
        if ("UI".equals(testCase.getType())) {
            return executeUITest(testCase);
        } else {
            return executeAPITest(testCase);
        }
    }
    
    private TestResult executeUITest(TestCase testCase) {
        TestResult result = new TestResult();
        try {
            List<TestStep> steps = objectMapper.readValue(testCase.getSteps(),
                new TypeReference<List<TestStep>>() {});
                
            for (TestStep step : steps) {
                executeUIStep(step);
            }
            
            result.setStatus("PASS");
        } catch (Exception e) {
            result.setStatus("FAIL");
            result.setLogs(e.getMessage());
        }
        
        return result;
    }
    
    private TestResult executeAPITest(TestCase testCase) {
        TestResult result = new TestResult();
        try {
            APIConfig config = objectMapper.readValue(testCase.getConfiguration(), APIConfig.class);
            JMeterTestPlan testPlan = createJMeterTestPlan(config);
            
            // Create and configure HashTree
            HashTree testPlanTree = new HashTree();
            testPlanTree.add(testPlan.getTestPlan());
            HashTree threadGroupTree = testPlanTree.add(testPlan.getTestPlan(), testPlan.getThreadGroup());
            threadGroupTree.add(testPlan.getHttpSampler());
            
            // Run the test
            jmeterEngine.configure(testPlanTree);
            jmeterEngine.run();
            
            result.setStatus("PASS");
        } catch (Exception e) {
            result.setStatus("FAIL");
            result.setLogs(e.getMessage());
        }
        return result;
    }

    public TestCase saveTestCase(TestCase testCase) {
        testCase.setCreateTime(LocalDateTime.now());
        testCase.setUpdateTime(LocalDateTime.now());
        return testCaseRepository.save(testCase);
    }

    private void executeUIStep(TestStep step) {
        WebDriver driver = webDriverFactory.getDriver();
        switch (step.getType()) {
            case "click":
                driver.findElement(By.cssSelector(step.getSelector())).click();
                break;
            case "input":
                driver.findElement(By.cssSelector(step.getSelector())).sendKeys(step.getValue());
                break;
            default:
                throw new RuntimeException("Unsupported UI test step type: " + step.getType());
        }
    }

    private JMeterTestPlan createJMeterTestPlan(APIConfig config) {
        JMeterTestPlan testPlan = new JMeterTestPlan();
        
        // Create HTTP Sampler
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setDomain(extractDomain(config.getUrl()));
        httpSampler.setPath(extractPath(config.getUrl()));
        httpSampler.setMethod(config.getMethod());
        httpSampler.setPostBodyRaw(true);
        httpSampler.addNonEncodedArgument("", config.getBody(), "");
        
        // Create Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setNumThreads(config.getThreads());
        threadGroup.setRampUp(1);
        threadGroup.setName("Test Thread Group");
        
        testPlan.setHttpSampler(httpSampler);
        testPlan.setThreadGroup(threadGroup);
        
        return testPlan;
    }

    private String extractDomain(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + url);
        }
    }

    private String extractPath(String url) {
        try {
            return new URL(url).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + url);
        }
    }

    public List<TestResult> getAllTestResults() {
        return testResultRepository.findAll();
    }
} 