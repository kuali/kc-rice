/**
 * Copyright 2005-2011 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.samplu.admin.test;

import edu.samplu.common.ITUtil;
import edu.samplu.common.WebDriverLegacyITBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * tests creating and cancelling new and edit Role maintenance screens
 *
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
public class IdentityRoleWDIT extends WebDriverLegacyITBase{

    public static final String TEST_URL = ITUtil.PORTAL + "?channelTitle=Role&channelUrl=" + ITUtil.getBaseUrlString() +
            "/kr/lookup.do?methodToCall=start&businessObjectClassName=org.kuali.rice.kim.impl.role.RoleBo&docFormKey=88888888&returnLocation=" +
            ITUtil.PORTAL_URL + "&hideReturnLink=true";

    @Override
    public String getTestUrl() {
        return TEST_URL;
    }

    @Test
    /**
     * tests that a new Role maintenance document can be cancelled
     */
    public void testCreateNewSearchReturnValueCancelConfirmation() throws Exception {
        super.testCreateNewSearchReturnValueCancelConfirmation();
    }

    @Test
    /**
     * tests that a Role maintenance document is created for an edit operation originating from a lookup screen
     */
    public void testSearchEditCancel() throws InterruptedException {
        super.testSearchEditCancel();
    }
}
