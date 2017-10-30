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

package com.emc.metalnx.integration.test.collection;

import com.emc.metalnx.core.domain.exceptions.DataGridException;
import com.emc.metalnx.integration.test.utils.CollectionUtils;
import com.emc.metalnx.integration.test.utils.FileUtils;
import com.emc.metalnx.integration.test.utils.TemplateUtils;
import com.emc.metalnx.integration.test.utils.UserUtils;
import com.emc.metalnx.test.generic.UITest;
import junit.framework.Assert;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Random;

@Deprecated
@Ignore
public class BreadcrumbTest {

    private static WebDriver driver = null;
    private static WebDriverWait wait = null;

    private static By breadcrumbLocator = By.className("breadcrumb");
    private static By navigationInputLocator = By.id("navigationInput");
    private static String zoneAndHomePath = "/" + UITest.IRODS_ZONE + "/home";

    @BeforeClass
    public static void setUpBeforeClass() {
        UITest.setUpBeforeClass();
        driver = UITest.getDriver();
        wait = new WebDriverWait(driver, 5);
    }

    @Before
    public void setUp() {
        UITest.login();

    }

    /**
     * After each test the user created for the test should be removed.
     */
    @After
    public void tearDown() {
        UITest.logout();
    }

    /**
     * After all tests are done, the test must quit the driver. This will close every window
     * associated with the current driver instance.
     */

    @AfterClass
    public static void tearDownAfterClass() {
        if (driver != null) {
            driver.quit();
            driver = null;
            UITest.setDriver(null);
        }
    }

    /**
     * Check if rodsadmin can go to /zone/home
     */
    @Test
    public void testGoToHomeCollection() {
        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, zoneAndHomePath);

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));

        String path = driver.findElement(navigationInputLocator).getAttribute("value");

        Assert.assertEquals(zoneAndHomePath, path);
    }

    /**
     * Check if user gets a message warning the user that the path entered doesn't exist
     */
    @Test
    public void testNonExistentCollection() {
        String randomString = RandomStringUtils.randomAlphanumeric(20);

        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, randomString);

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));

        Assert.assertEquals("The provided path \"" + randomString + "\" is not a valid one.",
                driver.findElement(By.id("invalidPathErrorMsg")).getText());
    }

    /**
     * when user tries to send an empty path input
     */
    @Test
    public void testBlankPath() {
        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, Keys.BACK_SPACE.toString());

        Assert.assertTrue(driver.findElement(navigationInputLocator).isDisplayed());
    }

    /**
     * Try to access a path that the current user doesn't have access to
     */
    @Test
    public void testPathWithoutPermission() {
        String uname = "breadcrumbPermission" + System.currentTimeMillis();
        String pwd = "webdriver";

        // creating a new user
        driver.get(UITest.ADD_USERS_URL);
        UserUtils.fillInUserGeneralInformation(uname, pwd, UITest.RODS_USER_TYPE, driver);
        UserUtils.submitUserForm(driver);

        String newUserCollection = zoneAndHomePath + "/" + uname;
        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, newUserCollection);

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));

        Assert.assertEquals("You do not have permissions to access information in the target collection or for the selected object.",
                driver.findElement(By.cssSelector("#tree-view-panel-body > .col-xs-12 > .text-center > span")).getText());

        // removing user
        driver.get(UITest.USERS_URL);

        By removeButton = By.id("btn_remove_" + uname);
        By removeConfirmationButton = By.id("btnConfUserRemoval_Yes");

        UserUtils.searchUser(driver, uname);

        wait.until(ExpectedConditions.elementToBeClickable(removeButton));

        driver.findElement(removeButton).click();
        wait.until(ExpectedConditions.elementToBeClickable(removeConfirmationButton));
        driver.findElement(removeConfirmationButton).click();
    }

    /**
     * Uploads a file to the rods user and tries to access the file via breadcrumb
     *
     * @throws DataGridException
     *
     */
    @Test
    public void testFilePath() throws DataGridException {
        // Upload test files
        FileUtils.uploadToHomeDirAsAdmin(TemplateUtils.TEST_FILES[0]);

        String filePath = zoneAndHomePath + "/" + UITest.RODS_USERNAME + "/" + TemplateUtils.TEST_FILES[0];

        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, filePath);

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));
        String path = driver.findElement(navigationInputLocator).getAttribute("value");

        Assert.assertEquals(filePath, path);
    }

    /**
     * Creates a collection and tries to access it via breadcrumb
     */
    @Test
    public void testCollectionPath() {
        String collectionName = "breadcrumbCollectionTest" + System.currentTimeMillis();
        CollectionUtils.createCollection(driver, collectionName);

        String collectionPath = zoneAndHomePath + "/" + UITest.RODS_USERNAME + "/" + collectionName;

        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, collectionPath);

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));

        String path = driver.findElement(navigationInputLocator).getAttribute("value");

        Assert.assertEquals(collectionPath, path);

        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, zoneAndHomePath + "/" + UITest.RODS_USERNAME);
        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));
        CollectionUtils.removeColl(driver, collectionName);
    }

    /**
     * Adds "/" at the end of an existent path
     */
    @Test
    public void testSlashAtTheEndOfPath() {
        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, zoneAndHomePath + "/");

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));

        String path = driver.findElement(navigationInputLocator).getAttribute("value");

        Assert.assertEquals(zoneAndHomePath, path);
    }

    /**
     * Adds multiples "/" at the end of an existent path
     */
    @Test
    public void testMultipleSlashesPath() {
        Random random = new Random();
        String zoneAndHomePathPlusSlashes = zoneAndHomePath;
        for (int i = 0; i < random.nextInt(20); i++) {
            zoneAndHomePathPlusSlashes += "/";
        }

        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, zoneAndHomePathPlusSlashes);

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));

        String path = driver.findElement(navigationInputLocator).getAttribute("value");

        Assert.assertEquals(zoneAndHomePath, path);
    }

    /**
     * Adds "\" at the end of an existent path
     */
    @Test
    public void testInvertedSlash() {
        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, zoneAndHomePath + "\\");

        wait.until(ExpectedConditions.elementToBeClickable(breadcrumbLocator));

        Assert.assertEquals("The provided path \"" + zoneAndHomePath + "\\\" is not a valid one.",
                driver.findElement(By.id("invalidPathErrorMsg")).getText());
    }

}
