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

import com.unionhole.zsguardian.dto.ChangeReviewDTO;
import com.unionhole.zsguardian.service.ChangeReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/change-reviews")
public class ChangeReviewController {
    @Autowired
    private ChangeReviewService service;

    @GetMapping
    public Page<ChangeReviewDTO> list(@RequestParam int page, @RequestParam int size) {
        return service.list(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/{id}")
    public ChangeReviewDTO get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ChangeReviewDTO create(@RequestBody ChangeReviewDTO dto) {
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ChangeReviewDTO update(@PathVariable Long id, @RequestBody ChangeReviewDTO dto) {
        dto.setId(id);
        return service.save(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get("uploads", fileName);
        try{
            Files.copy(file.getInputStream(), path);
        }catch (Exception e){
            log.error("upload error is {}",e);
        }
        return Collections.singletonMap("url", "/uploads/" + fileName);
    }
}