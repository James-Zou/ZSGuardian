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
package com.unionhole.zsguardian.model;

import lombok.Data;
import java.util.List;

@Data
public class GiteeWebhookPayload {
    private String ref;
    private String before;
    private String after;
    private List<Commit> commits;
    private Repository repository;
    private User pusher;

    @Data
    public static class Commit {
        private String id;
        private String message;
        private String timestamp;
        private String url;
        private Author author;
        private List<String> added;
        private List<String> removed;
        private List<String> modified;
    }

    @Data
    public static class Repository {
        private String name;
        private String fullName;
        private String description;
        private String url;
    }

    @Data
    public static class Author {
        private String name;
        private String email;
    }

    @Data
    public static class User {
        private String name;
        private String email;
    }
} 