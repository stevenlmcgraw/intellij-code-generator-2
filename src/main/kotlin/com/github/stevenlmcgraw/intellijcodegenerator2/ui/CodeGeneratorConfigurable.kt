package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.CodeGeneratorSettings
import com.github.stevenlmcgraw.intellijcodegenerator2.ui.include.IncludeConfig
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class CodeGeneratorConfigurable: SearchableConfigurable {

    private val settings: CodeGeneratorSettings
    private var codeGeneratorConfig: CodeGeneratorConfig? = null
    private var includeConfig: IncludeConfig? = null
    private var mainPaneConfig: MainPaneConfig? = null

    init {
        this.settings = ServiceManager.getService(CodeGeneratorSettings::class.java)
    }

    override fun apply() {
        val templates = this.codeGeneratorConfig?.getTabTemplates() ?: mutableListOf()
        when (templates.any { !it.isValid() }) {
            true -> throw ConfigurationException("Property cannot be empty and classNumber should be an integer")
            else -> {} //do nothing
        }
        this.settings.setCodeTemplates(templates)
        this.settings.setIncludes(this.includeConfig?.getIncludes() ?: mutableListOf())
        this.codeGeneratorConfig?.refresh(templates)
        this.includeConfig?.refresh(this.includeConfig?.getIncludes() ?: mutableListOf())
    }

    override fun createComponent(): JComponent? {
        if (this.codeGeneratorConfig == null) this.codeGeneratorConfig = CodeGeneratorConfig(this.settings)
        if (this.includeConfig == null) this.includeConfig = IncludeConfig(this.settings)
        if (this.mainPaneConfig == null) this.mainPaneConfig = MainPaneConfig(this.codeGeneratorConfig!!, this.includeConfig!!)
        return this.mainPaneConfig!!.getMainPanel()
    }

    override fun getDisplayName(): String = "CodeGenerator2"

    override fun getId(): String = "plugins.codegenerator"

    private fun isCodeGeneratorModified(): Boolean = when (this.codeGeneratorConfig) {
        null -> false
        else -> {
            val templates = this.codeGeneratorConfig!!.getTabTemplates()
            if (this.settings.getCodeTemplates().size != templates.size) true
            //returns true if any `codeTemplate` meets predicate otherwise returns false
            else templates.any { codeTemplate1 ->
                val codeTemplate2 = this.settings.getCodeTemplateById(codeTemplate1.id)
                codeTemplate1 != codeTemplate2
            }

        }
    }

    private fun isIncludeModified(): Boolean = when (this.includeConfig) {
        null -> false
        else -> {
            val includes = this.includeConfig!!.getIncludes()
            if (this.settings.getIncludes().size != includes.size) true
            //returns true if any `include` meets predicate otherwise returns false
            else includes.any { include1 ->
                val include2 = this.settings.getIncludeById(include1.id)
                include1 != include2
            }
        }
    }

    override fun isModified(): Boolean = isCodeGeneratorModified() || isIncludeModified()
}