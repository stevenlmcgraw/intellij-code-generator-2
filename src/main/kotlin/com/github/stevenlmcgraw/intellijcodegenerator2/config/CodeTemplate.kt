package com.github.stevenlmcgraw.intellijcodegenerator2.config

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.java.generate.config.DuplicationPolicy
import org.jetbrains.java.generate.config.InsertWhere
import java.io.IOException
import java.util.*
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlElements
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(namespace = "codeTemplate")
class CodeTemplate(var id: UUID = UUID.randomUUID()) {

    constructor(id: String) : this(UUID.fromString(id))

    var alwaysPromptForPackage = false
    var classNameVm = CLASS_NAME_VM_TEST
    var defaultTargetPackage: String? = null
    var defaultTargetModule: String? = null
    val defaultTemplate: String = try {
        CodeTemplate::class.java.getResourceAsStream(VELOCITY_TEMPLATE_LOCATION)
            ?.let { FileUtil.loadTextAndClose(it) } ?: EMPTY_STRING
    }
    catch (exceptMe: IOException) {
        exceptMe.stackTrace
        EMPTY_STRING
    }
    var enabled: Boolean = true
    var fileEncoding: String = DEFAULT_ENCODING
    var fileNamePattern: String = FILE_NAME_PATTERN
    var insertNewMethodOption = InsertWhere.AT_CARET
    var jumpToMethod = true // jump cursor to toString method
    var name: String = UNTITLED
    @XmlElements(
        XmlElement(name = "memberSelection", type = MemberSelectionConfig::class),
        XmlElement(name = "classSelection", type = ClassSelectionConfig::class)
    )
    @XmlElementWrapper
    @XCollection(elementTypes = [MemberSelectionConfig::class, ClassSelectionConfig::class])
    var pipeline: List<PipelineStep> = ArrayList()
    var template: String = "asd"
    var type: String = "body"
    var whenDuplicatesOption = DuplicationPolicy.ASK

    fun regenerateId() {
        this.id = UUID.randomUUID()
    }

    fun getIdString() = "${this.id}"

    fun isValid() = true

    override fun toString(): String {
        return "CodeTemplate(id=$id, alwaysPromptForPackage=$alwaysPromptForPackage, classNameVm='$classNameVm', defaultTargetPackage=$defaultTargetPackage, defaultTargetModule=$defaultTargetModule, defaultTemplate='$defaultTemplate', enabled=$enabled, fileEncoding='$fileEncoding', fileNamePattern='$fileNamePattern', insertNewMethodOption=$insertNewMethodOption, jumpToMethod=$jumpToMethod, name='$name', pipeline=$pipeline, template='$template', type='$type', whenDuplicatesOption=$whenDuplicatesOption)"
    }


}
