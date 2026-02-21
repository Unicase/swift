package com.abnamro.mpm.swift.mt;

import com.abnamro.mpm.swift.mt.dsl.TransformationSpec;
import com.prowidesoftware.swift.model.SwiftMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command-line interface for SWIFT MT message transformation.
 *
 * Usage:
 *   java -jar swift.jar <spec-file.yaml> <source-message.txt> [output-file.txt]
 *
 * If output-file is not specified, the result is printed to stdout.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            printUsage();
            System.exit(1);
        }

        String specFile = args[0];
        String sourceFile = args[1];
        String outputFile = args.length == 3 ? args[2] : null;

        try {
            // Load transformation specification
            log.info("Loading transformation specification from: {}", specFile);
            TransformationSpecLoader loader = new TransformationSpecLoader();
            TransformationSpec spec = loader.loadFromFile(Paths.get(specFile));
            log.info("Loaded transformation: {} ({})", spec.name(), spec.version());

            // Load source message
            log.info("Loading source message from: {}", sourceFile);
            String sourceContent = Files.readString(Paths.get(sourceFile));
            SwiftMessage sourceMessage = SwiftMessage.parse(sourceContent);

            if (sourceMessage == null) {
                log.error("Failed to parse source message from: {}", sourceFile);
                System.err.println("ERROR: Failed to parse source message");
                System.exit(1);
            }

            log.info("Parsed source message type: MT{}", sourceMessage.getType());

            // Execute transformation
            log.info("Executing transformation...");
            MtTransformationEngine engine = new MtTransformationEngine();
            SwiftMessage targetMessage = engine.transform(spec, sourceMessage);
            log.info("Transformation complete. Target message type: MT{}", targetMessage.getType());

            // Output result
            String result = targetMessage.message();

            if (outputFile != null) {
                // Write to file
                Path outputPath = Paths.get(outputFile);
                Files.writeString(outputPath, result);
                log.info("Result written to: {}", outputFile);
                System.out.println("Transformation successful. Output written to: " + outputFile);
            } else {
                // Print to stdout
                log.info("Printing result to stdout");
                System.out.println(result);
            }

            // Print summary
            System.err.println();
            System.err.println("=== Transformation Summary ===");
            System.err.println("Specification: " + spec.name());
            System.err.println("Source format: " + spec.sourceFormat() + " (MT" + sourceMessage.getType() + ")");
            System.err.println("Target format: " + spec.targetFormat() + " (MT" + targetMessage.getType() + ")");
            System.err.println("Generators: " + spec.generators().size());
            System.err.println("Variables: " + spec.variables().size());
            System.err.println("Transformations: " + spec.transformations().size());

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

    private static void printUsage() {
        System.err.println("SWIFT MT Message Transformation Tool");
        System.err.println();
        System.err.println("Usage:");
        System.err.println("  java -jar swift.jar <spec-file.yaml> <source-message.txt> [output-file.txt]");
        System.err.println();
        System.err.println("Arguments:");
        System.err.println("  spec-file.yaml       YAML transformation specification");
        System.err.println("  source-message.txt   Source SWIFT MT message");
        System.err.println("  output-file.txt      Optional output file (prints to stdout if omitted)");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  # Print to stdout");
        System.err.println("  java -jar swift.jar 541-545.yaml I541.txt");
        System.err.println();
        System.err.println("  # Write to file");
        System.err.println("  java -jar swift.jar 541-545.yaml I541.txt O545.txt");
        System.err.println();
    }
}

