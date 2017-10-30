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

package com.emc.metalnx.integration.test.templates;

import com.emc.metalnx.core.domain.exceptions.DataGridException;
import com.emc.metalnx.integration.test.utils.TemplateUtils;
import com.emc.metalnx.test.generic.UITest;
import junit.framework.Assert;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Test adding a long attribute field to a template.
 *
 */
@Deprecated
@Ignore
public class LongTemplateAVUFieldsTest {
    private static WebDriver driver = null;
    private static String templateName = null;

    /************************************* TEST SET UP *************************************/

    @BeforeClass
    public static void setUpBeforeClass() throws DataGridException {
        UITest.setUpBeforeClass();
        driver = UITest.getDriver();

        try {
            UITest.login();
            driver.get(UITest.TEMPLATES_URL);
            TemplateUtils.removeAllTemplates(driver);
        }
        catch (Exception e) {}
        finally {
            UITest.logout();
        }
    }

    @Before
    public void setUp() throws Exception {
        templateName = RandomStringUtils.randomAlphanumeric(60) + System.currentTimeMillis();
        UITest.login();
    }

    @After
    public void tearDown() throws Exception {
        UITest.logout();
    }

    /**
     * After all tests are done, the test must quit the driver. This will close every window
     * associated with the current driver instance.
     */

    @AfterClass
    public static void tearDownAfterClass() {
        UITest.login();
        driver.get(UITest.TEMPLATES_URL);
        TemplateUtils.removeAllTemplates(driver);
        UITest.logout();

        if (driver != null) {
            driver.quit();
            driver = null;
            UITest.setDriver(null);
        }
    }

    @Test
    public void testSystemTemplateAVUsWith100Characters() {
        String[] attrs = { RandomStringUtils.randomAlphanumeric(100) };
        String[] values = { RandomStringUtils.randomAlphanumeric(100) };
        String[] units = { RandomStringUtils.randomAlphanumeric(100) };

        TemplateUtils.createTemplateWithFields(driver, templateName, TemplateUtils.SYSTEM_TEMPLATE_TYPE, attrs, values, units);
        TemplateUtils.assertTemplateSuccessfulCreation(driver, templateName);

        TemplateUtils.searchByTemplateName(driver, templateName);
        Assert.assertNotNull(driver.findElement(By.cssSelector("#templatesListTable tbody tr td[title='" + templateName + "']")));
    }

    @Test
    public void testPrivateTemplateAVUsWith100Characters() {
        String[] attrs = { RandomStringUtils.randomAlphanumeric(100) };
        String[] values = { RandomStringUtils.randomAlphanumeric(100) };
        String[] units = { RandomStringUtils.randomAlphanumeric(100) };

        TemplateUtils.createTemplateWithFields(driver, templateName, TemplateUtils.PRIVATE_TEMPLATE_TYPE, attrs, values, units);
        TemplateUtils.assertTemplateSuccessfulCreation(driver, templateName);

        TemplateUtils.searchByTemplateName(driver, templateName);
        Assert.assertNotNull(driver.findElement(By.cssSelector("#templatesListTable tbody tr td[title='" + templateName + "']")));
    }
}
