package com.github.stevenlmcgraw.intellijcodegenerator2.config.include

import com.github.stevenlmcgraw.intellijcodegenerator2.config.CodeTemplate
import com.github.stevenlmcgraw.intellijcodegenerator2.config.EMPTY_STRING
import com.github.stevenlmcgraw.intellijcodegenerator2.config.UNTITLED
import com.github.stevenlmcgraw.intellijcodegenerator2.config.VELOCITY_TEMPLATE_LOCATION
import com.intellij.openapi.util.io.FileUtil
import java.io.IOException
import java.util.*
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "include")
@XmlAccessorType(XmlAccessType.FIELD)
class Include(var id: UUID = UUID.randomUUID()) {

    constructor(id: String) : this(UUID.fromString(id))

    @XmlAttribute
    val VERSION: String = "1.3"

    private val defaultTemplate: String = try {
        CodeTemplate::class.java.getResourceAsStream(VELOCITY_TEMPLATE_LOCATION)
            ?.let { FileUtil.loadTextAndClose(it) } ?: EMPTY_STRING
    }
    catch (exceptMe: IOException) {
        exceptMe.stackTrace
        EMPTY_STRING
    }

    var name: String = UNTITLED
    var content: String = defaultTemplate
    var defaultInclude: Boolean = false

    fun regenerateId() {
        this.id = UUID.randomUUID()
    }

    fun getIdString(): String = "${this.id}"
}