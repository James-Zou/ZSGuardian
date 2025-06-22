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
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

@Slf4j
@Service
public class XMindService {

    @Value("${file.upload.path}")
    private String uploadPath;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> parseXMindFile(MultipartFile file) {
        try {
            File tempFile = createTempFile(file);
            return parseXMindFile(tempFile);
        } catch (Exception e) {
            log.error("解析 XMind 文件失败", e);
            throw new RuntimeException("解析 XMind 文件失败: " + e.getMessage());
        }
    }

    public Map<String, Object> parseXMindFile(File file) {
        try {
            Map<String, Object> result = new HashMap<>();
            Set<String> impactAreas = new HashSet<>();
            Map<String, List<String>> testCasesByCategory = new HashMap<>();
            Set<String> allNodes = new HashSet<>();
            Set<String> secondLevelNodes = new HashSet<>();
            Set<String> leafNodes = new HashSet<>();
            int totalTopics = 0;

            try (ZipFile zipFile = new ZipFile(file)) {
                // 读取 content.xml
                ZipEntry contentEntry = zipFile.getEntry("content.xml");
                if (contentEntry == null) {
                    throw new RuntimeException("Invalid XMind file: content.xml not found");
                }

                // 解析 XML
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // 禁用外部实体解析，防止 XXE 攻击
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(zipFile.getInputStream(contentEntry));

                // 获取所有 topic 节点
                NodeList topics = doc.getElementsByTagName("topic");
                
                // 第一遍遍历：收集所有节点和层级信息
                for (int i = 0; i < topics.getLength(); i++) {
                    Element topic = (Element) topics.item(i);
                    String title = getTopicTitle(topic);
                    totalTopics++;
                    allNodes.add(title);

                    // 判断节点层级
                    int level = getTopicLevel(topic);
                    if (level == 2) {
                        secondLevelNodes.add(title);
                    }
                    
                    // 判断是否是叶子节点
                    if (!hasChildTopics(topic)) {
                        leafNodes.add(title);
                    }
                }

                // 处理影响范围：二级节点中的非叶子节点
                for (String node : secondLevelNodes) {
                    if (!leafNodes.contains(node)) {
                        impactAreas.add(node);
                    }
                }

                // 处理测试用例：所有不在影响范围内的节点
                for (String node : allNodes) {
                    if (!impactAreas.contains(node)) {
                        // 找到该节点的父节点（影响范围）
                        String parentArea = findParentArea(doc, node, impactAreas);
                        if (parentArea != null) {
                            testCasesByCategory.computeIfAbsent(parentArea, k -> new ArrayList<>())
                                             .add(node);
                        }
                    }
                }
            }

            // 构建返回结果
            result.put("success", true);
            result.put("impactAreas", new ArrayList<>(impactAreas));
            
            Map<String, Object> categories = new HashMap<>();
            categories.put("testCasesByCategory", testCasesByCategory);
            result.put("categories", categories);
            
            result.put("totalTopics", totalTopics);
            result.put("testCases", testCasesByCategory.values().stream()
                    .mapToInt(List::size)
                    .sum());

            // 清理临时文件
            file.delete();
            
            return result;

        } catch (Exception e) {
            log.error("解析 XMind 文件失败", e);
            throw new RuntimeException("解析 XMind 文件失败: " + e.getMessage());
        }
    }

    private String getTopicTitle(Element topic) {
        NodeList titleNodes = topic.getElementsByTagName("title");
        if (titleNodes.getLength() > 0) {
            return titleNodes.item(0).getTextContent().trim();
        }
        return "";
    }

