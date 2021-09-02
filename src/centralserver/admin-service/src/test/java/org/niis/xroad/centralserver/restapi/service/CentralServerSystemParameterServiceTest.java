/**
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.centralserver.restapi.service;

import org.junit.Ignore;
import org.junit.Test;
import org.niis.xroad.centralserver.restapi.config.AbstractFacadeMockingTestContext;
import org.niis.xroad.centralserver.restapi.config.HAConfigStatus;
import org.niis.xroad.centralserver.restapi.entity.SystemParameter;
import org.niis.xroad.centralserver.restapi.repository.SystemParameterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.niis.xroad.centralserver.restapi.service.CentralServerSystemParameterService.CENTRAL_SERVER_ADDRESS;
import static org.niis.xroad.centralserver.restapi.service.CentralServerSystemParameterService.INSTANCE_IDENTIFIER;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public class CentralServerSystemParameterServiceTest extends AbstractFacadeMockingTestContext {




    @MockBean
    HAConfigStatus currentHaConfigStatus;

    @Autowired
    SystemParameterRepository systemParameterRepository;


    @Autowired
    CentralServerSystemParameterService centralServerSystemParameterService;

    @Test
    public void mockContextLoads() {
        assertTrue(true);
    }

    @Test
    public void systemParameterValueStored() {

        final String instanceTestValue = "VALID_INSTANCE";
        SystemParameter systemParameter = centralServerSystemParameterService
                .updateOrCreateParameter(
                        INSTANCE_IDENTIFIER,
                        instanceTestValue
                );
        assertEquals(instanceTestValue, systemParameter.getValue());
        String storedSystemParameterValue = centralServerSystemParameterService
                .getParameterValue(
                        INSTANCE_IDENTIFIER,
                        "not-from-db");
        assertNotEquals("not-from-db", storedSystemParameterValue);
        assertEquals(instanceTestValue, storedSystemParameterValue);
    }

    // This works only when postgresql-specific HA triggers are defined. E.g. NOT with embedded test databases.
    @Ignore("HA-specific test cases need postgresql db")
    @Test
    public void systemParameterValueStoredHaEnabled() {
        when(currentHaConfigStatus.isHaConfigured()).thenReturn(true);
        when(currentHaConfigStatus.getCurrentHaNodeName()).thenReturn("node_1");
        final String centralServerAddress = "example.org";
        SystemParameter systemParameter = centralServerSystemParameterService
                .updateOrCreateParameter(
                        CENTRAL_SERVER_ADDRESS,
                        centralServerAddress
                );
        assertEquals(centralServerAddress, systemParameter.getValue());
        String storedSystemParameterValue = centralServerSystemParameterService
                .getParameterValue(
                        CENTRAL_SERVER_ADDRESS,
                        "not-from-db");
        assertNotEquals("not-from-db", storedSystemParameterValue);
        assertEquals(centralServerAddress, storedSystemParameterValue);
    }

}