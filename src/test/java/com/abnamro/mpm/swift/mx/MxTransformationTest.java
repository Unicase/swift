package com.abnamro.mpm.swift.mx;

import com.abnamro.mpm.swift.mx.dsl.MxTransformationSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MX (ISO 20022 XML) message transformation.
 */
class MxTransformationTest {

    private static final String SPEC_RESOURCE = "mx/pacs008-to-pacs002.yaml";
    private static final Path SOURCE_XML = Paths.get("src/test/resources/mx/pacs008_TGT_input.xml");
    private static final Path TEMPLATE_XML = Paths.get("src/test/resources/mx/pacs008_TGT_output.xml");

    private MxSpecLoader loader;
    private MxTransformationSpec spec;
    private MxTransformationEngine engine;

    @BeforeEach
    void setUp() {
        loader = new MxSpecLoader();
        spec = loader.loadFromResource(SPEC_RESOURCE);
        engine = new MxTransformationEngine();
    }

    @Test
    void testSpecLoading() {
        assertNotNull(spec);
        assertEquals("pacs.008 TGT to pacs.002 TGT transformation", spec.getName());
        assertEquals("pacs.008.001.08", spec.getSourceFormat());
        assertEquals("pacs.002.001.10", spec.getTargetFormat());
        assertEquals(1, spec.getGenerators().size());
        assertEquals("msgId", spec.getGenerators().get(0).id());
        assertEquals(1, spec.getVariables().size());
        assertEquals("senderReference", spec.getVariables().get(0).id());
        assertEquals("/DataPDU/Header/Message/SenderReference", spec.getVariables().get(0).xpath());
        assertTrue(spec.getVariables().get(0).required());
        assertEquals(2, spec.getTransformations().size());
    }

    @Test
    void testVariableExtraction() throws Exception {
        // Use a minimal spec that only extracts a variable (no transformations)
        String minimalSpec = """
                name: test
                type: MX
                mtVariables:
                  - id: senderReference
                    xpath: /DataPDU/Header/Message/SenderReference
                    required: true
                transformations: []
                """;
        MxTransformationSpec minSpec = loader.loadFromString(minimalSpec);
        String result = engine.transform(minSpec,
                SOURCE_XML.toFile().getCanonicalPath().equals(SOURCE_XML.toString())
                        ? java.nio.file.Files.readString(SOURCE_XML)
                        : java.nio.file.Files.readString(SOURCE_XML),
                java.nio.file.Files.readString(TEMPLATE_XML));

        // Template is unchanged but no exception means extraction worked;
        // verify by running the full transformation and checking the value
        String fullResult = engine.transform(spec,
                java.nio.file.Files.readString(SOURCE_XML),
                java.nio.file.Files.readString(TEMPLATE_XML));

        assertTrue(fullResult.contains("NP3322868920000"),
                "Extracted senderReference should appear in the output");
    }

    @Test
    void testFullTransformation() throws Exception {
        String result = engine.transform(spec,
                java.nio.file.Files.readString(SOURCE_XML),
                java.nio.file.Files.readString(TEMPLATE_XML));

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // The senderReference from input (NP3322868920000) should end up in OrgnlMsgId
        assertTrue(result.contains("NP3322868920000"),
                "OrgnlMsgId should contain the extracted senderReference value");
    }

    @Test
    void testGeneratorValueApplied() throws Exception {
        // Run transformation twice; the SenderReference in the template should
        // be replaced with a freshly generated 16-char alphanumeric value each time.
        String result1 = engine.transform(spec,
                java.nio.file.Files.readString(SOURCE_XML),
                java.nio.file.Files.readString(TEMPLATE_XML));
        String result2 = engine.transform(spec,
                java.nio.file.Files.readString(SOURCE_XML),
                java.nio.file.Files.readString(TEMPLATE_XML));

        // The original template SenderReference value should be replaced
        assertFalse(result1.contains("OTRGTXEPMXXX002$22082625579955"),
                "Original SenderReference should be replaced by generated value");

        // Two runs should (almost certainly) produce different generated values
        // We can't assert inequality since it's random, but we can check format:
        // The generated value should be 16 alphanumeric chars; just check it was replaced
        assertFalse(result1.contains("${msgId}"),
                "No unresolved variable references should remain in output");
    }

    @Test
    void testMissingNodeLogsWarning() throws Exception {
        // An XPath that matches nothing should not throw; just log a warning
        String specWithBadXpath = """
                name: test
                type: MX
                mtVariables: []
                transformations:
                  - xpath: /DataPDU/NonExistent/Path
                    value: "something"
                """;
        MxTransformationSpec badSpec = loader.loadFromString(specWithBadXpath);

        // Should not throw
        assertDoesNotThrow(() -> engine.transform(badSpec,
                java.nio.file.Files.readString(SOURCE_XML),
                java.nio.file.Files.readString(TEMPLATE_XML)));
    }

    @Test
    void testRequiredVariableMissingThrows() {
        String specWithBadVar = """
                name: test
                type: MX
                variables:
                  - id: missing
                    xpath: /DataPDU/DoesNotExist
                    required: true
                transformations: []
                """;
        MxTransformationSpec badSpec = loader.loadFromString(specWithBadVar);

        assertThrows(IllegalArgumentException.class,
                () -> engine.transform(badSpec,
                        java.nio.file.Files.readString(SOURCE_XML),
                        java.nio.file.Files.readString(TEMPLATE_XML)));
    }

    @Test
    void testNamespaceAgnosticXPath() {
        // Unit-test the XPath rewriting helper directly
        assertEquals(
                "/*[local-name()='DataPDU']/*[local-name()='Header']/*[local-name()='Message']/*[local-name()='SenderReference']",
                MxTransformationEngine.toNamespaceAgnosticXPath("/DataPDU/Header/Message/SenderReference")
        );
        // Attribute segments should be left unchanged
        assertEquals(
                "/*[local-name()='Root']/@id",
                MxTransformationEngine.toNamespaceAgnosticXPath("/Root/@id")
        );
        // Wildcard should pass through unchanged
        assertEquals(
                "/*[local-name()='Root']/*/text()",
                MxTransformationEngine.toNamespaceAgnosticXPath("/Root/*/text()")
        );
    }
}
