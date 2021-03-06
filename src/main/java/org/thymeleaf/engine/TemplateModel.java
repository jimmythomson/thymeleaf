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
package org.thymeleaf.engine;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.ICDATASection;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IComment;
import org.thymeleaf.model.IDocType;
import org.thymeleaf.model.IElementAttributes;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelVisitor;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.IProcessingInstruction;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.model.ITemplateEnd;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.ITemplateStart;
import org.thymeleaf.model.IText;
import org.thymeleaf.model.IXMLDeclaration;
import org.thymeleaf.processor.element.IElementProcessor;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.Validate;


/**
 *
 * @author Daniel Fern&aacute;ndez
 * @since 3.0.0
 *
 */
public final class TemplateModel implements IModel {

    private final Model model;
    private final TemplateData templateData;



    // Package-protected constructor, because we don't want anyone creating these objects from outside the engine.
    // Specifically, they will only be created from the TemplateManager.
    // If a processor (be it standard or custom-made) wants to create a piece of model, that should be a Model
    // object, not this.
    TemplateModel(final IEngineConfiguration configuration, final TemplateData templateData) {
        
        super();
        
        Validate.notNull(configuration, "Engine Configuration cannot be null");
        Validate.notNull(templateData, "Template Resolution cannot be null");

        this.model = new Model(configuration, templateData.getTemplateMode());
        this.templateData = templateData;

    }


    public final TemplateData getTemplateData() {
        return this.templateData;
    }


    public final IEngineConfiguration getConfiguration() {
        return this.model.getConfiguration();
    }


    public final TemplateMode getTemplateMode() {
        return this.model.getTemplateMode();
    }



    public final int size() {
        return this.model.size();
    }


    public final ITemplateEvent get(final int pos) {
        return immutableEvent(this.model.get(pos));
    }


    /**
     * <p>
     *   This method is not part of the {@link IModel} interface.
     * </p>
     * <p>
     *   It is meant to return the type of an event without the need to create an
     *   immutable wrapper object.
     * </p>
     *
     * @param pos the position (zero-based) of the event to be retrieved.
     * @return the retrieved event.
     */
    public final Class<? extends ITemplateEvent> getEventType(final int pos) {
        final ITemplateEvent event = this.model.get(pos);
        return (event != null? event.getClass() : null);
    }


    public final void add(final ITemplateEvent event) {
        immutableModelException();
    }


    public final void insert(final int pos, final ITemplateEvent event) {
        immutableModelException();
    }


    public final void addModel(final IModel model) {
        immutableModelException();
    }


    public final void insertModel(final int pos, final IModel model) {
        immutableModelException();
    }


    public final void remove(final int pos) {
        immutableModelException();
    }


    public final void reset() {
        immutableModelException();
    }




    // We don't want anyone to have direct access to the underlying Model object from outside the engine.
    // This will effectively turn our ParsedFragmentModel into immutable (though not really) and therefore allow us
    // to confidently cache these objects without worrying that anyone can modify them
    final Model getInternalModel() {
        return this.model;
    }



    public final IModel cloneModel() {
        return new Model(this);
    }




    public final void write(final Writer writer) throws IOException {
        this.model.write(writer);
    }




    public void accept(final IModelVisitor visitor) {

        final int queueSize = this.model.size();
        for (int i = 0; i < queueSize; i++) {
            // We will execute the visitor on the Immutable events, that we need to create during the visit
            get(i).accept(visitor);
        }

    }



    @Override
    public final String toString() {
        return this.model.toString();
    }



    private static ITemplateEvent immutableEvent(final ITemplateEvent event) {

        if (event instanceof IText) {
            return new ImmutableText((IText)event);
        } else if (event instanceof IOpenElementTag) {
            return new ImmutableOpenElementTag((IOpenElementTag)event);
        } else if (event instanceof ICloseElementTag) {
            return new ImmutableCloseElementTag((ICloseElementTag)event);
        } else if (event instanceof IStandaloneElementTag) {
            return new ImmutableStandaloneElementTag((IStandaloneElementTag)event);
        } else if (event instanceof IDocType) {
            return new ImmutableDocType((IDocType)event);
        } else if (event instanceof IComment) {
            return new ImmutableComment((IComment)event);
        } else if (event instanceof ICDATASection) {
            return new ImmutableCDATASection((ICDATASection)event);
        } else if (event instanceof IXMLDeclaration) {
            return new ImmutableXMLDeclaration((IXMLDeclaration)event);
        } else if (event instanceof IProcessingInstruction) {
            return new ImmutableProcessingInstruction((IProcessingInstruction)event);
        } else if (event instanceof ITemplateStart) {
            return new ImmutableTemplateStart((ITemplateStart)event);
        } else if (event instanceof ITemplateEnd) {
            return new ImmutableTemplateEnd((ITemplateEnd)event);
        } else {
            throw new TemplateProcessingException(
                    "Cannot process as immutable event of type: " + event.getClass().getName());
        }

    }




