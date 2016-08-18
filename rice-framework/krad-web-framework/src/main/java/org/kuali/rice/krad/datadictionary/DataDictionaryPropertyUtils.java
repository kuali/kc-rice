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
package org.kuali.rice.krad.datadictionary;


import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.kuali.rice.krad.datadictionary.exception.AttributeValidationException;
import org.kuali.rice.krad.datadictionary.exception.CompletionException;
import org.kuali.rice.krad.service.KRADServiceLocatorWeb;
import org.kuali.rice.krad.service.LegacyDataAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class DataDictionaryPropertyUtils {

    private static final Map<String, Map<String, PropertyDescriptor>> CACHE = new ConcurrentHashMap<>();
    private static LegacyDataAdapter legacyDataAdapter;
    private static final Logger LOG = LoggerFactory.getLogger(DataDictionaryPropertyUtils.class);

    private DataDictionaryPropertyUtils() {
        throw new UnsupportedOperationException("do not call");
    }

    /**
     * @return true if the given propertyName names a property of the given class
     * @throws CompletionException if there is a problem accessing the named property on the given class
     */
    public static boolean isPropertyOf(Class targetClass, String propertyName) {
        if (targetClass == null) {
            throw new IllegalArgumentException("invalid (null) targetClass");
        }
        if (StringUtils.isBlank(propertyName)) {
            throw new IllegalArgumentException("invalid (blank) propertyName");
        }
        try {
            PropertyDescriptor propertyDescriptor = buildReadDescriptor(targetClass, propertyName);

            return propertyDescriptor != null;
        } catch ( Exception ex ) {
            LOG.error( "Exception while obtaining property descriptor for " + targetClass.getName() + "." + propertyName, ex );
            return false;
        }
    }

    /**
     * @return true if the given propertyName names a Collection property of the given class
     * @throws CompletionException if there is a problem accessing the named property on the given class
     */
    public static boolean isCollectionPropertyOf(Class targetClass, String propertyName) {
        boolean isCollectionPropertyOf = false;

        PropertyDescriptor propertyDescriptor = buildReadDescriptor(targetClass, propertyName);
        if (propertyDescriptor != null) {
            Class clazz = propertyDescriptor.getPropertyType();

            if ((clazz != null) && Collection.class.isAssignableFrom(clazz)) {
                isCollectionPropertyOf = true;
            }
        }

        return isCollectionPropertyOf;
    }

    /**
     * This method determines the Class of the attributeName passed in. Null will be returned if the member is not
     * available, or if
     * a reflection exception is thrown.
     *
     * @param boClass - Class that the attributeName property exists in.
     * @param attributeName - Name of the attribute you want a class for.
     * @return The Class of the attributeName, if the attribute exists on the rootClass. Null otherwise.
     */
    public static Class getAttributeClass(Class boClass, String attributeName) {

        // fail loudly if the attributeName isnt a member of rootClass
        if (!isPropertyOf(boClass, attributeName)) {
            throw new AttributeValidationException(
                    "unable to find attribute '" + attributeName + "' in rootClass '" + boClass.getName() + "'");
        }

        //Implementing Externalizable Business Object Services...
        //The boClass can be an interface, hence handling this separately,
        //since the original method was throwing exception if the class could not be instantiated.
        if (boClass.isInterface()) {
            return getAttributeClassWhenBOIsInterface(boClass, attributeName);
        } else {
            return getAttributeClassWhenBOIsClass(boClass, attributeName);
        }

    }

    /**
     * This method gets the property type of the given attributeName when the bo class is a concrete class.
     */
    private static Class<?> getAttributeClassWhenBOIsClass(Class<?> boClass, String attributeName) {
        Object boInstance;
        try {

            //KULRICE-11351 should not differentiate between primitive types and their wrappers during DD validation
            if (boClass.isPrimitive()) {
                boClass = ClassUtils.primitiveToWrapper(boClass);
            }

            boInstance = boClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate Data Object: " + boClass, e);
        }

        // attempt to retrieve the class of the property
        try {
            return getLegacyDataAdapter().getPropertyType(boInstance, attributeName);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to determine property type for: " + boClass.getName() + "." + attributeName, e);
        }
    }

    /**
     * This method gets the property type of the given attributeName when the bo class is an interface
     * This method will also work if the bo class is not an interface,
     * but that case requires special handling, hence a separate method getAttributeClassWhenBOIsClass
     *
     */
    private static Class<?> getAttributeClassWhenBOIsInterface(Class<?> boClass, String attributeName) {
        if (boClass == null) {
            throw new IllegalArgumentException("invalid (null) boClass");
        }
        if (StringUtils.isBlank(attributeName)) {
            throw new IllegalArgumentException("invalid (blank) attributeName");
        }

        PropertyDescriptor propertyDescriptor;

        String[] intermediateProperties = attributeName.split("\\.");
        int lastLevel = intermediateProperties.length - 1;
        Class currentClass = boClass;

        for (int i = 0; i <= lastLevel; ++i) {

            String currentPropertyName = intermediateProperties[i];
            propertyDescriptor = buildSimpleReadDescriptor(currentClass, currentPropertyName);

            if (propertyDescriptor != null) {

                Class propertyType = propertyDescriptor.getPropertyType();
                if (getLegacyDataAdapter().isExtensionAttribute(currentClass, currentPropertyName, propertyType)) {
                    propertyType = getLegacyDataAdapter().getExtensionAttributeClass(currentClass, currentPropertyName);
                }
                if (Collection.class.isAssignableFrom(propertyType)) {
                    // TODO: determine property type using generics type definition
                    throw new AttributeValidationException(
                            "Can't determine the Class of Collection elements because when the business object is an (possibly ExternalizableBusinessObject) interface.");
                } else {
                    currentClass = propertyType;
                }
            } else {
                throw new AttributeValidationException(
                        "Can't find getter method of " + boClass.getName() + " for property " + attributeName);
            }
        }
        return currentClass;
    }

    /**
     * This method determines the Class of the elements in the collectionName passed in.
     *
     * @param boClass Class that the collectionName collection exists in.
     * @param collectionName the name of the collection you want the element class for
     * @return collection element type
     */
    public static Class getCollectionElementClass(Class boClass, String collectionName) {
        if (boClass == null) {
            throw new IllegalArgumentException("invalid (null) boClass");
        }
        if (StringUtils.isBlank(collectionName)) {
            throw new IllegalArgumentException("invalid (blank) collectionName");
        }

        PropertyDescriptor propertyDescriptor;

        String[] intermediateProperties = collectionName.split("\\.");
        Class currentClass = boClass;

        for (String currentPropertyName : intermediateProperties) {

            propertyDescriptor = buildSimpleReadDescriptor(currentClass, currentPropertyName);

            if (propertyDescriptor != null) {

                Class type = propertyDescriptor.getPropertyType();
                if (Collection.class.isAssignableFrom(type)) {
                    currentClass = getLegacyDataAdapter().determineCollectionObjectType(currentClass, currentPropertyName);
                } else {
                    currentClass = propertyDescriptor.getPropertyType();
                }
            }
        }

        return currentClass;
    }

    /**
     * @return PropertyDescriptor for the getter for the named property of the given class, if one exists.
     */
    private static PropertyDescriptor buildReadDescriptor(Class propertyClass, String propertyName) {
        if (propertyClass == null) {
            throw new IllegalArgumentException("invalid (null) propertyClass");
        }
        if (StringUtils.isBlank(propertyName)) {
            throw new IllegalArgumentException("invalid (blank) propertyName");
        }

        PropertyDescriptor propertyDescriptor = null;

        String[] intermediateProperties = propertyName.split("\\.");
        int lastLevel = intermediateProperties.length - 1;
        Class currentClass = propertyClass;

        for (int i = 0; i <= lastLevel; ++i) {

            String currentPropertyName = intermediateProperties[i];
            propertyDescriptor = buildSimpleReadDescriptor(currentClass, currentPropertyName);

            if (i < lastLevel) {

                if (propertyDescriptor != null) {

                    Class propertyType = propertyDescriptor.getPropertyType();
                    if (getLegacyDataAdapter().isExtensionAttribute(currentClass, currentPropertyName, propertyType)) {
                        propertyType = getLegacyDataAdapter().getExtensionAttributeClass(currentClass,
                                currentPropertyName);
                    }
                    if (Collection.class.isAssignableFrom(propertyType)) {
                        currentClass = getLegacyDataAdapter().determineCollectionObjectType(currentClass, currentPropertyName);
                    } else {
                        currentClass = propertyType;
                    }

                }

            }

        }

        return propertyDescriptor;
    }

    /**
     * @return PropertyDescriptor for the getter for the named property of the given class, if one exists.
     */
    private static PropertyDescriptor buildSimpleReadDescriptor(Class propertyClass, String propertyName) {
        if (propertyClass == null) {
            throw new IllegalArgumentException("invalid (null) propertyClass");
        }
        if (StringUtils.isBlank(propertyName)) {
            throw new IllegalArgumentException("invalid (blank) propertyName");
        }

        PropertyDescriptor p = null;

        // check to see if we've cached this descriptor already. if yes, return true.
        String propertyClassName = propertyClass.getName();
        Map<String, PropertyDescriptor> m = CACHE.get(propertyClassName);
        if (null != m) {
            p = m.get(propertyName);
            if (null != p) {
                return p;
            }
        }

        // Use PropertyUtils.getPropertyDescriptors instead of manually constructing PropertyDescriptor because of
        // issues with introspection and generic/co-variant return types
        // See https://issues.apache.org/jira/browse/BEANUTILS-340 for more details

        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(propertyClass);
        if (ArrayUtils.isNotEmpty(descriptors)) {
            for (PropertyDescriptor descriptor : descriptors) {
                if (descriptor.getName().equals(propertyName)) {
                    p = descriptor;
                }
            }
        }

        // cache the property descriptor if we found it.
        if (p != null) {
            if (m == null) {
                m = new TreeMap<>();
                CACHE.put(propertyClassName, m);
            }
            m.put(propertyName, p);
        }

        return p;
    }

    private static LegacyDataAdapter getLegacyDataAdapter() {
        if (legacyDataAdapter == null) {
            legacyDataAdapter = KRADServiceLocatorWeb.getLegacyDataAdapter();
        }
        return legacyDataAdapter;
    }
}
