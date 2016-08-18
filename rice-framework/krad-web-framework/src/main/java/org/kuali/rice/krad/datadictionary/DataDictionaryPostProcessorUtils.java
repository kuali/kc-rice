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


import org.kuali.rice.krad.datadictionary.parse.StringListConverter;
import org.kuali.rice.krad.datadictionary.parse.StringMapConverter;
import org.kuali.rice.krad.datadictionary.uif.ComponentBeanPostProcessor;
import org.kuali.rice.krad.uif.util.ExpressionFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public final class DataDictionaryPostProcessorUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DataDictionaryPostProcessorUtils.class);

    private DataDictionaryPostProcessorUtils() {
        throw new UnsupportedOperationException("do not call");
    }

    /**
     * Sets up the bean post processor and conversion service
     *
     * @param beans - The bean factory for the the dictionary beans
     */
    public static void setupProcessor(DefaultListableBeanFactory beans) {
        try {
            // UIF post processor that sets component ids
            BeanPostProcessor idPostProcessor = ComponentBeanPostProcessor.class.newInstance();
            beans.addBeanPostProcessor(idPostProcessor);
            beans.setBeanExpressionResolver(new StandardBeanExpressionResolver() {
                @Override
                protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
                    try {
                        evalContext.registerFunction("getService", ExpressionFunctions.class.getDeclaredMethod("getService", String.class));
                    } catch(NoSuchMethodException me) {
                        LOG.error("Unable to register custom expression to data dictionary bean factory", me);
                    }
                }
            });

            // special converters for shorthand map and list property syntax
            GenericConversionService conversionService = new GenericConversionService();
            conversionService.addConverter(new StringMapConverter());
            conversionService.addConverter(new StringListConverter());

            beans.setConversionService(conversionService);
        } catch (Exception e1) {
            throw new DataDictionaryException("Cannot create component decorator post processor: " + e1.getMessage(),
                    e1);
        }
    }
}
