/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2016, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package org.thymeleaf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.thymeleaf.cache.ICacheManager;
import org.thymeleaf.context.IEngineContextFactory;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.engine.AttributeDefinitions;
import org.thymeleaf.engine.ElementDefinitions;
import org.thymeleaf.engine.StandardModelFactory;
import org.thymeleaf.engine.TemplateManager;
import org.thymeleaf.expression.IExpressionObjectFactory;
import org.thymeleaf.linkbuilder.ILinkBuilder;
import org.thymeleaf.messageresolver.IMessageResolver;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.postprocessor.IPostProcessor;
import org.thymeleaf.preprocessor.IPreProcessor;
import org.thymeleaf.processor.cdatasection.ICDATASectionProcessor;
import org.thymeleaf.processor.comment.ICommentProcessor;
import org.thymeleaf.processor.doctype.IDocTypeProcessor;
import org.thymeleaf.processor.element.IElementProcessor;
import org.thymeleaf.processor.processinginstruction.IProcessingInstructionProcessor;
import org.thymeleaf.processor.templateboundaries.ITemplateBoundariesProcessor;
import org.thymeleaf.processor.text.ITextProcessor;
import org.thymeleaf.processor.xmldeclaration.IXMLDeclarationProcessor;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateparser.markup.decoupled.IDecoupledTemplateLogicResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.text.ITextRepository;
import org.thymeleaf.util.Validate;

/**
 * <p>
 *   Default implementation of the {@link IEngineConfiguration} interface.
 * </p>
 * <p>
 *   There is normally no reason why user code would directly use this class instead of its interface.
 * </p>
 *
 * @author Daniel Fern&aacute;ndez
 * @since 3.0.0
 * 
 */
public class EngineConfiguration implements IEngineConfiguration {

    private final DialectSetConfiguration dialectSetConfiguration;
    private final ITextRepository textRepository;
    private final Set<ITemplateResolver> templateResolvers;
    private final Set<IMessageResolver> messageResolvers;
    private final Set<ILinkBuilder> linkBuilders;
    private final ICacheManager cacheManager;
    private final IEngineContextFactory engineContextFactory;
    private final IDecoupledTemplateLogicResolver decoupledTemplateLogicResolver;
    private TemplateManager templateManager;
    private final ConcurrentHashMap<TemplateMode,IModelFactory> modelFactories;


    /*
     * There is no reason at all why anyone would want to manually create an instance of this.
     */
    EngineConfiguration(
            final Set<ITemplateResolver> templateResolvers,
            final Set<IMessageResolver> messageResolvers,
            final Set<ILinkBuilder> linkBuilders,
            final Set<DialectConfiguration> dialectConfigurations,
            final ICacheManager cacheManager,
            final IEngineContextFactory engineContextFactory,
            final IDecoupledTemplateLogicResolver decoupledTemplateLogicResolver,
            final ITextRepository textRepository) {

        super();

        Validate.notNull(templateResolvers, "Template Resolver set cannot be null");
        Validate.isTrue(templateResolvers.size() > 0, "Template Resolver set cannot be empty");
        Validate.containsNoNulls(templateResolvers, "Template Resolver set cannot contain any nulls");
        Validate.notNull(messageResolvers, "Message Resolver set cannot be null");
        Validate.notNull(dialectConfigurations, "Dialect configuration set cannot be null");
        // Cache Manager CAN be null
        Validate.notNull(engineContextFactory, "Engine Context Factory cannot be null");
        Validate.notNull(decoupledTemplateLogicResolver, "Decoupled Template Logic Resolver cannot be null");
        Validate.notNull(textRepository, "Text Repository cannot be null");

        final List<ITemplateResolver> templateResolversList = new ArrayList<ITemplateResolver>(templateResolvers);
        Collections.sort(templateResolversList, TemplateResolverComparator.INSTANCE);
        this.templateResolvers = Collections.unmodifiableSet(new LinkedHashSet<ITemplateResolver>(templateResolversList));

        final List<IMessageResolver> messageResolversList = new ArrayList<IMessageResolver>(messageResolvers);
        Collections.sort(messageResolversList, MessageResolverComparator.INSTANCE);
        this.messageResolvers = Collections.unmodifiableSet(new LinkedHashSet<IMessageResolver>(messageResolversList));

        final List<ILinkBuilder> linkBuilderList = new ArrayList<ILinkBuilder>(linkBuilders);
        Collections.sort(linkBuilderList, LinkBuilderComparator.INSTANCE);
        this.linkBuilders = Collections.unmodifiableSet(new LinkedHashSet<ILinkBuilder>(linkBuilderList));

        this.cacheManager = cacheManager;

        this.engineContextFactory = engineContextFactory;

        this.decoupledTemplateLogicResolver = decoupledTemplateLogicResolver;

        this.dialectSetConfiguration = DialectSetConfiguration.build(dialectConfigurations);
        this.textRepository = textRepository;

        // NOTE we are NOT initializing the templateManager here, but in #initialize()

        this.modelFactories = new ConcurrentHashMap<TemplateMode, IModelFactory>(6,1.0f);

    }


    /*
     * We need this method basically in order to initialize variables that have a dependency on the engine configuration
     * object itself, and therefore should not be instanced at the constructor.
     */
    void initialize() {
        this.templateManager = new TemplateManager(this);
    }



