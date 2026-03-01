# SWIFT MT Message Transformation DSL

![Build Status](https://github.com/Unicase/swift/actions/workflows/build.yml/badge.svg)
[![License](https://img.shields.io/badge/license-Proprietary-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17+-blue.svg)](https://www.oracle.com/java/)

A powerful, declarative DSL for transforming SWIFT MT messages using YAML configuration.

## Download

### Latest Release
Download the pre-built JAR from the [Releases](https://github.com/Unicase/swift/releases) page.

### Latest Build (main branch)
Download artifacts from the [Actions](https://github.com/Unicase/swift/actions) tab.

## Overview

This implementation provides a complete transformation engine for SWIFT MT messages under the package `com.abnamro.mpm.swift.mt`. It allows you to define complex message transformations using YAML configuration files.

## Features

- **Declarative YAML-based DSL**: Define transformations without writing code
- **Dynamic value generation**: Generate random values, timestamps, dates
- **Variable extraction**: Extract values from source messages
- **Multiple transformation actions**:
  - `set`: Set field values in any block
  - `delete`: Remove specific fields (with sequence awareness)
  - `deleteSequence`: Remove entire sequences (16R...16S)
  - `insertAfter`: Insert sequences after a specific field
  - `insertBefore`: Insert sequences before a specific field
- **Sequence-aware operations**: All actions respect sequence context
- **Field ordering preservation**: Maintains SWIFT message structure integrity
- **Qualifier support**: Handle qualified fields (e.g., 20C::SEME)
- **All SWIFT blocks supported**: Blocks 1-5 and custom System blocks
- **Command-line interface**: Run transformations from the terminal

## Quick Start

### 1. Define a Transformation Spec (YAML)

```yaml
name: "MT541 to MT545 transformation"
description: "Convert receive instruction to settlement instruction"
version: "1.0"
sourceFormat: "MT541"
targetFormat: "MT545"

generators:
  - id: "senderMessageReference"
    type: "alphanumeric"
    length: 14

variables:
  - id: "relatedMessageReference"
    source:
      block: 4
      field: "20C::SEME"
    required: true

transformations:
  - block: "2"
    action: "set"
    fields:
      - field: "messageType"
        value: "545"

  - block: "4"
    sequence: "GENL"
    action: "set"
    fields:
      - field: "20C::SEME"
        value: "${senderMessageReference}"

  - block: "4"
    sequence: "GENL"
    action: "insertAfter"
    field: "23G"
    sequences:
      - sequence: "LINK"
        fields:
          - field: "20C::RELA"
            value: "${relatedMessageReference}"

  - block: "4"
    sequence: "TRADDET"
    action: "deleteSequence"
    sequences:
      - sequence: "FIA"
```

### 2. Use in Java Code

```java
// Load transformation specification
MtSpecLoader loader = new MtSpecLoader();
MtTransformationSpec spec = loader.loadFromResource("mt/541-545.yaml");

// Parse source message
SwiftMessage sourceMessage = SwiftMessage.parse(mt541String);

// Transform
MtTransformationEngine engine = new MtTransformationEngine();
SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

// Get transformed message
String result = targetMessage.message();
```

### 3. Use from Command Line

```bash
# Build the JAR
mvn clean package

# Run transformation to stdout
java -jar target/swift.jar spec.yaml input.txt

# Or save to file
java -jar target/swift.jar spec.yaml input.txt output.txt

# Or use the shell script
./swift-transform.sh spec.yaml input.txt output.txt
```

See [CLI_USAGE.md](CLI_USAGE.md) for complete command-line documentation.

## Architecture

### Core Components

#### Engine Classes
| Class | Purpose |
|-------|---------|
| `MtTransformationEngine` | Main transformation orchestrator |
| `BlockSetterService` | Handles SET operations for all blocks |
| `BlockDeleteService` | Handles DELETE and DELETE_SEQUENCE operations |
| `BlockInsertService` | Handles INSERT_AFTER and INSERT_BEFORE operations |
| `GeneratorService` | Generates dynamic values (numeric, alphanumeric, time, date) |
| `VariableExtractor` | Extracts values from source messages |
| `FieldResolver` | Resolves variable references (${...}) |
| `MessageCopier` | Creates deep copies of SWIFT messages |

#### Loader Classes
| Class | Purpose |
|-------|---------|
| `MtSpecLoader` | Loads YAML specifications (extends BaseYamlSpecLoader) |
| `BaseYamlSpecLoader<T>` | Generic base class for YAML loading |

#### Configuration Classes (DSL)
| Class | Purpose |
|-------|---------|
| `MtTransformationSpec` | Root configuration object (extends BaseTransformationSpec) |
| `MtTransformation` | Single transformation action |
| `MtVariable` | Variable extraction configuration |
| `Generator` | Dynamic value generation config |
| `FieldConfig` | Field configuration |
| `SequenceConfig` | Sequence configuration |
| `ActionType` | Enum: SET, DELETE, DELETE_SEQUENCE, INSERT_AFTER, INSERT_BEFORE |
| `GeneratorType` | Enum: NUMERIC, ALPHANUMERIC, TIME, DATE |

#### Constants
| Class | Purpose |
|-------|---------|
| `SwiftConstants` | SWIFT-specific constants (16R, 16S, block IDs, separators) |

## DSL Reference

### Generators

Generate dynamic values:

```yaml
generators:
  - id: "referenceId"
    type: "alphanumeric"  # or: numeric, time, date
    length: 16
  - id: "currentTime"
    type: "time"
    format: "HHmmss"
  - id: "currentDate"
    type: "date"
    format: "yyyyMMdd"
```

### Variables

Extract values from source message:

```yaml
variables:
  - id: "originalReference"
    source:
      block: 4
      sequence: "GENL"  # optional - limits search to this sequence
      field: "20C::SEME"
    required: true
```

### Transformation Actions

All actions in block 4 support the optional `sequence` parameter to limit operations to a specific sequence.

#### SET - Update field values

```yaml
# Set fields globally in block 4
- block: "4"
  action: "set"
  fields:
    - field: "20C::SEME"
      value: "${newReference}"
    - field: "23G"
      value: "NEWM"

# Set fields only within GENL sequence
- block: "4"
  sequence: "GENL"
  action: "set"
  fields:
    - field: "20C::SEME"
      value: "${newReference}"
```

#### DELETE - Remove fields

```yaml
# Delete fields globally
- block: "4"
  action: "delete"
  fields:
    - field: "98C::PREP"
    - field: "90A::DEAL"

# Delete fields only within TRADDET sequence
- block: "4"
  sequence: "TRADDET"
  action: "delete"
  fields:
    - field: "90A::DEAL"
```

#### DELETE SEQUENCE - Remove entire sequences

```yaml
# Delete top-level sequence
- block: "4"
  action: "deleteSequence"
  sequences:
    - sequence: "LINK"

# Delete nested sequence (FIA within TRADDET)
- block: "4"
  sequence: "TRADDET"
  action: "deleteSequence"
  sequences:
    - sequence: "FIA"
```

#### INSERT AFTER - Insert sequences/fields after a field

```yaml
# Insert sequence after a field
- block: "4"
  sequence: "GENL"  # Look for field within GENL
  action: "insertAfter"
  field: "23G"
  sequences:
    - sequence: "LINK"
      fields:
        - field: "20C::RELA"
          value: "${originalReference}"

# Insert simple fields (no sequence markers)
- block: "4"
  action: "insertAfter"
  field: "23G"
  fields:
    - field: "22F::TRTR"
      value: "S"
    - field: "98A::PREP"
      value: "20260215"
```

#### INSERT BEFORE - Insert sequences/fields before a field

```yaml
- block: "4"
  sequence: "TRADDET"
  action: "insertBefore"
  field: "98A::SETT"
  sequences:
    - sequence: "NEWSEQ"
      fields:
        - field: "22F::INDICATOR"
          value: "Y"
```

## Testing

The implementation includes comprehensive JUnit 5 tests:

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=MtTransformationTest
```

### Test Coverage

- ✅ Transformation spec loading from YAML
- ✅ Message parsing and structure validation
- ✅ Full transformation execution
- ✅ Block 1-5 transformations
- ✅ System block handling
- ✅ Field replacement with sequence context
- ✅ Field deletion with sequence filtering
- ✅ Sequence deletion (nested and top-level)
- ✅ Sequence insertion (after/before)
- ✅ Variable extraction and resolution
- ✅ Generator functionality
- ✅ Message structure integrity preservation

All 15 tests pass successfully.

## Example: MT541 → MT545 Transformation

The included example transforms an MT541 (Receive Instruction) to MT545 (Settlement Instruction):

**Changes Applied:**
- ✅ Message type: 541 → 545
- ✅ New sender message reference generated
- ✅ LINK sequence inserted with RELA pointing to original reference
- ✅ PREP field deleted from GENL
- ✅ DEAL field deleted from TRADDET
- ✅ FIA sequence removed from TRADDET
- ✅ Block 5 CHK field updated
- ✅ System block fields added

## Variable References

Use `${variableName}` syntax to reference:
- Generated values: `${senderMessageReference}`
- Extracted variables: `${relatedMessageReference}`

The engine automatically resolves all references during transformation using the `FieldResolver` service.

## Block Support

| Block | Description | Supported Actions |
|-------|-------------|-------------------|
| 1 | Basic Header | SET |
| 2 | Application Header | SET |
| 3 | User Header | SET |
| 4 | Text Block | SET, DELETE, DELETE_SEQUENCE, INSERT_AFTER, INSERT_BEFORE |
| 5 | Trailer | SET |
| S | System Block | SET |

**Note:** All block 4 actions support sequence-aware operations via the optional `sequence` parameter.

## Field Specifications

### Simple Fields
```yaml
field: "23G"
value: "NEWM"
```

### Qualified Fields
```yaml
field: "20C::SEME"
value: "${reference}"
```

Format: `TAG::QUALIFIER`

The qualifier and value are formatted as `:QUALIFIER//value` in the SWIFT message.

## Sequence Context

The `sequence` parameter in transformations ensures operations only affect fields within that sequence:

```yaml
# This only updates 20C::SEME within the GENL sequence
- block: "4"
  sequence: "GENL"
  action: "set"
  fields:
    - field: "20C::SEME"
      value: "${newRef}"

# This deletes FIA sequence that's nested within TRADDET
- block: "4"
  sequence: "TRADDET"
  action: "deleteSequence"
  sequences:
    - sequence: "FIA"
```

## Project Structure

```
src/
├── main/java/com/abnamro/mpm/swift/
│   ├── common/
│   │   ├── BaseYamlSpecLoader.java     # Generic YAML loader
│   │   └── dsl/
│   │       ├── BaseTransformationSpec.java  # Base spec class
│   │       ├── FieldResolver.java
│   │       ├── Generator.java
│   │       ├── GeneratorType.java
│   │       └── TransformationType.java
│   ├── mt/
│   │   ├── Main.java                   # CLI entry point
│   │   ├── MtTransformationEngine.java # Main engine
│   │   ├── MtSpecLoader.java
│   │   └── SwiftConstants.java
│   │   ├── dsl/
│   │   │   ├── MtTransformationSpec.java
│   │   │   ├── MtTransformation.java
│   │   │   ├── MtVariable.java
│   │   │   ├── VariableSource.java
│   │   │   ├── ActionType.java
│   │   │   ├── FieldConfig.java
│   │   │   └── SequenceConfig.java
│   │   └── helpers/
│   │       ├── BlockSetterService.java
│   │       ├── BlockDeleteService.java
│   │       ├── BlockInsertService.java
│   │       ├── GeneratorService.java
│   │       ├── VariableExtractor.java
│   │       └── MessageCopier.java
│   └── mx/
│       ├── MxTransformationEngine.java
│       ├── MxSpecLoader.java
│       └── dsl/
│           ├── MxTransformationSpec.java
│           ├── MxTransformation.java
│           └── MxVariable.java
└── test/
    └── resources/
        └── mt/
            ├── I541.txt                # Sample MT541 message
            └── 541-545.yaml            # Transformation spec
```

## Resources

Test resources included:
- `src/test/resources/mt/I541.txt` - Sample MT541 message
- `src/test/resources/mt/541-545.yaml` - Complete transformation specification

## Command-Line Usage

See [CLI_USAGE.md](CLI_USAGE.md) for complete documentation.

Quick example:
```bash
java -jar target/swift.jar \
  src/test/resources/mt/541-545.yaml \
  src/test/resources/mt/I541.txt \
  output.txt
```

## Building

```bash
# Build with tests
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run tests only
mvn test
```

## Documentation

- [CLI_USAGE.md](CLI_USAGE.md) - Command-line interface documentation
- [GITHUB_ACTIONS.md](GITHUB_ACTIONS.md) - CI/CD workflow documentation
- [GITHUB_SETUP.md](GITHUB_SETUP.md) - Repository setup guide

## License

Part of the ABN AMRO MPM SWIFT transformation project.

