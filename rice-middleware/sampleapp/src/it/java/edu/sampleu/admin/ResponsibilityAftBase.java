/**
 * Copyright 2005-2015 The Kuali Foundation
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
package edu.sampleu.admin;

import org.apache.commons.lang.RandomStringUtils;

/**
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
public abstract class ResponsibilityAftBase extends AdminTmplMthdAftNavCreateNewBase {

    protected void createNewEnterDetails() throws InterruptedException {
        waitAndTypeByName("document.documentHeader.documentDescription",RandomStringUtils.randomAlphabetic(30));
        String randomAlphabetic = RandomStringUtils.randomAlphabetic(10);
        selectByName("document.newMaintainableObject.namespaceCode","KR-BUS - Service Bus");
        waitAndTypeByName("document.newMaintainableObject.name",randomAlphabetic);
        waitAndTypeByName("document.newMaintainableObject.documentTypeName","RoutingRuleDocument");
        waitAndTypeByName("document.newMaintainableObject.routeNodeName",randomAlphabetic);
        waitAndClickByName("document.newMaintainableObject.actionDetailsAtRoleMemberLevel");
        waitAndClickByName("document.newMaintainableObject.required");
    }

    /**document.documentHeader.documentDescription
     * {@inheritDoc}
     * Responsibility
     * @return
     */
    @Override
    protected String getLinkLocator() {
        return "Responsibility";
    }

    protected void saveAndClose() throws InterruptedException {
        checkForDocError();
        waitAndClickByXpath(SAVE_XPATH);
        waitForTextPresent("Document was successfully saved");
        waitAndClickByName("methodToCall.close");
//         waitAndClickByName("methodToCall.processAnswer.button1");
    }

    protected void saveAndReload() throws InterruptedException {
        checkForDocError();
        waitAndClickByXpath(SAVE_XPATH);
        waitForTextPresent("Document was successfully saved");
        waitAndClickByName("methodToCall.reload");
//         waitAndClickByName("methodToCall.processAnswer.button1");
    }

    protected void submitAndClose() throws InterruptedException {
        checkForDocError();
        waitAndClickByName("methodToCall.route");
        waitForTextPresent("Document was successfully submitted");
        waitAndClickByName("methodToCall.close");
//         waitAndClickByName("methodToCall.processAnswer.button1");
    }

    /**
     * submits the doc and asserts that it was successfully submitted
     * does not close the document
     *
     * @throws InterruptedException
     */
    protected void submit() throws InterruptedException {
        checkForDocError();
        waitAndClickByName("methodToCall.route");
        waitForTextPresent("Document was successfully submitted");
    }
}
