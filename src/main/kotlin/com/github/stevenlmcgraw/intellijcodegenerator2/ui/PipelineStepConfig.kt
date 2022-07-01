package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.PipelineStep
import javax.swing.JComponent

interface PipelineStepConfig {
    fun getConfig(): PipelineStep
    fun getComponent(): JComponent
}