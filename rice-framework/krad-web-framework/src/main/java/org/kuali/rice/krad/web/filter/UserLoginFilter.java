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
package org.kuali.rice.krad.web.filter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.kuali.rice.core.api.CoreApiServiceLocator;
import org.kuali.rice.core.api.config.property.ConfigurationService;
import org.kuali.rice.core.api.exception.RiceRuntimeException;
import org.kuali.rice.core.api.resourceloader.GlobalResourceLoader;
import org.kuali.rice.coreservice.framework.CoreFrameworkServiceLocator;
import org.kuali.rice.coreservice.framework.parameter.ParameterService;
import org.kuali.rice.kew.api.KewApiConstants;
import org.kuali.rice.kim.api.KimConstants;
import org.kuali.rice.kim.api.identity.AuthenticationService;
import org.kuali.rice.kim.api.identity.IdentityService;
import org.kuali.rice.kim.api.identity.principal.Principal;
import org.kuali.rice.kim.api.permission.PermissionService;
import org.kuali.rice.kim.api.services.KimApiServiceLocator;
import org.kuali.rice.krad.UserSession;
import org.kuali.rice.krad.exception.AuthenticationException;
import org.kuali.rice.krad.util.KRADConstants;
import org.kuali.rice.krad.util.KRADUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * A filter for processing user logins and creating a {@link org.kuali.rice.krad.UserSession}
 *
 * @author Kuali Rice Team (rice.collab@kuali.org)
 * @see org.kuali.rice.krad.UserSession
 */
