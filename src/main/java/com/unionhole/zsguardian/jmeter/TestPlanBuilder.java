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
package com.unionhole.zsguardian.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.testelement.TestPlan;

public class TestPlanBuilder {
    public TestPlan create() {
        TestPlan testPlan = new TestPlan("Test Plan");
        testPlan.setFunctionalMode(false);
        testPlan.setTearDownOnShutdown(false);
        testPlan.setUserDefinedVariables(new Arguments());
        return testPlan;
    }
} 