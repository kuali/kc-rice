package org.kuali.rice.ksb.impl.bus;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.kuali.rice.core.util.io.SerializationUtils;
import org.kuali.rice.ksb.api.bus.Endpoint;
import org.kuali.rice.ksb.api.bus.ServiceConfiguration;
import org.kuali.rice.ksb.api.registry.ServiceDescriptor;
import org.kuali.rice.ksb.api.registry.ServiceInfo;
import org.kuali.rice.ksb.api.registry.ServiceRegistry;

public final class RemoteService {
	
	private final ServiceInfo serviceInfo;
	private final ServiceRegistry serviceRegistry;
	
	private final Object endpointAcquisitionLock = new Object();
	private volatile Endpoint endpoint;
	
	public RemoteService(ServiceInfo serviceInfo, ServiceRegistry serviceRegistry) {
		validateServiceInfo(serviceInfo);
		if (serviceRegistry == null) {
			throw new IllegalArgumentException("serviceRegistry cannot be null");
		}
		this.serviceInfo = serviceInfo;
		this.serviceRegistry = serviceRegistry;
	}
	
	private static void validateServiceInfo(ServiceInfo serviceInfo) {
		if (serviceInfo == null) {
			throw new IllegalArgumentException("serviceInfo cannot be null");
		}
		if (serviceInfo.getServiceId() == null) {
			throw new IllegalArgumentException("serviceInfo must have a serviceId but was null");
		}
	}
	
	public QName getServiceName() {
		return serviceInfo.getServiceName();
	}
		
	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}
	
	public Endpoint getEndpoint() {
		// double-checked locking idiom - see Effective Java, Item 71
		Endpoint internalEndpoint = this.endpoint;
		if (internalEndpoint == null) {
			synchronized (endpointAcquisitionLock) {
				internalEndpoint = this.endpoint;
				if (internalEndpoint == null) {
					this.endpoint = internalEndpoint = new LazyEndpoint(constructServiceConfiguration()); 
				}
			}
		}
		return internalEndpoint;
	}
	
	protected ServiceConfiguration constructServiceConfiguration() {
		ServiceDescriptor serviceDescriptor = serviceRegistry.getServiceDescriptor(serviceInfo.getServiceId());
		if (serviceDescriptor == null) {
			throw new IllegalStateException("Failed to locate ServiceDescriptor for ServiceInfo with serviceEndpointId=" + serviceInfo.getServiceId());
		} else if (StringUtils.isBlank(serviceDescriptor.getDescriptor())) {
			throw new IllegalStateException("ServiceDescriptor descriptor value is blank or null for descriptor with serviceEndpointId=" + serviceInfo.getServiceId());
		}
		return (ServiceConfiguration)SerializationUtils.deserializeFromBase64(serviceDescriptor.getDescriptor());
	}
	
	@Override
    public boolean equals(Object obj) {
		if (!(obj instanceof RemoteService)) {
			return false;
		}
		return serviceInfo.equals(((RemoteService)obj).getServiceInfo());
    }

	@Override
    public int hashCode() {
        return serviceInfo.hashCode();
    }
	
}