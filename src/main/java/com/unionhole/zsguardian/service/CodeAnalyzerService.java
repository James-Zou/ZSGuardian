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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@Slf4j
public class CodeAnalyzerService {
    
    // 命名规范相关正则
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?:public|private|protected)?\\s+\\w+\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(");
    
    // 安全性检查相关正则
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|secret|key)\\s*=\\s*[\"'][^\"']+[\"']");
    
    // 代码质量检查相关正则
    private static final Pattern SYSOUT_PATTERN = Pattern.compile("System\\.out\\.println");
    private static final Pattern EMPTY_CATCH_PATTERN = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}");
    private static final Pattern MAGIC_NUMBER_PATTERN = Pattern.compile("(?<!\\.)\\b\\d{4,}\\b(?!\\.\\d)");
    private static final Pattern METHOD_BODY_PATTERN = Pattern.compile("\\{([^{}]*+(?:\\{[^{}]*}[^{}]*)*+)\\}");
    
    // 性能检查相关正则
    private static final Pattern LOOP_NEW_PATTERN = Pattern.compile("for\\s*\\([^)]+\\)\\s*\\{[^}]*new\\s+[A-Z][a-zA-Z0-9]*");
    private static final Pattern STRING_CONCAT_PATTERN = Pattern.compile("String\\s+\\w+\\s*\\+\\=");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("new\\s+(FileInputStream|FileOutputStream|Connection|Statement|ResultSet)");
    
    // 异常和风险检查相关正则
    private static final Pattern NULL_CHECK_PATTERN = Pattern.compile("(?:\\w+\\.\\w+\\(.*\\)|\\w+\\.\\w+)\\s*(?!\\.equals|!=|==|\\s*\\?|\\s*==\\s*null|\\s*!=\\s*null)");
    private static final Pattern COLLECTION_PATTERN = Pattern.compile("new\\s+(ArrayList|HashMap|HashSet)\\s*<[^>]*>\\s*\\([^)]*\\)");
    private static final Pattern CACHE_PATTERN = Pattern.compile("@Cacheable|Cache<|LoadingCache");
    private static final Pattern SQL_CONCAT_PATTERN = Pattern.compile("\"\\s*\\+\\s*\\w+\\s*\\+\\s*\".*(?i)(?:select|update|delete|insert)");
    private static final Pattern BATCH_SQL_PATTERN = Pattern.compile("executeBatch|addBatch");
    private static final Pattern THREAD_PATTERN = Pattern.compile("ArrayList|HashMap|HashSet");
    private static final Pattern FILE_PATTERN = Pattern.compile("new\\s+File\\(|FileInputStream|FileOutputStream");
    private static final Pattern DB_PATTERN = Pattern.compile("getConnection\\(|createStatement\\(|prepareStatement\\(");
    
    // 生产安全检查相关正则
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile("@Transactional(?!\\([^)]*timeout\\s*=)");
    private static final Pattern PROPAGATION_PATTERN = Pattern.compile("@Transactional(?!\\([^)]*propagation\\s*=)");
    private static final Pattern LOCK_PATTERN = Pattern.compile("synchronized|Lock\\s+\\w+|@Lock");
    private static final Pattern THREAD_POOL_PATTERN = Pattern.compile("new\\s+ThreadPoolExecutor\\(");
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile("(?i)(password|idcard|phone|email|address)\\s*=");
    private static final Pattern TABLE_PATTERN = Pattern.compile("from\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s+where");
    private static final Pattern RPC_PATTERN = Pattern.compile("@FeignClient|RestTemplate|HttpClient");
    private static final Pattern CATCH_PATTERN = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{[^}]*\\}");

    @Data
    @AllArgsConstructor
    public static class CodeIssue {
        private String type;        // 问题类型：SYNTAX/SQL/SECURITY/STYLE
        private String level;       // 严重程度：CRITICAL|MAJOR|MINOR|INFO
        private String message;     // 问题描述
        private String suggestion;  // 修复建议
        private Integer line;       // 问题所在行
    }

    public List<Map<String, String>> analyzeCode(String code, String fileType) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 根据文件类型进行不同的检查
        switch (fileType.toLowerCase()) {
            case "java":
                issues.addAll(checkJavaCode(code));
                break;
            case "sql":
                issues.addAll(checkSqlCode(code));
                break;
            case "js":
            case "jsx":
            case "ts":
            case "tsx":
                issues.addAll(checkJavaScriptCode(code));
                break;
        }
        
        // 将 CodeIssue 转换为 Map 格式
        return issues.stream().map(issue -> {
            Map<String, String> result = new HashMap<>();
            result.put("type", issue.getType());
            result.put("level", issue.getLevel());
            result.put("message", issue.getMessage());
            result.put("suggestion", issue.getSuggestion());
            result.put("line", String.valueOf(issue.getLine()));
            return result;
        }).collect(Collectors.toList());
    }

    private List<CodeIssue> checkJavaCode(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 1. 命名规范检查
        // 1.1 类名检查
        Matcher classMatcher = CLASS_PATTERN.matcher(code);
        while (classMatcher.find()) {
            String className = classMatcher.group(1);
            if (!className.matches("^[A-Z][a-zA-Z0-9]*$")) {
                issues.add(new CodeIssue(
                    "STYLE", "MEDIUM",
                    "类名 '" + className + "' 不符合驼峰命名规范",
                    "类名应该以大写字母开头，遵循驼峰命名规则",
                    getLineNumber(code, classMatcher.start())
                ));
            }
        }

        // 1.2 方法名检查
        Matcher methodMatcher = METHOD_PATTERN.matcher(code);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            if (!methodName.matches("^[a-z][a-zA-Z0-9]*$")) {
                issues.add(new CodeIssue(
                    "STYLE", "MEDIUM",
                    "方法名 '" + methodName + "' 不符合驼峰命名规范",
                    "方法名应该以小写字母开头，遵循驼峰命名规则",
                    getLineNumber(code, methodMatcher.start())
                ));
            }
        }

        // 2. 安全性检查
        // 2.1 硬编码的IP地址
        Matcher ipMatcher = IP_PATTERN.matcher(code);
        while (ipMatcher.find()) {
            issues.add(new CodeIssue(
                "SECURITY", "HIGH",
                "发现硬编码的IP地址",
                "建议将IP地址配置在配置文件中",
                getLineNumber(code, ipMatcher.start())
            ));
        }

        // 2.2 检查硬编码的密码或密钥
        Matcher passwordMatcher = PASSWORD_PATTERN.matcher(code);
        while (passwordMatcher.find()) {
            issues.add(new CodeIssue(
                "SECURITY", "CRITICAL",
                "发现硬编码的敏感信息",
                "建议使用配置文件或环境变量存储敏感信息",
                getLineNumber(code, passwordMatcher.start())
            ));
        }

        // 3. 代码质量检查
        // 3.1 System.out.println 使用检查
        Matcher sysoutMatcher = SYSOUT_PATTERN.matcher(code);
        while (sysoutMatcher.find()) {
            issues.add(new CodeIssue(
                "STYLE", "LOW",
                "使用了System.out.println",
                "建议使用日志框架替代System.out.println",
                getLineNumber(code, sysoutMatcher.start())
            ));
        }

        // 3.2 检查是否有空的catch块
        Matcher emptyCatchMatcher = EMPTY_CATCH_PATTERN.matcher(code);
        while (emptyCatchMatcher.find()) {
            issues.add(new CodeIssue(
                "STYLE", "HIGH",
                "发现空的catch块",
                "建议至少记录异常信息或者添加必要的处理逻辑",
                getLineNumber(code, emptyCatchMatcher.start())
            ));
        }

        // 3.3 检查魔法数字
        Matcher magicNumberMatcher = MAGIC_NUMBER_PATTERN.matcher(code);
        while (magicNumberMatcher.find()) {
            issues.add(new CodeIssue(
                "STYLE", "LOW",
                "发现魔法数字",
                "建议将数字定义为常量，提高代码可维护性",
                getLineNumber(code, magicNumberMatcher.start())
            ));
        }

        // 3.4 检查过长的方法
        Matcher methodBodyMatcher = METHOD_BODY_PATTERN.matcher(code);
        while (methodBodyMatcher.find()) {
            String methodBody = methodBodyMatcher.group(1);
            int lineCount = methodBody.split("\n").length;
            if (lineCount > 50) {
                issues.add(new CodeIssue(
                    "STYLE", "MEDIUM",
                    "方法过长（" + lineCount + "行）",
                    "建议将大型方法拆分为更小的方法，每个方法不超过50行",
                    getLineNumber(code, methodBodyMatcher.start())
                ));
            }
        }

        // 4. 性能检查
        // 4.1 检查是否在循环中创建对象
        Matcher loopNewMatcher = LOOP_NEW_PATTERN.matcher(code);
        while (loopNewMatcher.find()) {
            issues.add(new CodeIssue(
                "PERFORMANCE", "MEDIUM",
                "在循环中创建对象",
                "建议将对象创建移到循环外部，避免频繁创建对象",
                getLineNumber(code, loopNewMatcher.start())
            ));
        }

        // 4.2 检查是否使用StringBuffer/StringBuilder
        Matcher stringConcatMatcher = STRING_CONCAT_PATTERN.matcher(code);
        while (stringConcatMatcher.find()) {
            issues.add(new CodeIssue(
                "PERFORMANCE", "LOW",
                "使用String累加",
                "建议使用StringBuilder或StringBuffer进行字符串拼接",
                getLineNumber(code, stringConcatMatcher.start())
            ));
        }

        // 4.3 检查是否有未关闭的资源
        Matcher resourceMatcher = RESOURCE_PATTERN.matcher(code);
        while (resourceMatcher.find()) {
            if (!code.contains("try-with-resources") && !code.contains(".close()")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "可能存在资源未关闭的风险",
                    "建议使用try-with-resources语句或在finally块中确保资源关闭",
                    getLineNumber(code, resourceMatcher.start())
                ));
            }
        }

        // 5. 异常和风险检查
        // 5.1 空指针风险检查
        Matcher nullCheckMatcher = NULL_CHECK_PATTERN.matcher(code);
        while (nullCheckMatcher.find()) {
            String expression = nullCheckMatcher.group();
            if (!code.contains("@NonNull") && !expression.startsWith("this.")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "可能存在空指针风险: " + expression,
                    "建议添加null检查或使用Optional包装",
                    getLineNumber(code, nullCheckMatcher.start())
                ));
            }
        }

        // 5.2 内存泄漏风险检查
        // 5.2.1 检查大集合的使用
        Matcher collectionMatcher = COLLECTION_PATTERN.matcher(code);
        while (collectionMatcher.find()) {
            if (code.contains("while") || code.contains("for")) {
                issues.add(new CodeIssue(
                    "PERFORMANCE", "HIGH",
                    "在循环中使用大集合可能导致OOM",
                    "建议使用分页或流式处理大数据集",
                    getLineNumber(code, collectionMatcher.start())
                ));
            }
        }

        // 5.2.2 检查是否有大对象的缓存
        Matcher cacheMatcher = CACHE_PATTERN.matcher(code);
        while (cacheMatcher.find()) {
            issues.add(new CodeIssue(
                "PERFORMANCE", "MEDIUM",
                "使用缓存需要注意内存占用",
                "建议设置缓存大小限制和过期策略",
                getLineNumber(code, cacheMatcher.start())
            ));
        }

        // 5.3 SQL异常风险检查
        // 5.3.1 检查SQL拼接
        Matcher sqlConcatMatcher = SQL_CONCAT_PATTERN.matcher(code);
        while (sqlConcatMatcher.find()) {
            issues.add(new CodeIssue(
                "SECURITY", "CRITICAL",
                "发现SQL注入风险：使用字符串拼接构建SQL",
                "建议使用预编译语句和参数绑定",
                getLineNumber(code, sqlConcatMatcher.start())
            ));
        }

        // 5.3.2 检查大批量SQL操作
        Matcher batchSqlMatcher = BATCH_SQL_PATTERN.matcher(code);
        while (batchSqlMatcher.find()) {
            issues.add(new CodeIssue(
                "PERFORMANCE", "MEDIUM",
                "检测到批量SQL操作",
                "建议控制批次大小，避免单次处理数据过多",
                getLineNumber(code, batchSqlMatcher.start())
            ));
        }

        // 5.4 线程安全检查
        // 5.4.1 检查线程安全的集合使用
        Matcher threadMatcher = THREAD_PATTERN.matcher(code);
        if (threadMatcher.find() && (code.contains("@Async") || code.contains("new Thread") || code.contains("ExecutorService"))) {
            issues.add(new CodeIssue(
                "SECURITY", "HIGH",
                "在多线程环境中使用非线程安全的集合",
                "建议使用ConcurrentHashMap、CopyOnWriteArrayList等线程安全集合",
                getLineNumber(code, threadMatcher.start())
            ));
        }

        // 5.5 资源耗尽检查
        // 5.5.1 检查文件操作
        Matcher fileMatcher = FILE_PATTERN.matcher(code);
        while (fileMatcher.find()) {
            if (!code.contains("try-with-resources")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "文件资源可能未正确关闭",
                    "建议使用try-with-resources语句确保资源正确关闭",
                    getLineNumber(code, fileMatcher.start())
                ));
            }
        }

        // 5.5.2 检查数据库连接
        Matcher dbMatcher = DB_PATTERN.matcher(code);
        while (dbMatcher.find()) {
            if (!code.contains("try-with-resources")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "数据库连接可能未正确关闭",
                    "建议使用try-with-resources语句或在finally中关闭连接",
                    getLineNumber(code, dbMatcher.start())
                ));
            }
        }

        // 5.6 递归调用检查
        if (code.contains("递归") || containsRecursion(code)) {
            issues.add(new CodeIssue(
                "PERFORMANCE", "MEDIUM",
                "检测到递归调用",
                "建议添加递归深度限制，防止栈溢出",
                1
            ));
        }

        // 6. 生产安全检查
        // 6.1 事务处理检查
        // 6.1.1 检查事务超时设置
        Matcher transactionMatcher = TRANSACTION_PATTERN.matcher(code);
        while (transactionMatcher.find()) {
            issues.add(new CodeIssue(
                "SECURITY", "HIGH",
                "事务未设置超时时间",
                "建议设置事务超时时间，避免长事务占用数据库连接",
                getLineNumber(code, transactionMatcher.start())
            ));
        }

        // 6.1.2 检查事务传播行为
        Matcher propagationMatcher = PROPAGATION_PATTERN.matcher(code);
        while (propagationMatcher.find()) {
            issues.add(new CodeIssue(
                "SECURITY", "MEDIUM",
                "未明确指定事务传播行为",
                "建议明确指定事务传播行为，避免事务混乱",
                getLineNumber(code, propagationMatcher.start())
            ));
        }

        // 6.2 并发处理检查
        // 6.2.1 检查分布式锁使用
        Matcher lockMatcher = LOCK_PATTERN.matcher(code);
        if (lockMatcher.find() && code.contains("@Service")) {
            issues.add(new CodeIssue(
                "SECURITY", "HIGH",
                "分布式环境下使用本地锁",
                "建议使用分布式锁（如Redisson）代替本地锁",
                getLineNumber(code, lockMatcher.start())
            ));
        }

        // 6.2.2 检查线程池配置
        Matcher threadPoolMatcher = THREAD_POOL_PATTERN.matcher(code);
        while (threadPoolMatcher.find()) {
            if (!code.contains("setRejectedExecutionHandler")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "线程池未配置拒绝策略",
                    "建议配置合适的拒绝策略，避免任务堆积导致OOM",
                    getLineNumber(code, threadPoolMatcher.start())
                ));
            }
        }

        // 6.3 缓存使用检查
        // 6.3.1 检查缓存穿透防护
        Matcher lodingCacheMatcher = CACHE_PATTERN.matcher(code);
        while (cacheMatcher.find()) {
            if (!code.contains("null") && !code.contains("Optional")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "缓存可能存在穿透风险",
                    "建议对空值进行缓存或使用布隆过滤器",
                    getLineNumber(code, lodingCacheMatcher.start())
                ));
            }
        }

        // 6.4 异常处理检查
        // 6.4.1 检查异常吞噬
        Matcher catchMatcher = CATCH_PATTERN.matcher(code);
        while (catchMatcher.find()) {
            String catchBlock = catchMatcher.group();
            if (!catchBlock.contains("log") && !catchBlock.contains("throw")) {
                issues.add(new CodeIssue(
                    "SECURITY", "CRITICAL",
                    "异常被吞噬",
                    "建议至少记录异常日志或抛出自定义异常",
                    getLineNumber(code, catchMatcher.start())
                ));
            }
        }

        // 6.5 敏感操作检查
        // 6.5.1 检查敏感数据处理
        Matcher sensitiveMatcher = SENSITIVE_PATTERN.matcher(code);
        while (sensitiveMatcher.find()) {
            if (!code.contains("encrypt") && !code.contains("mask")) {
                issues.add(new CodeIssue(
                    "SECURITY", "CRITICAL",
                    "敏感信息未加密或脱敏",
                    "建议对敏感信息进行加密或脱敏处理",
                    getLineNumber(code, sensitiveMatcher.start())
                ));
            }
        }

        // 6.6 数据库操作检查
        // 6.6.1 检查大表操作
        Matcher tableMatcher = TABLE_PATTERN.matcher(code.toLowerCase());
        while (tableMatcher.find()) {
            if (!code.contains("limit") && !code.contains("分页")) {
                issues.add(new CodeIssue(
                    "PERFORMANCE", "HIGH",
                    "可能存在全表扫描风险",
                    "建议添加分页或限制查询范围",
                    getLineNumber(code, tableMatcher.start())
                ));
            }
        }

        // 6.7 定时任务检查
        // 6.7.1 检查定时任务超时处理
        if (code.contains("@Scheduled") || code.contains("@EnableScheduling")) {
            if (!code.contains("try") || !code.contains("catch")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "定时任务缺少异常处理",
                    "建议添加异常处理和任务超时控制",
                    1
                ));
            }
        }

        // 6.8 远程调用检查
        // 6.8.1 检查超时和重试配置
        Matcher rpcMatcher = RPC_PATTERN.matcher(code);
        while (rpcMatcher.find()) {
            if (!code.contains("timeout") && !code.contains("retry")) {
                issues.add(new CodeIssue(
                    "SECURITY", "HIGH",
                    "远程调用缺少超时和重试配置",
                    "建议设置合理的超时时间和重试策略",
                    getLineNumber(code, rpcMatcher.start())
                ));
            }
        }

        return issues;
    }

    private boolean containsRecursion(String code) {
        // 简单的递归检测：方法内调用自身
        Matcher methodMatcher = METHOD_PATTERN.matcher(code);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            if (code.contains(methodName + "(")) {
                return true;
            }
        }
        return false;
    }

    private List<CodeIssue> checkSqlCode(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 检查SQL注入风险
        if (code.toLowerCase().contains("select *")) {
            issues.add(new CodeIssue(
                "SQL",
                "MEDIUM",
                "使用了SELECT *",
                "建议明确指定查询的列名",
                1
            ));
        }

        // 检查是否有DELETE/UPDATE语句没有WHERE条件
        Matcher matcher = Pattern.compile("(?i)(delete|update)\\s+\\w+\\s*(?!where)").matcher(code);
        if (matcher.find()) {
            issues.add(new CodeIssue(
                "SQL",
                "CRITICAL",
                "DELETE/UPDATE语句缺少WHERE条件",
                "建议添加WHERE条件以避免全表更新/删除",
                getLineNumber(code, matcher.start())
            ));
        }

        return issues;
    }

    private List<CodeIssue> checkJavaScriptCode(String code) {
        List<CodeIssue> issues = new ArrayList<>();
        
        // 检查是否使用var而不是let/const
        Matcher varMatcher = Pattern.compile("\\bvar\\s+").matcher(code);
        while (varMatcher.find()) {
            issues.add(new CodeIssue(
                "STYLE",
                "LOW",
                "使用了var关键字",
                "建议使用let或const替代var",
                getLineNumber(code, varMatcher.start())
            ));
        }

        return issues;
    }

    private int getLineNumber(String code, int position) {
        return code.substring(0, position).split("\n").length;
    }
} 