    private static void immutableModelException() {
        throw new UnsupportedOperationException(
                "Modifications are not allowed on immutable model objects. This model object is an immutable " +
                        "implementation of the " + IModel.class.getName() + " interface, and no modifications are allowed in " +
                        "order to keep cache consistency and improve performance. To modify model events, convert first your " +
                        "immutable model object to a mutable one by means of the " + IModel.class.getName() + "#cloneModel() method");
    }


    private static void immutableEventException() {
        throw new UnsupportedOperationException(
                "Modifications are not allowed on immutable events. This event object was returned by an immutable " +
                        "implementation of the " + IModel.class.getName() + " interface, and no modifications are allowed in " +
                        "order to keep cache consistency and improve performance. To modify model events, convert first your " +
                        "immutable model object to a mutable one by means of the " + IModel.class.getName() + "#cloneModel() method");
    }



    private static abstract class AbstractImmutableTemplateEvent implements ITemplateEvent {

        private final ITemplateEvent wrapped;

        protected AbstractImmutableTemplateEvent(final ITemplateEvent wrapped) {
            super();
            this.wrapped = wrapped;
        }

        public final boolean hasLocation() {
            return this.wrapped.hasLocation();
        }

        public final String getTemplateName() {
            return this.wrapped.getTemplateName();
        }

        public final int getLine() {
            return this.wrapped.getLine();
        }

        public final int getCol() {
            return this.wrapped.getCol();
        }

        public final void write(final Writer writer) throws IOException {
            this.wrapped.write(writer);
        }

    }