    public Set<ITemplateResolver> getTemplateResolvers() {
        return this.templateResolvers;
    }

    public Set<IMessageResolver> getMessageResolvers() {
        return this.messageResolvers;
    }

    public Set<ILinkBuilder> getLinkBuilders() {
        return this.linkBuilders;
    }



    public ICacheManager getCacheManager() {
        return this.cacheManager;
    }



    public IEngineContextFactory getEngineContextFactory() {
        return this.engineContextFactory;
    }



    public IDecoupledTemplateLogicResolver getDecoupledTemplateLogicResolver() {
        return this.decoupledTemplateLogicResolver;
    }




    public Set<DialectConfiguration> getDialectConfigurations() {
        return this.dialectSetConfiguration.getDialectConfigurations();
    }

    public Set<IDialect> getDialects() {
        return this.dialectSetConfiguration.getDialects();
    }

    public boolean isStandardDialectPresent() {
        return this.dialectSetConfiguration.isStandardDialectPresent();
    }

    public String getStandardDialectPrefix() {
        return this.dialectSetConfiguration.getStandardDialectPrefix();
    }

    public ITextRepository getTextRepository() {
        return this.textRepository;
    }


    public ElementDefinitions getElementDefinitions() {
        return this.dialectSetConfiguration.getElementDefinitions();
    }


    public AttributeDefinitions getAttributeDefinitions() {
        return this.dialectSetConfiguration.getAttributeDefinitions();
    }


    public Set<ITemplateBoundariesProcessor> getTemplateBoundariesProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getTemplateBoundariesProcessors(templateMode);
    }

    public Set<ICDATASectionProcessor> getCDATASectionProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getCDATASectionProcessors(templateMode);
    }

    public Set<ICommentProcessor> getCommentProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getCommentProcessors(templateMode);
    }

    public Set<IDocTypeProcessor> getDocTypeProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getDocTypeProcessors(templateMode);
    }

    public Set<IElementProcessor> getElementProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getElementProcessors(templateMode);
    }

    public Set<ITextProcessor> getTextProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getTextProcessors(templateMode);
    }

    public Set<IProcessingInstructionProcessor> getProcessingInstructionProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getProcessingInstructionProcessors(templateMode);
    }

    public Set<IXMLDeclarationProcessor> getXMLDeclarationProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getXMLDeclarationProcessors(templateMode);
    }


    public Set<IPreProcessor> getPreProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getPreProcessors(templateMode);
    }

    public Set<IPostProcessor> getPostProcessors(final TemplateMode templateMode) {
        return this.dialectSetConfiguration.getPostProcessors(templateMode);
    }



    public Map<String, Object> getExecutionAttributes() {
        return this.dialectSetConfiguration.getExecutionAttributes();
    }


    public IExpressionObjectFactory getExpressionObjectFactory() {
        return this.dialectSetConfiguration.getExpressionObjectFactory();
    }


    public TemplateManager getTemplateManager() {
        return this.templateManager;
    }


    public IModelFactory getModelFactory(final TemplateMode templateMode) {
        if (this.modelFactories.containsKey(templateMode)) {
            return this.modelFactories.get(templateMode);
        }
        // TODO The classes to be instanced for creating model factories should be configurable
        final IModelFactory modelFactory = new StandardModelFactory(this, templateMode);
        this.modelFactories.putIfAbsent(templateMode, modelFactory);
        return this.modelFactories.get(templateMode);
    }


    /**
     * Compares <tt>Integer</tt> types, taking into account possible <tt>null</tt>
     * values.  When <tt>null</tt>, then the return value will be such that the
     * other value will come first in a comparison.  If both values are <tt>null</tt>,
     * then they are effectively equal.
     * 
     * @param o1 The first value to compare.
     * @param o2 The second value to compare.
     * @return -1, 0, or 1 if the first value should come before, equal to, or
     *         after the second.
     */
    private static int nullSafeIntegerComparison(Integer o1, Integer o2) {
        return o1 != null ? o2 != null ? o1.compareTo(o2) : -1 : o2 != null ? 1 : 0;
    }


    private static final class TemplateResolverComparator implements Comparator<ITemplateResolver> {

        private static TemplateResolverComparator INSTANCE = new TemplateResolverComparator();

        TemplateResolverComparator() {
            super();
        }

        public int compare(final ITemplateResolver tr1, final ITemplateResolver tr2) {
            return nullSafeIntegerComparison(tr1.getOrder(), tr2.getOrder());
        }
    }


    private static final class MessageResolverComparator implements Comparator<IMessageResolver> {

        private static MessageResolverComparator INSTANCE = new MessageResolverComparator();

        MessageResolverComparator() {
            super();
        }

        public int compare(final IMessageResolver mr1, final IMessageResolver mr2) {
            return nullSafeIntegerComparison(mr1.getOrder(), mr2.getOrder());
        }
    }


    private static final class LinkBuilderComparator implements Comparator<ILinkBuilder> {

        private static LinkBuilderComparator INSTANCE = new LinkBuilderComparator();

        LinkBuilderComparator() {
            super();
        }

        public int compare(final ILinkBuilder mr1, final ILinkBuilder mr2) {
            return nullSafeIntegerComparison(mr1.getOrder(), mr2.getOrder());
        }
    }

}
