<%--
 Copyright 2006-2007 The Kuali Foundation
 
 Licensed under the Educational Community License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.opensource.org/licenses/ecl2.php
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
--%>
<%@ include file="/krad/WEB-INF/jsp/tldHeader.jsp"%>

<tiles:useAttribute name="items" classname="java.util.List"/>
<tiles:useAttribute name="manager" classname="org.kuali.rice.kns.uif.layout.TableLayoutManager"/>

<%--
    Table Layout Manager:
    
      Works on a collection group to lay out the items as a table.
 --%>
 
<table id="${manager.id}" style="${manager.style}" class="${manager.styleClass}">
  <thead>
     <krad:grid items="${manager.headerFields}" numberOfColumns="${manager.numberOfColumns}" 
                renderHeaderColumns="true"/>
  </thead>
  
  <tbody>
     <krad:grid items="${manager.dataFields}" numberOfColumns="${manager.numberOfColumns}" 
                applyAlternatingRowStyles="${manager.applyAlternatingRowStyles}"/>
  </tbody>
</table>

<br/><br/>
 
<%-- invoke table tools widget --%>
<c:if test="${(!empty manager.tableTools) && manager.tableTools.render}">              
   <tiles:insertTemplate template="${manager.tableTools.template}">
      <tiles:putAttribute name="${manager.tableTools.componentTypeName}" value="${manager.tableTools}"/>
      <tiles:putAttribute name="componentId" value="${manager.id}"/>
   </tiles:insertTemplate>  
</c:if> 
 