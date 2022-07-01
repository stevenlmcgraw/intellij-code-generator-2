package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.CODE_TEMPLATES
import com.github.stevenlmcgraw.intellijcodegenerator2.config.INCLUDES
import com.github.stevenlmcgraw.intellijcodegenerator2.ui.include.IncludeConfig
import javax.swing.JPanel
import javax.swing.JTabbedPane

class MainPaneConfig(codeGeneratorConfig: CodeGeneratorConfig, includeConfig: IncludeConfig) {

    private val mainPanel: JPanel = JPanel()
    private val tabbedPane: JTabbedPane = com.intellij.ui.components.JBTabbedPane()

    init {
        tabbedPane.add(CODE_TEMPLATES, codeGeneratorConfig.getMainPane())
        tabbedPane.add(INCLUDES, includeConfig.getMainPane())
    }

    fun getMainPanel(): JPanel = this.mainPanel
}