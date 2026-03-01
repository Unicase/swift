package com.abnamro.mpm.swift.mt;

import com.abnamro.mpm.swift.common.BaseYamlSpecLoader;
import com.abnamro.mpm.swift.mt.dsl.MtTransformationSpec;

/**
 * Loads MtTransformationSpec from YAML files.
 * Uses Jackson for YAML parsing with case-insensitive enum handling.
 */
public class MtSpecLoader extends BaseYamlSpecLoader<MtTransformationSpec> {

    public MtSpecLoader() {
        super(MtTransformationSpec.class);
    }
}

