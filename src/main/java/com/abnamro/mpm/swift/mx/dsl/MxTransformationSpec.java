package com.abnamro.mpm.swift.mx.dsl;

import com.abnamro.mpm.swift.common.dsl.BaseTransformationSpec;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Root object representing a complete MX message transformation specification.
 * Maps to the YAML DSL structure for ISO 20022 XML messages.
 */
@Getter
@NoArgsConstructor
public class MxTransformationSpec extends BaseTransformationSpec {
    private List<MxVariable> variables = new ArrayList<>();
    private List<MxTransformation> transformations = new ArrayList<>();
}
