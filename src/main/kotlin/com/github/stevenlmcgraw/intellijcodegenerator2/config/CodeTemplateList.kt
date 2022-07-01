package com.github.stevenlmcgraw.intellijcodegenerator2.config

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.daemon.common.experimental.log
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper

@XmlAccessorType(XmlAccessType.FIELD)
class CodeTemplateList(
    @XmlElement(type = CodeTemplate::class)
    @XmlElementWrapper
    private var templates: MutableList<CodeTemplate> = mutableListOf()
) {

    constructor(template: CodeTemplate) : this(mutableListOf(template))

    private val log: Logger = Logger.getInstance(CodeTemplateList::class.java)

    fun getTemplates(): MutableList<CodeTemplate> {
        this.templates.forEach { it.regenerateId() }
        log.info("[CodeTemplateList::getTemplates] `this.templates`: ${this.templates}")
        return this.templates
    }

    companion object {

        private val log: Logger = Logger.getInstance(Companion::class.java)
        private val logMessage: (String, String) -> String = { method, message ->
            "[CodeTemplateList.Companion::$method] $message"
        }

        fun fromXML(xml: String): List<CodeTemplate> {
            log.info(logMessage("fromXML", "xml input: $xml"))
            val codeTemplateList = JAXB.unmarshal(StringReader(xml), CodeTemplateList::class.java)
            return codeTemplateList.getTemplates()
        }

        fun toXML(templates: MutableList<CodeTemplate>): String {
            val codeTemplateList = CodeTemplateList(templates)
            val stringWriter = StringWriter()
            JAXB.marshal(codeTemplateList, stringWriter)
            return stringWriter.toString()
        }

        fun toXML(template: CodeTemplate): String {
            val codeTemplateList = CodeTemplateList(template)
            val stringWriter = StringWriter()
            JAXB.marshal(codeTemplateList, stringWriter)
            return stringWriter.toString()
        }
    }
}