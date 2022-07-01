package com.github.stevenlmcgraw.intellijcodegenerator2.config

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "classSelection")
@XmlAccessorType(XmlAccessType.FIELD)
data class ClassSelectionConfig(
    var initialClass: String? = "\$class0.qualifiedName",
    var enabled: Boolean = true,
    var postfix: String = ""
): PipelineStep {

    override fun type(): String = CLASS_SELECTION

    override fun postfix(): String = this.postfix

    override fun postfix(postfix: String) {
        this.postfix = postfix
    }

    override fun enabled(): Boolean = this.enabled

    override fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
