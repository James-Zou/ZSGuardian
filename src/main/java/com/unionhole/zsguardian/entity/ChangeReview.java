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
package com.unionhole.zsguardian.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "change_review")
public class ChangeReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private LocalDateTime changeTime;
    private String changeType;
    private String description;
    private String documentUrl;
    private Long executorId;
    private Long auditorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @ManyToMany
    @JoinTable(
        name = "change_review_requirement",
        joinColumns = @JoinColumn(name = "change_review_id"),
        inverseJoinColumns = @JoinColumn(name = "requirement_id")
    )
    private List<Requirement> requirements = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "change_review_bug",
        joinColumns = @JoinColumn(name = "change_review_id"),
        inverseJoinColumns = @JoinColumn(name = "bug_id")
    )
    private List<Bug> bugs = new ArrayList<>();
    // getter/setter
}