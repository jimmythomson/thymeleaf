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

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.model.IModelVisitor;
import org.thymeleaf.model.IXMLDeclaration;
import org.thymeleaf.text.ITextRepository;
import org.thymeleaf.util.TextUtils;
import org.thymeleaf.util.Validate;

/**
 *
 * @author Daniel Fern&aacute;ndez
 * @since 3.0.0
 * 
 */
final class XMLDeclaration extends AbstractTemplateEvent implements IXMLDeclaration, IEngineTemplateEvent {

    // XML Declaration nodes do not exist in text parsing, so we are safe expliciting markup structures here
    public static final String DEFAULT_KEYWORD = "xml";

    public static final String ATTRIBUTE_NAME_VERSION = "version";
    public static final String ATTRIBUTE_NAME_ENCODING = "encoding";
    public static final String ATTRIBUTE_NAME_STANDALONE = "standalone";

    private final ITextRepository textRepository;

    private String xmlDeclaration;
    private String keyword;
    private String version;
    private String encoding;
    private String standalone;


    /*
     * Objects of this class are meant to both be reused by the engine and also created fresh by the processors. This
     * should allow reducing the number of instances of this class to the minimum.
     *
     * The 'xmlDeclaration' property, which is computed from the other properties, should be computed lazily in order to avoid
     * unnecessary creation of Strings which would use more memory than needed.
     */


    // Meant to be called only from the template handler adapter
    XMLDeclaration(final ITextRepository textRepository) {
        super();
        this.textRepository = textRepository;
    }



    // Meant to be called only from the model factory
    XMLDeclaration(
            final ITextRepository textRepository,
            final String version,
            final String encoding,
            final String standalone) {
        super();
        this.textRepository = textRepository;
        initializeFromXmlDeclaration(DEFAULT_KEYWORD, version, encoding, standalone);
    }




    public String getKeyword() {
        return this.keyword;
    }

    public String getVersion() {
        return this.version;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public String getStandalone() {
        return this.standalone;
    }




    public String getXmlDeclaration() {

        if (this.xmlDeclaration == null) {

            final StringBuilder strBuilder = new StringBuilder();

            strBuilder.append("<?");
            strBuilder.append(this.keyword);
            if (this.version != null) {
                strBuilder.append(' ');
                strBuilder.append(XMLDeclaration.ATTRIBUTE_NAME_VERSION);
                strBuilder.append("=\"");
                strBuilder.append(this.version);
                strBuilder.append('"');
            }
            if (this.encoding != null) {
                strBuilder.append(' ');
                strBuilder.append(XMLDeclaration.ATTRIBUTE_NAME_ENCODING);
                strBuilder.append("=\"");
                strBuilder.append(this.encoding);
                strBuilder.append('"');
            }
            if (this.standalone != null) {
                strBuilder.append(' ');
                strBuilder.append(XMLDeclaration.ATTRIBUTE_NAME_STANDALONE);
                strBuilder.append("=\"");
                strBuilder.append(this.standalone);
                strBuilder.append('"');
            }
            strBuilder.append("?>");

            this.xmlDeclaration = this.textRepository.getText(strBuilder);

        }

        return this.xmlDeclaration;

    }




    public void setVersion(final String version) {
        initializeFromXmlDeclaration(this.keyword, version, this.encoding, this.standalone);
    }

    public void setEncoding(final String encoding) {
        initializeFromXmlDeclaration(this.keyword, this.version, encoding, this.standalone);
    }

    public void setStandalone(final String standalone) {
        initializeFromXmlDeclaration(this.keyword, this.version, this.encoding, standalone);
    }




    // Meant to be called only from within the engine - removes the need to validate compute the 'xmlDeclaration' field
    void reset(final String xmlDeclaration,
               final String keyword,
               final String version,
               final String encoding,
               final String standalone,
               final String templateName, final int line, final int col) {

        super.resetTemplateEvent(templateName, line, col);

        this.keyword = keyword;
        this.version = version;
        this.encoding = encoding;
        this.standalone = standalone;

        this.xmlDeclaration = xmlDeclaration;

    }



    private void initializeFromXmlDeclaration(
            final String keyword,
            final String version,
            final String encoding,
            final String standalone) {

        if (keyword == null || !TextUtils.equals(true, DEFAULT_KEYWORD, keyword)) {
            throw new IllegalArgumentException("XML Declaration keyword must be non-null and equal to '" + DEFAULT_KEYWORD + "'");
        }

        super.resetTemplateEvent(null, -1, -1);

        this.keyword = keyword;
        this.version = version;
        this.encoding = encoding;
        this.standalone = standalone;

        this.xmlDeclaration = null;

    }







    public void accept(final IModelVisitor visitor) {
        visitor.visit(this);
    }


    public void write(final Writer writer) throws IOException {
        Validate.notNull(writer, "Writer cannot be null");
        writer.write(getXmlDeclaration());
    }






    public XMLDeclaration cloneEvent() {
        final XMLDeclaration clone = new XMLDeclaration(this.textRepository);
        clone.resetAsCloneOf(this);
        return clone;
    }



    // Meant to be called only from within the engine
    void resetAsCloneOf(final XMLDeclaration original) {

        super.resetAsCloneOfTemplateEvent(original);
        this.xmlDeclaration = original.xmlDeclaration;
        this.keyword = original.keyword;
        this.version = original.version;
        this.encoding = original.encoding;
        this.standalone = original.standalone;

    }


    // Meant to be called only from within the engine
    static XMLDeclaration asEngineXMLDeclaration(
            final IEngineConfiguration configuration, final IXMLDeclaration xmlDeclaration, final boolean cloneAlways) {

        if (xmlDeclaration instanceof XMLDeclaration) {
            if (cloneAlways) {
                return ((XMLDeclaration) xmlDeclaration).cloneEvent();
            }
            return (XMLDeclaration) xmlDeclaration;
        }

        final XMLDeclaration newInstance = new XMLDeclaration(configuration.getTextRepository());
        newInstance.xmlDeclaration = xmlDeclaration.getXmlDeclaration();
        newInstance.keyword = xmlDeclaration.getKeyword();
        newInstance.version = xmlDeclaration.getVersion();
        newInstance.encoding = xmlDeclaration.getEncoding();
        newInstance.standalone = xmlDeclaration.getStandalone();
        newInstance.resetTemplateEvent(xmlDeclaration.getTemplateName(), xmlDeclaration.getLine(), xmlDeclaration.getCol());
        return newInstance;

    }





    @Override
    public final String toString() {
        return getXmlDeclaration();
    }

}
