package com.github.stevenlmcgraw.intellijcodegenerator2.config

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "memberSelection")
@XmlAccessorType(XmlAccessType.FIELD)
data class MemberSelectionConfig(
    var filterConstantField: Boolean = true,
    var filterEnumField: Boolean = false,
    var filterTransientModifier: Boolean = false,
    var filterStaticModifier: Boolean = true,
    var filterLoggers: Boolean = true,
    var filterFieldName: String? = "",
    var filterFieldType: String? = "",
    var filterMethodName: String? = "",
    var filterMethodType: String? = "",
    var enableMethods: Boolean = false,
    var providerTemplate: String? = MEMBER_SELECTION_CONFIG_DEFAULT_TEMPLATE,
    var allowMultiSelection: Boolean = true,
    var allowEmptySelection: Boolean = true,
    var sortElements: Int = 0,
    var postfix: String = "",
    var enabled: Boolean = true,
): PipelineStep {

    override fun type(): String = MEMBER_SELECTION

    override fun postfix(): String = this.postfix

    override fun postfix(postfix: String) { this.postfix = postfix }

    override fun enabled(): Boolean = this.enabled

    override fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