    private int getTopicLevel(Element topic) {
        int level = 0;
        Node parent = topic.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            if ("topic".equals(parent.getNodeName())) {
                level++;
            }
            parent = parent.getParentNode();
        }
        return level;
    }

    private boolean hasChildTopics(Element topic) {
        NodeList children = topic.getElementsByTagName("topic");
        return children.getLength() > 0;
    }

    private String findParentArea(Document doc, String nodeName, Set<String> impactAreas) {
        NodeList topics = doc.getElementsByTagName("topic");
        for (int i = 0; i < topics.getLength(); i++) {
            Element topic = (Element) topics.item(i);
            String title = getTopicTitle(topic);
            if (impactAreas.contains(title)) {
                // 检查该影响范围节点的所有子节点
                if (isNodeUnderTopic(topic, nodeName)) {
                    return title;
                }
            }
        }
        return null;
    }

    private boolean isNodeUnderTopic(Element parentTopic, String nodeName) {
        NodeList children = parentTopic.getElementsByTagName("topic");
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if (getTopicTitle(child).equals(nodeName)) {
                return true;
            }
        }
        return false;
    }

    private File createTempFile(MultipartFile file) throws IOException {
        // 确保上传目录存在
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 创建临时文件
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".xmind";
        File tempFile = File.createTempFile("xmind_", extension, uploadDir);
        
        // 写入文件内容
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        
        return tempFile;
    }

    public String generateImprovedXMind(Map<String, Object> parseResult, Map<String, Object> aiSuggestions) {
        try {
            // 1. Create output directory if it doesn't exist
            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // 2. Create temp directory
            Path tempDir = Files.createTempDirectory("xmind_");
            log.info("Created temp directory: {}", tempDir);

            try {
                // 3. Create and write files with error logging
                try {
                    Map<String, Object> content = createXMindContent(parseResult, aiSuggestions);
                    Files.write(tempDir.resolve("content.json"), 
                               objectMapper.writeValueAsBytes(content));
                    log.info("Written content.json");
                } catch (Exception e) {
                    log.error("Failed to create content.json", e);
                    throw e;
                }

                try {
                    Map<String, Object> manifest = createXMindManifest();
                    Files.write(tempDir.resolve("manifest.json"), 
                               objectMapper.writeValueAsBytes(manifest));
                    log.info("Written manifest.json");
                } catch (Exception e) {
                    log.error("Failed to create manifest.json", e);
                    throw e;
                }

                try {
                    Map<String, Object> metadata = createXMindMetadata();
                    Files.write(tempDir.resolve("metadata.json"), 
                               objectMapper.writeValueAsBytes(metadata));
                    log.info("Written metadata.json");
                } catch (Exception e) {
                    log.error("Failed to create metadata.json", e);
                    throw e;
                }

                // 4. Create ZIP file
                String fileName = "improved_" + System.currentTimeMillis() + ".xmind";
                Path outputPath = outputDir.toPath().resolve(fileName);
                
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {
                    Files.walk(tempDir)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> addToZip(zos, path, tempDir));
                    log.info("Created ZIP file: {}", outputPath);
                }

                return fileName;
            } finally {
                // 5. Clean up temp directory
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                    log.info("Cleaned up temp directory");
                } catch (Exception e) {
                    log.error("Failed to clean up temp directory", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate XMind file", e);
            return null;
        }
    }

    private Map<String, Object> createXMindContent(Map<String, Object> parseResult, Map<String, Object> aiSuggestions) {
        Map<String, Object> content = new HashMap<>();
        // 根据 XMind 格式规范构建内容
        // 这里需要参考 XMind 的文件格式规范进行实现
        return content;
    }

    private Map<String, Object> createXMindManifest() {
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("file-entries", Arrays.asList(
            new HashMap<String, String>() {{ 
                put("full-path", "/");
                put("media-type", "application/vnd.xmind.workbook");
            }},
            new HashMap<String, String>() {{ 
                put("full-path", "content.json");
                put("media-type", "application/json");
            }},
            new HashMap<String, String>() {{ 
                put("full-path", "metadata.json");
                put("media-type", "application/json");
            }}
        ));
        return manifest;
    }

    private Map<String, Object> createXMindMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("creator", new HashMap<String, String>() {{ 
            put("name", "ZSGuardian");
            put("version", "1.0.0");
        }});
        metadata.put("created", System.currentTimeMillis());
        return metadata;
    }

    private void addToZip(ZipOutputStream zos, Path file, Path sourceDir) {
        try {
            String relativePath = sourceDir.relativize(file).toString();
            ZipEntry entry = new ZipEntry(relativePath);
            zos.putNextEntry(entry);
            Files.copy(file, zos);
            zos.closeEntry();
        } catch (IOException e) {
            log.error("添加文件到 ZIP 失败", e);
        }
    }

    public boolean validateXMindFile(String fileName) {
        try {
            Path filePath = Paths.get("output", fileName);
            try (ZipFile zipFile = new ZipFile(filePath.toFile())) {
                // 检查必要的文件是否存在
                return zipFile.getEntry("content.json") != null &&
                       zipFile.getEntry("manifest.json") != null &&
                       zipFile.getEntry("metadata.json") != null;
            }
        } catch (Exception e) {
            log.error("验证 XMind 文件失败", e);
            return false;
        }
    }

    /**
     * 处理分片上传的XMind文件
     * @param chunk 当前分片文件
     * @param chunkNumber 当前分片序号
     * @param totalChunks 总分片数
     * @param originalFilename 原始文件名
     * @return 如果是最后一片则解析并返回结果，否则返回空Map
     */
    public Map<String, Object> handleChunkUpload(MultipartFile chunk, int chunkNumber, int totalChunks, String originalFilename) {
        try {
            // 创建临时目录用于存储分片
            String tempDirPath = uploadPath + File.separator + "chunks_" + originalFilename;
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // 保存当前分片
            File chunkFile = new File(tempDir, "chunk_" + chunkNumber);
            chunk.transferTo(chunkFile);

            // 如果不是最后一片，直接返回
            if (chunkNumber < totalChunks - 1) {
                return new HashMap<>();
            }

            // 是最后一片，开始合并文件
            File mergedFile = new File(uploadPath, originalFilename);
            try (FileOutputStream fos = new FileOutputStream(mergedFile)) {
                for (int i = 0; i < totalChunks; i++) {
                    File currentChunk = new File(tempDir, "chunk_" + i);
                    Files.copy(currentChunk.toPath(), fos);
                }
            }

            // 清理分片文件夹
            FileUtils.deleteDirectory(tempDir);

            // 解析合并后的文件
            return parseXMindFile(mergedFile);

        } catch (Exception e) {
            log.error("处理分片上传失败", e);
            throw new RuntimeException("处理分片上传失败: " + e.getMessage());
        }
    }
} 