    private static final class ImmutableTemplateStart
            extends AbstractImmutableTemplateEvent implements ITemplateStart {

        private final ITemplateStart wrapped;

        private ImmutableTemplateStart(final ITemplateStart wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public long getStartTimeNanos() {
            return this.wrapped.getStartTimeNanos();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public ITemplateStart cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableTemplateEnd
            extends AbstractImmutableTemplateEvent implements ITemplateEnd {

        private final ITemplateEnd wrapped;

        private ImmutableTemplateEnd(final ITemplateEnd wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public long getEndTimeNanos() {
            return this.wrapped.getEndTimeNanos();
        }

        public long getTotalTimeNanos() {
            return this.wrapped.getTotalTimeNanos();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public ITemplateEnd cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableText
            extends AbstractImmutableTemplateEvent implements IText {

        private final IText wrapped;

        private ImmutableText(final IText wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public String getText() {
            return this.wrapped.getText();
        }

        public int length() {
            return this.wrapped.length();
        }

        public char charAt(final int index) {
            return this.wrapped.charAt(index);
        }

        public CharSequence subSequence(final int start, final int end) {
            return this.wrapped.subSequence(start, end);
        }

        public void setText(final CharSequence text) {
            immutableEventException();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public IText cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableCDATASection
            extends AbstractImmutableTemplateEvent implements ICDATASection {

        private final ICDATASection wrapped;

        private ImmutableCDATASection(final ICDATASection wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public int length() {
            return this.wrapped.length();
        }

        public char charAt(final int index) {
            return this.wrapped.charAt(index);
        }

        public CharSequence subSequence(final int start, final int end) {
            return this.wrapped.subSequence(start, end);
        }

        public String getCDATASection() {
            return this.wrapped.getCDATASection();
        }

        public String getContent() {
            return this.wrapped.getContent();
        }

        public void setContent(final CharSequence content) {
            immutableEventException();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public ICDATASection cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableComment
            extends AbstractImmutableTemplateEvent implements IComment {

        private final IComment wrapped;

        private ImmutableComment(final IComment wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public int length() {
            return this.wrapped.length();
        }

        public char charAt(final int index) {
            return this.wrapped.charAt(index);
        }

        public CharSequence subSequence(final int start, final int end) {
            return this.wrapped.subSequence(start, end);
        }

        public String getComment() {
            return this.wrapped.getComment();
        }

        public String getContent() {
            return this.wrapped.getContent();
        }

        public void setContent(final CharSequence content) {
            immutableEventException();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public IComment cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableDocType
            extends AbstractImmutableTemplateEvent implements IDocType {

        private final IDocType wrapped;

        private ImmutableDocType(final IDocType wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public String getKeyword() {
            return this.wrapped.getKeyword();
        }

        public String getElementName() {
            return this.wrapped.getElementName();
        }

        public String getType() {
            return this.wrapped.getType();
        }

        public String getPublicId() {
            return this.wrapped.getPublicId();
        }

        public String getSystemId() {
            return this.wrapped.getSystemId();
        }

        public String getInternalSubset() {
            return this.wrapped.getInternalSubset();
        }

        public String getDocType() {
            return this.wrapped.getDocType();
        }

        public void setKeyword(final String keyword) {
            immutableEventException();
        }

        public void setElementName(final String elementName) {
            immutableEventException();
        }

        public void setToHTML5() {
            immutableEventException();
        }

        public void setIDs(final String publicId, final String systemId) {
            immutableEventException();
        }

        public void setIDs(final String type, final String publicId, final String systemId) {
            immutableEventException();
        }

        public void setInternalSubset(final String internalSubset) {
            immutableEventException();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public IDocType cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableProcessingInstruction
            extends AbstractImmutableTemplateEvent implements IProcessingInstruction {

        private final IProcessingInstruction wrapped;

        private ImmutableProcessingInstruction(final IProcessingInstruction wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public String getTarget() {
            return this.wrapped.getTarget();
        }

        public String getContent() {
            return this.wrapped.getContent();
        }

        public String getProcessingInstruction() {
            return this.wrapped.getProcessingInstruction();
        }

        public void setTarget(final String target) {
            immutableEventException();
        }

        public void setContent(final String content) {
            immutableEventException();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public IProcessingInstruction cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableXMLDeclaration
            extends AbstractImmutableTemplateEvent implements IXMLDeclaration {

        private final IXMLDeclaration wrapped;

        private ImmutableXMLDeclaration(final IXMLDeclaration wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public String getKeyword() {
            return this.wrapped.getKeyword();
        }

        public String getVersion() {
            return this.wrapped.getVersion();
        }

        public String getEncoding() {
            return this.wrapped.getEncoding();
        }

        public String getStandalone() {
            return this.wrapped.getStandalone();
        }

        public String getXmlDeclaration() {
            return this.wrapped.getXmlDeclaration();
        }

        public void setVersion(final String version) {
            immutableEventException();
        }

        public void setEncoding(final String encoding) {
            immutableEventException();
        }

        public void setStandalone(final String standalone) {
            immutableEventException();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public IXMLDeclaration cloneEvent() {
            immutableEventException();
            return null;
        }

    }


    private static final class ImmutableElementAttributes implements IElementAttributes {

        private final IElementAttributes wrapped;

        private ImmutableElementAttributes(final IElementAttributes wrapped) {
            super();
            this.wrapped = wrapped;
        }

        public int size() {
            return this.wrapped.size();
        }

        public List<String> getAllCompleteNames() {
            return this.wrapped.getAllCompleteNames();
        }

        public List<AttributeName> getAllAttributeNames() {
            return this.wrapped.getAllAttributeNames();
        }

        public boolean hasAttribute(final String completeName) {
            return this.wrapped.hasAttribute(completeName);
        }

        public boolean hasAttribute(final String prefix, final String name) {
            return this.wrapped.hasAttribute(prefix, name);
        }

        public boolean hasAttribute(final AttributeName attributeName) {
            return this.wrapped.hasAttribute(attributeName);
        }

        public String getCompleteName(final String completeName) {
            return this.wrapped.getCompleteName(completeName);
        }

        public String getCompleteName(final String prefix, final String name) {
            return this.wrapped.getCompleteName(prefix, name);
        }

        public String getCompleteName(final AttributeName attributeName) {
            return this.wrapped.getCompleteName(attributeName);
        }

        public String getValue(final String completeName) {
            return this.wrapped.getValue(completeName);
        }

        public String getValue(final String prefix, final String name) {
            return this.wrapped.getValue(prefix, name);
        }

        public String getValue(final AttributeName attributeName) {
            return this.wrapped.getValue(attributeName);
        }

        public AttributeDefinition getAttributeDefinition(final String completeName) {
            return this.wrapped.getAttributeDefinition(completeName);
        }

        public AttributeDefinition getAttributeDefinition(final String prefix, final String name) {
            return this.wrapped.getAttributeDefinition(prefix, name);
        }

        public AttributeDefinition getAttributeDefinition(final AttributeName attributeName) {
            return this.wrapped.getAttributeDefinition(attributeName);
        }

        public ValueQuotes getValueQuotes(final String completeName) {
            return this.wrapped.getValueQuotes(completeName);
        }

        public ValueQuotes getValueQuotes(final String prefix, final String name) {
            return this.wrapped.getValueQuotes(prefix, name);
        }

        public ValueQuotes getValueQuotes(final AttributeName attributeName) {
            return this.wrapped.getValueQuotes(attributeName);
        }

        public boolean hasLocation(final String completeName) {
            return this.wrapped.hasLocation(completeName);
        }

        public boolean hasLocation(final String prefix, final String name) {
            return this.wrapped.hasAttribute(prefix, name);
        }

        public boolean hasLocation(final AttributeName attributeName) {
            return this.wrapped.hasLocation(attributeName);
        }

        public int getLine(final String completeName) {
            return this.wrapped.getLine(completeName);
        }

        public int getLine(final String prefix, final String name) {
            return this.wrapped.getLine(prefix, name);
        }

        public int getLine(final AttributeName attributeName) {
            return this.wrapped.getLine(attributeName);
        }

        public int getCol(final String completeName) {
            return this.wrapped.getCol(completeName);
        }

        public int getCol(final String prefix, final String name) {
            return this.wrapped.getCol(prefix, name);
        }

        public int getCol(final AttributeName attributeName) {
            return this.wrapped.getCol(attributeName);
        }

        public void clearAll() {
            immutableEventException();
        }

        public void setAttribute(final String completeName, final String value) {
            immutableEventException();
        }

        public void setAttribute(final String completeName, final String value, final ValueQuotes valueQuotes) {
            immutableEventException();
        }

        public void replaceAttribute(final AttributeName oldName, final String completeNewName, final String value) {
            immutableEventException();
        }

        public void replaceAttribute(final AttributeName oldName, final String completeNewName, final String value, final ValueQuotes valueQuotes) {
            immutableEventException();
        }

        public void removeAttribute(final String prefix, final String name) {
            immutableEventException();
        }

        public void removeAttribute(final String completeName) {
            immutableEventException();
        }

        public void removeAttribute(final AttributeName attributeName) {
            immutableEventException();
        }

        public void write(final Writer writer) throws IOException {
            immutableEventException();
        }

    }



    private static abstract class AbstractImmutableElementTag extends AbstractImmutableTemplateEvent implements IElementTag {

        private final IElementTag wrapped;

        protected AbstractImmutableElementTag(final IElementTag wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public ElementDefinition getElementDefinition() {
            return this.wrapped.getElementDefinition();
        }

        public String getElementName() {
            return this.wrapped.getElementName();
        }

    }



    private static abstract class AbstractImmutableProcessableElementTag
            extends AbstractImmutableElementTag implements IProcessableElementTag {

        private final IProcessableElementTag wrapped;

        protected AbstractImmutableProcessableElementTag(final IProcessableElementTag wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public IElementAttributes getAttributes() {
            return new ImmutableElementAttributes(this.wrapped.getAttributes());
        }

        public void precomputeAssociatedProcessors() {
            immutableEventException();
        }

        public boolean hasAssociatedProcessors() {
            return this.wrapped.hasAssociatedProcessors();
        }

        public List<IElementProcessor> getAssociatedProcessorsInOrder() {
            return this.wrapped.getAssociatedProcessorsInOrder();
        }

    }



    private static final class ImmutableStandaloneElementTag
            extends AbstractImmutableProcessableElementTag implements IStandaloneElementTag {

        private final IStandaloneElementTag wrapped;

        private ImmutableStandaloneElementTag(final IStandaloneElementTag wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public boolean isMinimized() {
            return this.wrapped.isMinimized();
        }

        public void setMinimized(final boolean minimized) {
            immutableEventException();
        }

        public boolean isSynthetic() {
            return this.wrapped.isSynthetic();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public IStandaloneElementTag cloneEvent() {
            immutableEventException();
            return null;
        }

    }



    private static final class ImmutableOpenElementTag
            extends AbstractImmutableProcessableElementTag implements IOpenElementTag {

        private final IOpenElementTag wrapped;

        private ImmutableOpenElementTag(final IOpenElementTag wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public boolean isSynthetic() {
            return this.wrapped.isSynthetic();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public IOpenElementTag cloneEvent() {
            immutableEventException();
            return null;
        }

    }



    private static final class ImmutableCloseElementTag
            extends AbstractImmutableElementTag implements ICloseElementTag {

        private final ICloseElementTag wrapped;

        private ImmutableCloseElementTag(final ICloseElementTag wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }

        public boolean isSynthetic() {
            return this.wrapped.isSynthetic();
        }

        public boolean isUnmatched() {
            return this.wrapped.isUnmatched();
        }

        public void accept(final IModelVisitor visitor) {
            visitor.visit(this);
        }

        public ICloseElementTag cloneEvent() {
            immutableEventException();
            return null;
        }

    }


}