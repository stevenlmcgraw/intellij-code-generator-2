package com.github.stevenlmcgraw.intellijcodegenerator2.config

import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.Include
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.UUID

@State(name = "CodeGeneratorSettings", storages = [Storage("\$APP_CONFIG$/intellij-code-generator-2-settings.xml")])
class CodeGeneratorSettings: PersistentStateComponent<CodeGeneratorSettings> {

    private var codeTemplates: MutableList<CodeTemplate> = mutableListOf()
    private var includes: MutableList<Include> = mutableListOf()

    fun getCodeTemplates() = this.codeTemplates

    fun setCodeTemplates(codeTemplates: MutableList<CodeTemplate>) {
        this.codeTemplates = codeTemplates
    }

    fun getIncludes(): MutableList<Include> = this.includes

    fun setIncludes(includes: MutableList<Include>) {
        this.includes = includes
    }

    fun getCodeTemplateById(id: String) = codeTemplates.find { it.getIdString() == id }

    fun getCodeTemplateById(id: UUID) = codeTemplates.find { it.id == id }

    fun getIncludeById(id: String) = includes.find { it.getIdString() == id }

    fun getIncludeById(id: UUID) = includes.find { it.id == id }

    fun removeCodeTemplate(id: String) = codeTemplates.removeIf { it.name == id }

    @OptIn(ExperimentalStdlibApi::class)
    fun loadDefaultTemplates(): MutableList<CodeTemplate> = try {
        buildList<CodeTemplate> {
            loadTemplates("getters-and-setters.xml")
            loadTemplates("to-string.xml")
            loadTemplates("HUE-serialization.xml")
        }.toMutableList()
    }
    catch (exceptMe: Exception) {
        exceptMe.printStackTrace()
        mutableListOf()
    }

    private fun loadTemplates(templateFileName: String) =
//        CodeTemplateList.fromXML(
//            this::class.java.getResourceAsStream("template/$templateFileName")?.let {
//                FileUtil.loadTextAndClose(it)
//            } ?: "slheemy"
//        )
//        CodeTemplateList.fromXML(
//            this::class.java.getResource("template/$templateFileName")?.let {
//                FileUtil.loadTextAndClose(it.openStream())
//            } ?: "slheemy"
//        )
        CodeTemplateList.fromXML(GETTER_AND_SETTER_XML)

    override fun getState(): CodeGeneratorSettings? = when (this.codeTemplates.isEmpty()) {
        true -> {
            this.codeTemplates = loadDefaultTemplates()
            this
        }
        else -> this
    }

    override fun loadState(state: CodeGeneratorSettings) = XmlSerializerUtil.copyBean(state, this)
}