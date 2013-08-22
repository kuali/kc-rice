/**
 * Copyright 2005-2013 The Kuali Foundation
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
package org.kuali.rice.krad.service;

import org.junit.Test;
import org.kuali.rice.kns.service.KNSServiceLocator;
import org.kuali.rice.krad.test.document.bo.Account;
import org.kuali.rice.krad.test.document.bo.AccountManager;
import org.kuali.rice.krad.test.KRADTestCase;
import org.kuali.rice.location.api.state.State;
import org.kuali.rice.location.impl.state.StateBo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * BusinessObjectServiceTest tests KULRICE-1666: missing Spring mapping for ojbCollectionHelper
 * (not injected into BusinessObjectDaoTest)
 *
 * @author Kuali Rice Team (rice.collab@kuali.org)
 *
 */
@KRADTestCase.Legacy
public class BusinessObjectServiceTest extends KRADTestCase {

    public BusinessObjectServiceTest() {}

    /**
     * This method tests saving a BO with a collection member
     *
     * @throws Exception
     */
    @Test
    public void testSave() throws Exception {
        BusinessObjectService businessObjectService = KNSServiceLocator.getBusinessObjectService();
        
        AccountManager am = new AccountManager();
        am.setUserName("bhutchin");
        List<Account> accounts = new ArrayList<Account>();
        Account account1 = new Account();
        account1.setNumber("1");
        account1.setName("account 1");
        account1.setAccountManager(am);
        accounts.add(account1);

        Account account2 = new Account();
        account2.setNumber("2");
        account2.setName("account 2");
        account2.setAccountManager(am);

        accounts.add(account2);
        am.setAccounts(accounts);

        businessObjectService.save(am);
    }
    
    /**
     * Checks that a business object can be correctly retrieved through BusinessObjectService#retrieve
     */
    @Test
    public void testRetrieve() {
    	BusinessObjectService businessObjectService = KNSServiceLocator.getBusinessObjectService();
    	
    	AccountManager manager = new AccountManager();
    	manager.setUserName("mgorilla");
    	List<Account> accounts = new ArrayList<Account>();
    	Account account1 = new Account();
    	account1.setNumber("MG1");
    	account1.setName("Manilla Gorilla Account");
    	account1.setAccountManager(manager);
    	accounts.add(account1);
    	manager.setAccounts(accounts);
    	
    	manager = (AccountManager)businessObjectService.save(manager);
    	
    	AccountManager manager2 = (AccountManager)businessObjectService.retrieve(manager);
    	assertNotNull("manager2 should not be null", manager2);
    	assertEquals("manager2 should have the same user name as manager", manager.getUserName(), manager2.getUserName());
    	
    	AccountManager manager3 = new AccountManager();
    	manager3.setAmId(manager.getAmId());
    	manager2 = (AccountManager)businessObjectService.retrieve(manager3);
    	assertNotNull("manager2 should not be null", manager2);
    	assertEquals("manager2 should have the same user name as manager", manager.getUserName(), manager2.getUserName());
    	
    	manager3.setAmId(-99L);
    	manager2 = (AccountManager)businessObjectService.retrieve(manager3);
    	assertNull("manager2 should be null", manager2);
        
        AccountManager manager4 = new AccountManager();
        manager4.setAmId(manager.getAmId());
        manager2 = (AccountManager)businessObjectService.findBySinglePrimaryKey(AccountManager.class, manager4.getAmId());
        assertNotNull("manager2 should not be null", manager2);
        assertEquals("manager2 should have the same user name as manager", manager.getUserName(), manager2.getUserName());

    }

}
