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

@Entity
@Data
@Table(name = "execution_cases", indexes = {
    @Index(name = "idx_execution_plan", columnList = "execution_plan_id"),
    @Index(name = "idx_parent", columnList = "parent_id")
})
public class ExecutionCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_plan_id")
    private ExecutionPlan executionPlan;
    
    private String title;
    private String type;  // folder 或 case
    private String priority;
    private String precondition;
    private String steps;
    private String expectedResult;
    private String actualResult;
    private String result; // PASS, FAIL, BLOCKED
    private String bugDescription;
    private String status; // PENDING, COMPLETED
    private Integer sortOrder;

    @Column(name = "parent_id")
    private Long parentId;
}
