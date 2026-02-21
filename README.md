# SWIFT MT Message Transformation DSL

A powerful, declarative DSL for transforming SWIFT MT messages using YAML configuration.

## Overview

This implementation provides a complete transformation engine for SWIFT MT messages under the package `com.abnamro.mpm.swift.mt`. It allows you to define complex message transformations using YAML configuration files.

## Features

- **Declarative YAML-based DSL**: Define transformations without writing code
- **Dynamic value generation**: Generate random values, timestamps, dates
- **Variable extraction**: Extract values from source messages
- **Multiple transformation actions**:
  - `set`: Set field values in any block
  - `delete`: Remove specific fields
  - `deleteSequence`: Remove entire sequences (16R...16S)
  - `insertAfter`: Insert sequences after a specific field
  - `insertBefore`: Insert sequences before a specific field
- **Field ordering preservation**: Maintains SWIFT message structure integrity
- **Qualifier support**: Handle qualified fields (e.g., 20C::SEME)
- **All SWIFT blocks supported**: Blocks 1-5 and custom System blocks

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
    action: "set"
    fields:
      - field: "20C::SEME"
        value: "${senderMessageReference}"

  - block: "4"
    action: "insertAfter"
    field: "23G"
    sequences:
      - sequence: "LINK"
        fields:
          - field: "20C::RELA"
            value: "${relatedMessageReference}"
```

### 2. Use in Java Code

```java
// Load transformation specification
TransformationSpecLoader loader = new TransformationSpecLoader();
TransformationSpec spec = loader.loadFromResource("mt/541-545.yaml");

// Parse source message
SwiftMessage sourceMessage = SwiftMessage.parse(mt541String);

// Transform
MtTransformationEngine engine = new MtTransformationEngine();
SwiftMessage targetMessage = engine.transform(spec, sourceMessage);

// Get transformed message
String result = targetMessage.message();
```

## Architecture

### Core Classes

| Class | Purpose |
|-------|---------|
| `TransformationSpec` | Root configuration object |
| `MtTransformationEngine` | Main transformation engine |
| `GeneratorService` | Generates dynamic values |
| `VariableExtractor` | Extracts values from source messages |
| `TransformationSpecLoader` | Loads YAML specifications |

### Configuration Classes

| Class | Purpose |
|-------|---------|
| `Generator` | Dynamic value generation config |
| `Variable` | Variable extraction config |
| `Transformation` | Single transformation action |
| `FieldConfig` | Field configuration |
| `SequenceConfig` | Sequence configuration |

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
      sequence: "GENL"  # optional
      field: "20C::SEME"
    required: true
```

### Transformation Actions

#### SET - Update field values

```yaml
- block: "4"
  action: "set"
  fields:
    - field: "20C::SEME"
      value: "${newReference}"
    - field: "23G"
      value: "NEWM"
```

#### DELETE - Remove fields

```yaml
- block: "4"
  action: "delete"
  fields:
    - field: "98C::PREP"
    - field: "90A::DEAL"
```

#### DELETE SEQUENCE - Remove entire sequences

```yaml
- block: "4"
  action: "deleteSequence"
  sequences:
    - sequence: "FIA"
```

#### INSERT AFTER - Insert sequences after a field

```yaml
- block: "4"
  action: "insertAfter"
  field: "23G"
  sequences:
    - sequence: "LINK"
      fields:
        - field: "20C::RELA"
          value: "${originalReference}"
```

#### INSERT BEFORE - Insert sequences before a field

```yaml
- block: "4"
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
mvn test -Dtest=Mt541To545TransformationTest
```

### Test Coverage

- ✅ Transformation spec loading
- ✅ Message parsing
- ✅ Full transformation execution
- ✅ Block 1-5 transformations
- ✅ System block handling
- ✅ Field replacement
- ✅ Field deletion
- ✅ Sequence deletion
- ✅ Sequence insertion
- ✅ Message structure integrity

All 15 tests pass successfully.

## Example: MT541 → MT545 Transformation

The included example transforms an MT541 (Receive Instruction) to MT545 (Settlement Instruction):

**Changes Applied:**
- ✅ Message type: 541 → 545
- ✅ New sender message reference generated
- ✅ LINK sequence inserted with RELA pointing to original reference
- ✅ PREP field deleted
- ✅ DEAL field deleted
- ✅ FIA sequence removed
- ✅ Block 5 CHK field updated
- ✅ System block fields added

## Variable References

Use `${variableName}` syntax to reference:
- Generated values: `${senderMessageReference}`
- Extracted variables: `${relatedMessageReference}`

The engine automatically resolves all references during transformation.

## Block Support

| Block | Description | Supported Actions |
|-------|-------------|-------------------|
| 1 | Basic Header | SET |
| 2 | Application Header | SET |
| 3 | User Header | SET |
| 4 | Text Block | SET, DELETE, DELETE SEQUENCE, INSERT AFTER, INSERT BEFORE |
| 5 | Trailer | SET |
| S | System Block | SET |

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

## Resources

Test resources included:
- `src/test/resources/mt/I541.txt` - Sample MT541 message
- `src/test/resources/mt/541-545.yaml` - Transformation specification

## License

Part of the ABN AMRO MPM SWIFT transformation project.

