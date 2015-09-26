/*
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

function updateStatesBasedOnCountryCode(index) { 
  var countryCode = dwr.util.getValue("newAddress.countryCode"); 
  if (index != undefined) { 
    countryCode = dwr.util.getValue("document.addrs[" + index + "].countryCode"); 
  } 
  var dwrReply = { 
    callback:function(data) { 
      if ( data != null ) { 
        var stateProvinceCode = "newAddress.stateProvinceCode"; 
        if (index != undefined) { 
          stateProvinceCode = "document.addrs[" + index + "].stateProvinceCode"; 
        } 
        dwr.util.removeAllOptions(stateProvinceCode); 
        dwr.util.addOptions( stateProvinceCode, data, 'code', 'name' ); 
      } 
    },         errorHandler:function( errorMessage ) { 
      window.status = errorMessage;         } 
  };      StateService.findAllStatesInCountry(countryCode, dwrReply); }