public class UserLoginFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(UserLoginFilter.class);
    private static final String MDC_USER = "user";

    private IdentityService identityService;
    private PermissionService permissionService;
    private ConfigurationService kualiConfigurationService;
    private ParameterService parameterService;

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.filterConfig = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        this.doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    protected void doFilter(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        try {
            establishUserSession(request);
            establishSessionCookie(request, response);
            establishBackdoorUser(request);

            addToMDC(request);

            chain.doFilter(request, response);
        } finally {
            removeFromMDC();
        }
    }

    @Override
    public void destroy() {
        filterConfig = null;
    }

    /**
     * Checks if a user can be authenticated and if so establishes a UserSession for that user.
     */
    protected void establishUserSession(HttpServletRequest request) {
        UserSession userSession = (UserSession) request.getSession().getAttribute(KRADConstants.USER_SESSION_KEY);
        String remoteUser = extractRemoteUser(request);
        logDebugInfoAboutUser(remoteUser, userSession);
        if (userSession == null || !isRemoteUserSameAsUserSession(userSession, remoteUser)) {
            LOG.info("creating new user session for " + remoteUser);
            validatePrincipal(remoteUser);
            userSession = createUserSession(request, remoteUser);
        }
		updateUserSession(userSession, request);
    }
    
    protected void logDebugInfoAboutUser(String remoteUser, UserSession userSession) {
    	if (LOG.isDebugEnabled()) {
    		StringBuilder buffer = new StringBuilder();
    		buffer.append("remote user = ").append(remoteUser);
    		if (userSession != null) {
    			buffer.append(" -- current user = ")
    			.append(userSession.getLoggedInUserPrincipalName())
    			.append("(")
    			.append(userSession.getLoggedInUserPrincipalId())
    			.append(")");
    		} else {
    			buffer.append(" -- no usersession");
    		}
    		LOG.debug(buffer.toString());
    	}
    }

	protected boolean isRemoteUserSameAsUserSession(UserSession userSession,
			String remoteUser) {
		if (userSession == null) {
			return false;
		} else {
			return isRemoteUserPrincpialId()
				? StringUtils.equalsIgnoreCase(userSession.getLoggedInUserPrincipalId(), remoteUser)
				: StringUtils.equalsIgnoreCase(userSession.getLoggedInUserPrincipalName(), remoteUser);
		}
	}
    
    /**
     * Method to allow local overrides to update the userSession as necessary during each request. Defaults to a noop. 
     */
    protected void updateUserSession(UserSession userSession, HttpServletRequest request) { }

	protected void validatePrincipal(String remoteUser) {
		if (StringUtils.isBlank(remoteUser)) {
		    throw new AuthenticationException("Blank User from AuthenticationService - This should never happen.");
		}

		final Principal principal = isRemoteUserPrincpialId() 
		    ? getIdentityService().getPrincipal(remoteUser)
		    : getIdentityService().getPrincipalByPrincipalName(remoteUser);

		if (principal == null) {
		    throw new AuthenticationException("Unknown User: " + remoteUser);
		}

		if (!isAuthorizedToLogin(principal.getPrincipalId())) {
		    throw new AuthenticationException(
		            "You cannot log in, because you are not an active Kuali user.\nPlease ask someone to activate your account if you need to use Kuali Systems.\nThe user id provided was: "
		                    + remoteUser + ".\n");
		}
	}

	protected boolean isRemoteUserPrincpialId() {
		return getKualiConfigurationService().getPropertyValueAsBoolean(KRADConstants.AUTHN_USE_PRINCIPAL_ID);
	}

	protected String extractRemoteUser(HttpServletRequest request) {
		return ((AuthenticationService) GlobalResourceLoader.getResourceLoader().getService(
		        new QName("kimAuthenticationService"))).getPrincipalName(request);
	}

	protected UserSession createUserSession(HttpServletRequest request,
			String remoteUser) {
		UserSession userSession;
		userSession = new UserSession(remoteUser);
		if (userSession.getPerson() == null) {
		    throw new AuthenticationException("Invalid User: " + remoteUser);
		}

		request.getSession().setAttribute(KRADConstants.USER_SESSION_KEY, userSession);
		return userSession;
	}

    /**
     * checks if the passed in principalId is authorized to log in.
     */
    protected boolean isAuthorizedToLogin(String principalId) {
        return getPermissionService().isAuthorized(principalId, KimConstants.KIM_TYPE_DEFAULT_NAMESPACE,
                KimConstants.PermissionNames.LOG_IN, Collections.singletonMap("principalId", principalId));
    }

    /**
     * Creates a session id cookie if one does not exists.  Write the cookie out to the response with that session id.
     * Also, sets the cookie on the established user session.
     */
    protected void establishSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        String kualiSessionId = this.getKualiSessionId(request.getCookies());
        if (kualiSessionId == null) {
            kualiSessionId = UUID.randomUUID().toString();
            response.addCookie(new Cookie(KRADConstants.KUALI_SESSION_ID, kualiSessionId));
        }

        if (KRADUtils.getUserSessionFromRequest(request).getKualiSessionId() == null) {
            KRADUtils.getUserSessionFromRequest(request).setKualiSessionId(kualiSessionId);
        }
    }

    /**
     * gets the kuali session id from an array of cookies.  If a session id does not exist returns null.
     */
    protected String getKualiSessionId(final Cookie[] cookies) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (KRADConstants.KUALI_SESSION_ID.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * establishes the backdoor user on the established user id if backdoor capabilities are valid.
     */
    private void establishBackdoorUser(HttpServletRequest request) {
        final String backdoor = request.getParameter(KRADConstants.BACKDOOR_PARAMETER);
        if (StringUtils.isNotBlank(backdoor)) {
            if (!getKualiConfigurationService().getPropertyValueAsString(KRADConstants.PROD_ENVIRONMENT_CODE_KEY)
                    .equalsIgnoreCase(getKualiConfigurationService().getPropertyValueAsString(
                            KRADConstants.ENVIRONMENT_KEY))) {
                if (getParameterService().getParameterValueAsBoolean(KRADConstants.KUALI_RICE_WORKFLOW_NAMESPACE,
                        KRADConstants.DetailTypes.BACKDOOR_DETAIL_TYPE, KewApiConstants.SHOW_BACK_DOOR_LOGIN_IND)) {
                    try {
                        KRADUtils.getUserSessionFromRequest(request).setBackdoorUser(backdoor);
                    } catch (RiceRuntimeException re) {
                        //Ignore so BackdoorAction can redirect to invalid_backdoor_portal
                    }
                }
            }
        }
    }

    private void addToMDC(HttpServletRequest request) {
        MDC.put(MDC_USER, KRADUtils.getUserSessionFromRequest(request).getPrincipalName());
    }

    private void removeFromMDC() {
        MDC.remove(MDC_USER);
    }

    protected IdentityService getIdentityService() {
        if (this.identityService == null) {
            this.identityService = KimApiServiceLocator.getIdentityService();
        }

        return this.identityService;
    }

    protected PermissionService getPermissionService() {
        if (this.permissionService == null) {
            this.permissionService = KimApiServiceLocator.getPermissionService();
        }

        return this.permissionService;
    }

    protected ConfigurationService getKualiConfigurationService() {
        if (this.kualiConfigurationService == null) {
            this.kualiConfigurationService = CoreApiServiceLocator.getKualiConfigurationService();
        }

        return this.kualiConfigurationService;
    }

    protected ParameterService getParameterService() {
        if (this.parameterService == null) {
            this.parameterService = CoreFrameworkServiceLocator.getParameterService();
        }

        return this.parameterService;
    }
}
