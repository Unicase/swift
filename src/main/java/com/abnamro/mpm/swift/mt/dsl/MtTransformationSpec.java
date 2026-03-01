package com.abnamro.mpm.swift.mt.dsl;

import com.abnamro.mpm.swift.common.dsl.BaseTransformationSpec;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Root object representing a complete MT message transformation specification.
 * Maps to the YAML DSL structure.
 */
@Getter
@NoArgsConstructor
public class MtTransformationSpec extends BaseTransformationSpec {
    private List<MtVariable> variables = new ArrayList<>();
    private List<MtTransformation> transformations = new ArrayList<>();
}
