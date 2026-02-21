# Example: Insert Simple Fields

## Scenario 1: Insert a sequence with fields (current behavior)
```yaml
- block: "4"
  action: "insertAfter"
  field: "23G"
  sequences:
    - sequence: "LINK"
      fields:
        - field: "20C::RELA"
          value: "${relatedMessageReference}"
```
**Result**: Inserts after field 23G:
```
:23G:NEWM
:16R:LINK
:20C::RELA//REF123
:16S:LINK
```

---

## Scenario 2: Insert simple fields without a sequence (NEW)
```yaml
- block: "4"
  action: "insertAfter"
  field: "23G"
  fields:
    - field: "20C::SEME"
      value: "${newReference}"
    - field: "98A::TRAD"
      value: "20260215"
```
**Result**: Inserts fields directly after field 23G (no 16R/16S markers):
```
:23G:NEWM
:20C::SEME//ABC123456
:98A::TRAD//20260215
```

---

## Scenario 3: Combine both sequences AND simple fields (NEW)
```yaml
- block: "4"
  action: "insertBefore"
  field: "98A::SETT"
  sequences:
    - sequence: "LINK"
      fields:
        - field: "20C::RELA"
          value: "${relatedRef}"
  fields:
    - field: "22F::TRTR"
      value: "S"
```
**Result**: Inserts both the LINK sequence and a simple field:
```
:16R:LINK
:20C::RELA//OLDREF
:16S:LINK
:22F::TRTR//S
:98A::SETT//20260220
```

---

## Use Cases for Simple Field Insertion

1. **Add a single field** without the overhead of a sequence structure
2. **Insert multiple related fields** in a specific order
3. **Mix sequences and simple fields** in the same insert operation
4. **Add optional fields** based on business logic

The `buildTagsToInsert()` method now handles both:
- **Sequences**: Wrapped with 16R/16S markers (SWIFT sequence boundaries)
- **Simple Fields**: Inserted directly without markers

