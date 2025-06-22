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

import com.unionhole.zsguardian.service.DeepSeekService;
import com.unionhole.zsguardian.service.XMindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api")
public class AnalyzerController {

    @Autowired
    private XMindService xmindService;

    @Autowired
    private DeepSeekService aiService;

    @Value("${file.upload.path}")
    private String uploadPath;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFile( @RequestParam("file") MultipartFile chunk,
                                          @RequestParam("chunkNumber") int chunkNumber,
                                          @RequestParam("totalChunks") int totalChunks,
                                          @RequestParam("filename") String filename,
                                          HttpServletRequest request) {
        try {

            Map<String, Object> result = xmindService.handleChunkUpload(
                    chunk, chunkNumber, totalChunks, filename);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("分析文件失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "分析文件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/ai-correct")
    public ResponseEntity<?> aiCorrect(@RequestBody Map<String, Object> parseResult,
                                       HttpServletRequest request) {
        try {
            String username = (String) request.getSession().getAttribute("username");
            if (username == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "请先登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> result = aiService.correctAnalysis(parseResult);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("AI 校正失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "AI 校正失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("output", fileName);
            Resource resource = new FileSystemResource(filePath.toFile());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .body(resource);
                
        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
} 