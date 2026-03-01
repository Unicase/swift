# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A SWIFT MT message mtTransformation engine using a declarative YAML-based DSL. It transforms SWIFT financial messages (e.g., MT541 → MT545) by applying a sequence of typed actions (SET, DELETE, INSERT_AFTER, INSERT_BEFORE, DELETE_SEQUENCE) with support for dynamic value generation and mtVariable extraction from source messages.

## Build & Run

```bash
# Build (creates target/swift.jar fat JAR)
mvn clean package

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=Mt541To545TransformationTest

# Run the CLI (print to stdout)
java -jar target/swift.jar <spec-file.yaml> <source-message.txt>

# Run the CLI (write to file)
java -jar target/swift.jar <spec-file.yaml> <source-message.txt> <output-file.txt>
```

## Architecture

**Entry point:** `Main.java` → `MtTransformationEngine.java`

**Transformation pipeline:**
1. `TransformationSpecLoader` parses YAML spec into `TransformationSpec` (record)
2. `MessageCopier` deep-copies the source `SwiftMessage` to produce the target
3. `GeneratorService` pre-generates all declared dynamic values (random IDs, timestamps)
4. `VariableExtractor` extracts values from source message fields/sequences
5. For each `Transformation` in spec order, the engine delegates to the appropriate service:
   - `BlockSetterService` — SET action
   - `BlockDeleteService` — DELETE and DELETE_SEQUENCE actions
   - `BlockInsertService` — INSERT_AFTER and INSERT_BEFORE actions
6. `FieldResolver` resolves `${varName}` references in field values during execution

**Package layout:**
- `dsl/` — Immutable Java records for configuration (`TransformationSpec`, `Transformation`, `Generator`, `Variable`, `FieldConfig`, `SequenceConfig`, `ActionType`, `GeneratorType`)
- `helpers/` — Stateless service classes that operate on `SwiftMessage` blocks

**Key constants** (`SwiftConstants.java`): block identifiers (`1`–`5`, `S`), `SEQUENCE_START_MARKER = "16R"`, `SEQUENCE_END_MARKER = "16S"`, `QUALIFIER_SEPARATOR = "::"`.

## DSL Quick Reference

A mtTransformation spec YAML has three top-level sections:

```yaml
generators:        # Pre-generate dynamic values
  - id: myRef
    type: alphanumeric   # alphanumeric | numeric | time | date
    length: 16           # for alphanumeric/numeric
    format: "HHmmss"     # for time/date

mtVariables:         # Extract values from the source message
  - id: myVar
    block: "4"
    sequence: "GENL"     # optional; omit for unsequenced fields
    field: "20C::SEME"

transformations:   # Ordered list of actions applied to the target
  - action: SET          # SET | DELETE | DELETE_SEQUENCE | INSERT_AFTER | INSERT_BEFORE
    block: "1"
    field: "20"
    value: "${myRef}"    # literal or ${varName} reference
```

## Test Resources

- `src/test/resources/mt/I541.txt` — sample source MT541 message
- `src/test/resources/mt/541-545.yaml` — example spec (MT541 → MT545 mtTransformation)

## Stack

- Java 17, Maven
- [Prowide `pw-swift-core`](https://github.com/prowide/prowide-core) for SWIFT message parsing/manipulation
- Jackson for YAML deserialization
- JUnit 5 for tests
- Logback/SLF4J for logging
