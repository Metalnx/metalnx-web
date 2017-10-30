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

package com.emc.metalnx.integration.test.scenarios;

import com.emc.metalnx.core.domain.exceptions.DataGridException;
import com.emc.metalnx.integration.test.group.GroupUtils;
import com.emc.metalnx.integration.test.utils.*;
import com.emc.metalnx.test.generic.UITest;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Test class that represents the creation of a profile scenario.
 *
 */
@Deprecated
@Ignore
public class ApplyProfileTest {

    private static String pwd = "webdriver";
    private static String adminUName = "webdriver_admin";
    private static String uname = "webdriver";
    private static String profileName = "profiletestscenario";
    private static String groupName = "grouptestscenario";
    private static String collName = "testscenariocoll";
    private static String collPath = null;
    private static WebDriverWait wait = null;
    private static WebDriver driver = null;

    /************************************* TEST SET UP *************************************/

    @BeforeClass
    public static void setUpBeforeClass() throws DataGridException {
        UITest.setUpBeforeClass();
        driver = UITest.getDriver();
        wait = new WebDriverWait(driver, 10);
    }

    @Before
    public void setUp() {
        long currTime = System.currentTimeMillis();
        pwd = "webdriver";
        adminUName = "webdriver_admin" + currTime;
        uname = "webdriver" + currTime;
        profileName = "profiletestscenario" + currTime;
        groupName = "grouptestscenario" + currTime;
        collName = "testscenariocoll" + currTime;
        collPath = String.format("/%s/%s", UITest.IRODS_ZONE, collName);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    @AfterClass
    public static void tearDownAfterClass() throws DataGridException {
        if (driver != null) {
            driver.quit();
            driver = null;
            UITest.setDriver(null);
        }
    }

    /*
     * ********************************************************************************************
     * ******************************** SCENARIOS *************************************************
     * ********************************************************************************************
     */

    /**
     * Test scenario:
     * As rods, create a collection
     * As rods, create a group, give ownership of the collection above to this group.
     * As different admin, create a profile and include the group created above in this profile.
     * As different admin, apply this profile when creating a user.
     * Check if the profile was applied and all groups the user is attached to
     * Verify by direct entry on breadcrumb
     * Verify that upload, file deletion for this user works in the collection above.
     *
     * @throws IOException
     * @throws MalformedURLException
     * @throws FailingHttpStatusCodeException
     */
    @Test
    public void testApplyProfileWhenAddingAUser() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        UITest.login();
        CollectionUtils.createCollectionUnderZone(driver, collName, UITest.IRODS_ZONE);
        GroupUtils.createGroupWithPermissions(driver, groupName, UITest.OWN_PERMISSION, collName);
        UITest.logout();

        UserUtils.createUser(adminUName, pwd, UITest.RODS_ADMIN_TYPE, driver);

        UserUtils.createProfileAndIncludeGroupToThisProfileAsAdmin(driver, adminUName, pwd, profileName, groupName);

        UserUtils.createUserAsAdminAndApplyProfile(driver, adminUName, pwd, uname, pwd, UITest.RODS_USER_TYPE, profileName);

        UserUtils.checkIfUserIsInGroup(driver, uname, groupName);

        UITest.login(uname, pwd);
        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, collPath);
        FileUtils.uploadFileThroughUI(driver, MetadataUtils.METADATA_SEARCH_FILES);

        driver.get(UITest.COLLECTIONS_URL);
        CollectionUtils.removeItems(driver, MetadataUtils.METADATA_SEARCH_FILES);

        cleanUpTrash();
        UITest.logout();
    }

    /**
     * Test scenario:
     * As rods, create a collection
     * As rods, create a group, give ownership of the collection above to this group.
     * As different admin, create a profile and include the group created above in this profile.
     * As different admin, apply this profile when modifying a user.
     * Check if the profile was applied and all groups the user is attached to
     * Verify by direct entry on breadcrumb
     * Verify that upload, file deletion for this user works in the collection above.
     *
     * @throws IOException
     * @throws MalformedURLException
     * @throws FailingHttpStatusCodeException
     */
    @Test
    public void testApplyProfileWhenModifyingAUser() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        UITest.login();
        CollectionUtils.createCollectionUnderZone(driver, collName, UITest.IRODS_ZONE);
        GroupUtils.createGroupWithPermissions(driver, groupName, UITest.OWN_PERMISSION, collName);
        UITest.logout();

        UserUtils.createUser(adminUName, pwd, UITest.RODS_ADMIN_TYPE, driver);

        UserUtils.createProfileAndIncludeGroupToThisProfileAsAdmin(driver, adminUName, pwd, profileName, groupName);

        UserUtils.createUser(uname, pwd, UITest.RODS_USER_TYPE, driver);

        UserUtils.modifyUserAsAdminAndApplyProfile(driver, adminUName, pwd, uname, UITest.IRODS_ZONE, UITest.RODS_USER_TYPE, profileName);

        UserUtils.checkIfUserIsInGroup(driver, uname, groupName);

        UITest.login(uname, pwd);
        CollectionUtils.writeOnEditableBreadCrumb(driver, wait, collPath);
        FileUtils.uploadFileThroughUI(driver, MetadataUtils.METADATA_SEARCH_FILES);

        driver.get(UITest.COLLECTIONS_URL);
        CollectionUtils.removeItems(driver, MetadataUtils.METADATA_SEARCH_FILES);

        cleanUpTrash();
        UITest.logout();
    }

    /**
     * Removes all items from the user's trash can.
     */
    private void cleanUpTrash() {
        driver.get(UITest.TRASH_URL);
        CollectionUtils.clickOnEmptyTrash(driver);
        CollectionUtils.confirmEmptyTrash(driver);
        wait.until(ExpectedConditions.visibilityOfElementLocated(CollectionUtils.COLLS_TABLE));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#treeViewTable tbody tr td[class='dataTables_empty']")));
    }

    /**
     * Removes users and groups from the data grid before the tests start and after they are executed.
     */
    private static void cleanUp() {
        try {
            UserUtils.removeUser(adminUName, driver);
            UserUtils.removeUser(uname, driver);
            UITest.login();
            GroupUtils.removeGroup(groupName, driver);
            ProfileUtils.removeProfile(profileName, driver);
            CollectionUtils.cleanUpCollectionsUnderZone(driver, collName);
        }
        catch (Exception e) {}
        finally {
            UITest.logout();
        }
    }
}
