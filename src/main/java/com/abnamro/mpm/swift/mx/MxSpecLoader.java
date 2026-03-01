package com.abnamro.mpm.swift.mx;

import com.abnamro.mpm.swift.common.BaseYamlSpecLoader;
import com.abnamro.mpm.swift.mx.dsl.MxTransformationSpec;

/**
 * Loads MxTransformationSpec from YAML files.
 * Uses Jackson for YAML parsing with case-insensitive enum handling.
 */
public class MxSpecLoader extends BaseYamlSpecLoader<MxTransformationSpec> {

    public MxSpecLoader() {
        super(MxTransformationSpec.class);
    }
}
