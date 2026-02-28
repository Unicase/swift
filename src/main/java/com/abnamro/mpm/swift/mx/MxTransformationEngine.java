package com.abnamro.mpm.swift.mx;

import com.abnamro.mpm.swift.mt.helpers.FieldResolver;
import com.abnamro.mpm.swift.mt.helpers.GeneratorService;
import com.abnamro.mpm.swift.mx.dsl.MxTransformation;
import com.abnamro.mpm.swift.mx.dsl.MxTransformationSpec;
import com.abnamro.mpm.swift.mx.dsl.MxVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Engine that executes MX (ISO 20022 XML) message transformations.
 *
 * <p>Uses namespace-agnostic XPath evaluation so user-provided XPaths like
 * {@code /DataPDU/Header/Message/SenderReference} work regardless of whether
 * the XML uses default namespaces or prefixed namespaces.
 */
public class MxTransformationEngine {

    private static final Logger log = LoggerFactory.getLogger(MxTransformationEngine.class);

    // Matches a plain XML element name: starts with letter, followed by letters/digits/hyphens/underscores
    private static final Pattern PLAIN_ELEMENT = Pattern.compile("[a-zA-Z][a-zA-Z0-9_\\-]*");

    private final GeneratorService generatorService;
    private final FieldResolver fieldResolver;

    public MxTransformationEngine() {
        this.generatorService = new GeneratorService();
        this.fieldResolver = new FieldResolver();
    }

    /**
     * Execute the transformation and return the resulting XML as a string.
     *
     * @param spec           The MX transformation specification
     * @param sourceXmlPath  Path to the source XML file (variables are extracted from here)
     * @param templateXmlPath Path to the target template XML file (transformations are applied here)
     * @return Transformed XML as a string
     */
    public String transform(MxTransformationSpec spec, Path sourceXmlPath, Path templateXmlPath) throws Exception {
        String sourceXml = Files.readString(sourceXmlPath);
        String templateXml = Files.readString(templateXmlPath);
        return transform(spec, sourceXml, templateXml);
    }

    /**
     * Execute the transformation and return the resulting XML as a string.
     *
     * @param spec        The MX transformation specification
     * @param sourceXml   Source XML content (variables are extracted from here)
     * @param templateXml Target template XML content (transformations are applied here)
     * @return Transformed XML as a string
     */
    public String transform(MxTransformationSpec spec, String sourceXml, String templateXml) throws Exception {
        // Generate dynamic values
        Map<String, String> generatedValues = generatorService.generateAll(spec.generators());
        log.debug("Generated {} values", generatedValues.size());

        // Parse source XML (namespace-aware to preserve document fidelity)
        Document sourceDoc = parseXml(sourceXml);

        // Extract variables from source
        Map<String, String> variableValues = extractVariables(spec, sourceDoc);
        log.debug("Extracted {} variables", variableValues.size());

        // Merge into single context
        Map<String, String> context = new HashMap<>();
        context.putAll(generatedValues);
        context.putAll(variableValues);

        // Parse template XML
        Document templateDoc = parseXml(templateXml);

        // Apply transformations to template
        XPath xpathEval = XPathFactory.newInstance().newXPath();
        for (MxTransformation transformation : spec.transformations()) {
            applyTransformation(transformation, templateDoc, context, xpathEval);
        }

        // Serialize template DOM back to string
        return serializeXml(templateDoc);
    }

    private Map<String, String> extractVariables(MxTransformationSpec spec, Document sourceDoc) throws Exception {
        Map<String, String> values = new HashMap<>();
        XPath xpathEval = XPathFactory.newInstance().newXPath();

        for (MxVariable variable : spec.variables()) {
            String agnosticXpath = toNamespaceAgnosticXPath(variable.xpath());
            Node node = (Node) xpathEval.evaluate(agnosticXpath, sourceDoc, XPathConstants.NODE);

            if (node == null) {
                if (variable.required()) {
                    throw new IllegalArgumentException(
                            "Required variable '" + variable.id() + "' not found at XPath: " + variable.xpath());
                }
                log.warn("Optional variable '{}' not found at XPath: {}", variable.id(), variable.xpath());
                continue;
            }

            String value = node.getTextContent();
            values.put(variable.id(), value);
            log.debug("Extracted variable '{}' = '{}'", variable.id(), value);
        }

        return values;
    }

    private void applyTransformation(MxTransformation transformation, Document doc,
                                     Map<String, String> context, XPath xpathEval) throws Exception {
        String resolvedValue = fieldResolver.resolveValue(transformation.value(), context);
        String agnosticXpath = toNamespaceAgnosticXPath(transformation.xpath());

        Node node = (Node) xpathEval.evaluate(agnosticXpath, doc, XPathConstants.NODE);
        if (node == null) {
            log.warn("XPath matched no node in template, skipping: {}", transformation.xpath());
            return;
        }

        node.setTextContent(resolvedValue);
        log.debug("Set '{}' = '{}'", transformation.xpath(), resolvedValue);
    }

    /**
     * Converts a simple slash-separated XPath to a namespace-agnostic form using local-name().
     *
     * <p>Example: {@code /DataPDU/Header/Message/SenderReference}
     * becomes {@code /*[local-name()='DataPDU']/*[local-name()='Header']/*[local-name()='Message']/*[local-name()='SenderReference']}
     *
     * <p>Non-plain-element segments (attributes {@code @attr}, wildcards {@code *},
     * axis steps {@code .} / {@code ..}, function calls, predicates) are left unchanged.
     */
    static String toNamespaceAgnosticXPath(String xpath) {
        if (xpath == null || xpath.isEmpty()) {
            return xpath;
        }

        StringBuilder result = new StringBuilder();
        String[] segments = xpath.split("/", -1);

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            if (i > 0) {
                result.append('/');
            }

            if (segment.isEmpty() || !PLAIN_ELEMENT.matcher(segment).matches()) {
                // Empty (leading slash), attribute, wildcard, function, predicate-only etc.
                result.append(segment);
            } else {
                result.append("*[local-name()='").append(segment).append("']");
            }
        }

        return result.toString();
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String serializeXml(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        // Preserve the original XML declaration from the document
        String xmlVersion = doc.getXmlVersion();
        if (xmlVersion != null) {
            transformer.setOutputProperty(OutputKeys.VERSION, xmlVersion);
        }

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
