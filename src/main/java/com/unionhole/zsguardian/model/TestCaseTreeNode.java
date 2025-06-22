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

import com.unionhole.zsguardian.entity.TestCase;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestCaseTreeNode {
    private Long id;
    private String title;
    private String type;
    private String priority;
    private String precondition;
    private String steps;
    private String expectedResult;
    private String description;
    private List<TestCaseTreeNode> children;

    public TestCaseTreeNode(TestCase testCase) {
        this.id = testCase.getId();
        this.title = testCase.getTitle();
        this.type = testCase.getType();
        this.priority = testCase.getPriority();
        this.children = new ArrayList<>();
    }
}
