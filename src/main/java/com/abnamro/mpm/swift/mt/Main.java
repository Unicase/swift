package com.abnamro.mpm.swift.mt;

import com.abnamro.mpm.swift.mt.dsl.MtTransformationSpec;
import com.abnamro.mpm.swift.mx.MxSpecLoader;
import com.abnamro.mpm.swift.mx.MxTransformationEngine;
import com.abnamro.mpm.swift.mx.dsl.MxTransformationSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.prowidesoftware.swift.model.SwiftMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Command-line interface for SWIFT MT and MX message transformation.
 *
 * Usage (MT):
 *   java -jar swift.jar <spec-file.yaml> <source-message.txt> [output-file.txt]
 *
 * Usage (MX):
 *   java -jar swift.jar <spec-file.yaml> <source.xml> <template.xml> [output.xml]
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String specFile = args[0];

        try {
            // Peek at the spec to determine type (MT vs MX)
            String specType = readSpecType(specFile);

            if ("MX".equalsIgnoreCase(specType)) {
                runMx(args, specFile);
            } else {
                runMt(args, specFile);
            }

        } catch (IOException e) {
            log.error("I/O error", e);
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Transformation error", e);
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static String readSpecType(String specFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> raw = mapper.readValue(Paths.get(specFile).toFile(), Map.class);
        Object type = raw.get("type");
        return type != null ? type.toString() : "MT";
    }

    private static void runMx(String[] args, String specFile) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.err.println("ERROR: MX mode requires: <spec.yaml> <source.xml> <template.xml> [output.xml]");
            printUsage();
            System.exit(1);
        }

        String sourceFile = args[1];
        String templateFile = args[2];
        String outputFile = args.length == 4 ? args[3] : null;

        log.info("Loading MX transformation specification from: {}", specFile);
        MxSpecLoader loader = new MxSpecLoader();
        MxTransformationSpec spec = loader.loadFromFile(Paths.get(specFile));
        log.info("Loaded MX transformation: {}", spec.getName());

        log.info("Executing MX transformation...");
        MxTransformationEngine engine = new MxTransformationEngine();
        String result = engine.transform(spec, Paths.get(sourceFile), Paths.get(templateFile));
        log.info("MX transformation complete");

        if (outputFile != null) {
            Path outputPath = Paths.get(outputFile);
            Files.writeString(outputPath, result);
            log.info("Result written to: {}", outputFile);
            System.out.println("Transformation successful. Output written to: " + outputFile);
        } else {
            System.out.println(result);
        }

        System.err.println();
        System.err.println("=== MX Transformation Summary ===");
        System.err.println("Specification: " + spec.getName());
        System.err.println("Source format: " + spec.getSourceFormat());
        System.err.println("Target format: " + spec.getTargetFormat());
        System.err.println("Generators: " + spec.getGenerators().size());
        System.err.println("Variables: " + spec.getVariables().size());
        System.err.println("Transformations: " + spec.getTransformations().size());
    }

    private static void runMt(String[] args, String specFile) throws Exception {
        if (args.length < 2 || args.length > 3) {
            System.err.println("ERROR: MT mode requires: <spec.yaml> <source.txt> [output.txt]");
            printUsage();
            System.exit(1);
        }

        String sourceFile = args[1];
        String outputFile = args.length == 3 ? args[2] : null;

        log.info("Loading transformation specification from: {}", specFile);
        MtSpecLoader loader = new MtSpecLoader();
        MtTransformationSpec spec = loader.loadFromFile(Paths.get(specFile));
        log.info("Loaded transformation: {} ({})", spec.getName(), spec.getVersion());

        log.info("Loading source message from: {}", sourceFile);
        String sourceContent = Files.readString(Paths.get(sourceFile));
        SwiftMessage sourceMessage = SwiftMessage.parse(sourceContent);

        if (sourceMessage == null) {
            log.error("Failed to parse source message from: {}", sourceFile);
            System.err.println("ERROR: Failed to parse source message");
            System.exit(1);
        }

        log.info("Parsed source message type: MT{}", sourceMessage.getType());

        log.info("Executing transformation...");
        MtTransformationEngine engine = new MtTransformationEngine();
        SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
        log.info("Transformation complete. Target message type: MT{}", targetMessage.getType());

        String result = targetMessage.message();

        if (outputFile != null) {
            Path outputPath = Paths.get(outputFile);
            Files.writeString(outputPath, result);
            log.info("Result written to: {}", outputFile);
            System.out.println("Transformation successful. Output written to: " + outputFile);
        } else {
            System.out.println(result);
        }

        System.err.println();
        System.err.println("=== Transformation Summary ===");
        System.err.println("Specification: " + spec.getName());
        System.err.println("Source format: " + spec.getSourceFormat() + " (MT" + sourceMessage.getType() + ")");
        System.err.println("Target format: " + spec.getTargetFormat() + " (MT" + targetMessage.getType() + ")");
        System.err.println("Generators: " + spec.getGenerators().size());
        System.err.println("Variables: " + spec.getVariables().size());
        System.err.println("Transformations: " + spec.getTransformations().size());
    }

    private static void printUsage() {
        System.err.println("SWIFT Message Transformation Tool");
        System.err.println();
        System.err.println("Usage (MT):");
        System.err.println("  java -jar swift.jar <spec-file.yaml> <source-message.txt> [output-file.txt]");
        System.err.println();
        System.err.println("Usage (MX / ISO 20022):");
        System.err.println("  java -jar swift.jar <spec-file.yaml> <source.xml> <template.xml> [output.xml]");
        System.err.println();
        System.err.println("Arguments:");
        System.err.println("  spec-file.yaml    YAML transformation specification (type: MX for ISO 20022)");
        System.err.println("  source.txt/xml    Source SWIFT message");
        System.err.println("  template.xml      (MX only) Output XML template to apply transformations to");
        System.err.println("  output            Optional output file (prints to stdout if omitted)");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  # MT: print to stdout");
        System.err.println("  java -jar swift.jar 541-545.yaml I541.txt");
        System.err.println();
        System.err.println("  # MT: write to file");
        System.err.println("  java -jar swift.jar 541-545.yaml I541.txt O545.txt");
        System.err.println();
        System.err.println("  # MX: print to stdout");
        System.err.println("  java -jar swift.jar pacs008-to-pacs002.yaml source.xml template.xml");
        System.err.println();
        System.err.println("  # MX: write to file");
        System.err.println("  java -jar swift.jar pacs008-to-pacs002.yaml source.xml template.xml output.xml");
        System.err.println();
    }
}
