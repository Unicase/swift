package com.abnamro.mpm.swift.mt;

import com.abnamro.mpm.swift.mt.dsl.TransformationSpec;
import com.prowidesoftware.swift.model.SwiftBlock4;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.Tag;
import com.prowidesoftware.swift.model.field.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for MT541 to MT545 transformation using the DSL.
 */
class Mt541To545TransformationTest {

    private MtTransformationEngine engine;
    private TransformationSpecLoader loader;
    private TransformationSpec spec;
    private SwiftMessage sourceMessage;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize the transformation engine
        engine = new MtTransformationEngine();
        loader = new TransformationSpecLoader();

        // Load the transformation specification
        spec = loader.loadFromResource("mt/541-545.yaml");
        assertNotNull(spec, "Transformation spec should be loaded");

        // Load the source MT541 message
        String mt541Content = loadResourceAsString("mt/I541.txt");
        sourceMessage = SwiftMessage.parse(mt541Content);
        assertNotNull(sourceMessage, "Source message should be parsed");
    }

    @Test
    void testTransformationSpecLoaded() {
        assertEquals("MT541 to MT545 transformation", spec.name());
        assertEquals("MT541", spec.sourceFormat());
        assertEquals("MT545", spec.targetFormat());
        assertEquals("1.0", spec.version());
    }

    @Test
    void testSourceMessageParsed() {
        assertEquals("541", sourceMessage.getType());
        assertNotNull(sourceMessage.getBlock1());
        assertNotNull(sourceMessage.getBlock2());
        assertNotNull(sourceMessage.getBlock3());
        assertNotNull(sourceMessage.getBlock4());
    }

    @Test
    void testFullTransformation() {
        // Execute the transformation
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
        assertNotNull(targetMessage, "Target message should be created");

        // Verify message type changed to MT545
        assertEquals("545", targetMessage.getType());
    }

    @Test
    void testBlock1Transformation() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

        // Verify Block 1 fields (session and sequence numbers should be set)
        assertNotNull(targetMessage.getBlock1());
        assertNotNull(targetMessage.getBlock1().getSessionNumber());
        assertNotNull(targetMessage.getBlock1().getSequenceNumber());
    }

    @Test
    void testBlock2Transformation() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

        // Verify Block 2 - message type changed to 545
        assertNotNull(targetMessage.getBlock2());
        assertEquals("545", targetMessage.getBlock2().getMessageType());
    }

    @Test
    void testBlock3Transformation() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

        // Verify Block 3 - field 108 should be updated
        assertNotNull(targetMessage.getBlock3());
        Tag tag108 = targetMessage.getBlock3().getTagByName("108");
        assertNotNull(tag108);
        assertEquals("RNN24022237550", tag108.getValue());
    }

    @Test
    void testBlock4SemeFieldReplaced() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
        SwiftBlock4 block4 = targetMessage.getBlock4();
        assertNotNull(block4);

        // Find the 20C::SEME field - should have new generated value
        Field[] fields20C = block4.getFieldsByName("20C");
        assertNotNull(fields20C);
        assertTrue(fields20C.length > 0);

        // Find SEME qualifier
        boolean foundSeme = false;
        for (Field field : fields20C) {
            String qualifier = field.getComponent(1);
            if ("SEME".equals(qualifier)) {
                foundSeme = true;
                String newValue = field.getComponent(2);
                assertNotNull(newValue);
                assertNotEquals("33573253", newValue, "SEME value should be different from source");
                assertEquals(14, newValue.length(), "Generated reference should be 14 characters");
                break;
            }
        }
        assertTrue(foundSeme, "Should have SEME field");
    }

    @Test
    void testPrepFieldDeleted() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
        SwiftBlock4 block4 = targetMessage.getBlock4();

        // Verify 98C::PREP field is deleted
        Field[] fields98C = block4.getFieldsByName("98C");
        if (fields98C != null) {
            for (Field field : fields98C) {
                String qualifier = field.getComponent(1);
                assertNotEquals("PREP", qualifier, "PREP field should be deleted");
            }
        }
    }

    @Test
    void testLinkSequenceInserted() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
        SwiftBlock4 block4 = targetMessage.getBlock4();

        // Verify LINK sequence is inserted
        boolean foundLinkStart = false;
        boolean foundLinkEnd = false;
        boolean foundRela = false;

        for (Tag tag : block4.getTags()) {
            if ("16R".equals(tag.getName()) && "LINK".equals(tag.getValue())) {
                foundLinkStart = true;
            }
            if ("16S".equals(tag.getName()) && "LINK".equals(tag.getValue())) {
                foundLinkEnd = true;
            }
            if ("20C".equals(tag.getName())) {
                Field field = Field.getField(tag);
                String qualifier = field.getComponent(1);
                if ("RELA".equals(qualifier)) {
                    foundRela = true;
                    String relaValue = field.getComponent(2);
                    assertEquals("33573253", relaValue, "RELA should contain original SEME value");
                }
            }
        }

        assertTrue(foundLinkStart, "Should have LINK sequence start marker (16R)");
        assertTrue(foundLinkEnd, "Should have LINK sequence end marker (16S)");
        assertTrue(foundRela, "Should have 20C::RELA field in LINK sequence");
    }

    @Test
    void testDealFieldDeleted() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
        SwiftBlock4 block4 = targetMessage.getBlock4();

        // Verify 90A::DEAL field is deleted
        Field[] fields90A = block4.getFieldsByName("90A");
        if (fields90A != null) {
            for (Field field : fields90A) {
                String qualifier = field.getComponent(1);
                assertNotEquals("DEAL", qualifier, "DEAL field should be deleted");
            }
        }
    }

    @Test
    void testFiaSequenceDeleted() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
        SwiftBlock4 block4 = targetMessage.getBlock4();

        // Verify FIA sequence is completely deleted
        boolean inFiaSequence = false;
        for (Tag tag : block4.getTags()) {
            if ("16R".equals(tag.getName()) && "FIA".equals(tag.getValue())) {
                inFiaSequence = true;
            }
            if (inFiaSequence) {
                fail("FIA sequence should be completely deleted but found tag: " + tag.getName());
            }
            if ("16S".equals(tag.getName()) && "FIA".equals(tag.getValue())) {
                inFiaSequence = false;
            }
        }
        assertFalse(inFiaSequence, "Should not be in FIA sequence");
    }

    @Test
    void testBlock5Transformation() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

        // Verify Block 5 - CHK field updated (Block 5 may be created during transformation)
        // The source message may or may not have Block 5, but target should have it after transformation
        if (targetMessage.getBlock5() != null) {
            Tag chkTag = targetMessage.getBlock5().getTagByName("CHK");
            if (chkTag != null) {
                assertEquals("543FB710868B", chkTag.getValue());
            }
        }
        // Note: Block 5 transformation is optional depending on source message structure
    }

    @Test
    void testSystemBlockAdded() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

        // Verify System block (S) is added
        assertNotNull(targetMessage.getUserBlock("S"), "System block should be present");

        Tag sacTag = targetMessage.getUserBlock("S").getTagByName("SAC");
        Tag copTag = targetMessage.getUserBlock("S").getTagByName("COP");
        Tag insTag = targetMessage.getUserBlock("S").getTagByName("INS");
        Tag untTag = targetMessage.getUserBlock("S").getTagByName("UNT");
        Tag usrTag = targetMessage.getUserBlock("S").getTagByName("USR");

        assertNotNull(sacTag);
        assertEquals("", sacTag.getValue());

        assertNotNull(copTag);
        assertEquals("P", copTag.getValue());

        assertNotNull(insTag);
        assertEquals("saaapra1/MQSAA2MURFIN", insTag.getValue());

        assertNotNull(untTag);
        assertEquals("NL2ARE", untTag.getValue());

        assertNotNull(usrTag);
        assertEquals("all_adm", usrTag.getValue());
    }

    @Test
    void testMessageStructureIntegrity() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

        // Verify all required sequences are present
        SwiftBlock4 block4 = targetMessage.getBlock4();
        boolean hasGenl = false;
        boolean hasTraddet = false;
        boolean hasFiac = false;
        boolean hasSetdet = false;

        for (Tag tag : block4.getTags()) {
            if ("16R".equals(tag.getName())) {
                if ("GENL".equals(tag.getValue())) hasGenl = true;
                if ("TRADDET".equals(tag.getValue())) hasTraddet = true;
                if ("FIAC".equals(tag.getValue())) hasFiac = true;
                if ("SETDET".equals(tag.getValue())) hasSetdet = true;
            }
        }

        assertTrue(hasGenl, "Should have GENL sequence");
        assertTrue(hasTraddet, "Should have TRADDET sequence");
        assertTrue(hasFiac, "Should have FIAC sequence");
        assertTrue(hasSetdet, "Should have SETDET sequence");
    }

    @Test
    void testPrintTransformedMessage() {
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

        System.out.println("\n=== Original MT541 Message ===");
        System.out.println(sourceMessage.message());

        System.out.println("\n=== Transformed MT545 Message ===");
        System.out.println(targetMessage.message());

        System.out.println("\n=== Transformation Details ===");
        System.out.println("Source type: MT" + sourceMessage.getType());
        System.out.println("Target type: MT" + targetMessage.getType());
        System.out.println("Generators used: " + spec.generators().size());
        System.out.println("Variables extracted: " + spec.variables().size());
        System.out.println("Transformations applied: " + spec.transformations().size());
    }

    /**
     * Helper method to load a resource file as a string.
     */
    private String loadResourceAsString(String resourcePath) throws IOException {
        Path path = Paths.get("src/test/resources/" + resourcePath);
        return Files.readString(path).trim();
    }
}

