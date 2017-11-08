/*
 * Copyright (c) 2015-2017, Dell EMC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.emc.metalnx.integration.test.mygroups;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.emc.metalnx.core.domain.exceptions.DataGridException;
import com.emc.metalnx.integration.test.utils.UserUtils;
import com.emc.metalnx.test.generic.UITest;

import junit.framework.Assert;

/**
 * Test class that checks if a user who is not in any group can access the My
 * Groups page.
 *
 */
@Deprecated
@Ignore
public class RodsAdminNotInAnyGroupTest {

	private static String pwd = "webdriver";
	private static String rodsAdminUsername;
	private static WebDriver driver = null;
	private By groupBookmarksTable = By.id("groupBookmarksTable");
	private By groupBookmarksTableBody = By.cssSelector("#groupBookmarksTable tbody tr td");

	/*************************************
	 * TEST SET UP
	 *************************************/

	@BeforeClass
	public static void setUpBeforeClass() throws DataGridException {
		// UITest.setUpBeforeClass();
		driver = UITest.getDriver();

		rodsAdminUsername = "userMyGroupsRodsAdmin" + System.currentTimeMillis();
		UserUtils.createUser(rodsAdminUsername, pwd, UITest.RODS_USER_TYPE, driver);
	}

	@Before
	public void setUp() {
		UITest.login(rodsAdminUsername, pwd);
	}

	@After
	public void tearDown() {
		UITest.logout();
	}

	@AfterClass
	public static void tearDownAfterClass() throws DataGridException {
		UserUtils.removeUser(rodsAdminUsername, driver);

		if (driver != null) {
			driver.quit();
			driver = null;
			UITest.setDriver(null);
		}
	}

	@Test
	public void testRodsUserNotInAnyGroupShouldSeeNothingOnMyGroupsPage() {
		driver.get(UITest.MY_GROUPS_PAGE);
		new WebDriverWait(driver, 10).until(ExpectedConditions.presenceOfElementLocated(groupBookmarksTable));
		new WebDriverWait(driver, 10).until(ExpectedConditions.visibilityOfElementLocated(groupBookmarksTableBody));

		Assert.assertNotNull(driver.findElement(By.cssSelector("#groupBookmarksTable .dataTables_empty")));
	}
}
