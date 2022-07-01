package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.ClassSelectionConfig
import com.github.stevenlmcgraw.intellijcodegenerator2.config.PipelineStep
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class ClassSelectionPane(private val config: ClassSelectionConfig): PipelineStepConfig {

    private val topPane: JPanel = JPanel()
    private val initialClassText: JTextField = JTextField()

    init {
        this.initialClassText.text = config.initialClass
    }

    override fun getConfig(): PipelineStep = ClassSelectionConfig().apply {
        this.initialClass = this@ClassSelectionPane.initialClassText.text
    }

    override fun getComponent(): JComponent = topPane
}