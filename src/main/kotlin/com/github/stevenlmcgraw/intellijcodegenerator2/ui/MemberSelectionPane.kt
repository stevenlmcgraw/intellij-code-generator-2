package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.MemberSelectionConfig
import com.github.stevenlmcgraw.intellijcodegenerator2.config.PipelineStep
import com.github.stevenlmcgraw.intellijcodegenerator2.config.VM
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.uiDesigner.core.GridConstraints
import java.awt.Dimension
import javax.swing.*

class MemberSelectionPane(private val config: MemberSelectionConfig): PipelineStepConfig {

    private val editorPane: JPanel = JPanel()
    private val excludeConstantFieldsCheckBox: JCheckBox = JCheckBox()
    private val excludeStaticFieldsCheckBox: JCheckBox = JCheckBox()
    private val excludeTransientFieldsCheckBox: JCheckBox = JCheckBox()
    private val excludeEnumFieldsCheckBox: JCheckBox = JCheckBox()
    private val excludeLoggerFieldsLog4jCheckBox: JCheckBox = JCheckBox()
    private val excludeFieldsNameText: JTextField = JTextField()
    private val excludeFieldsByTypeText: JTextField = JTextField()
    private val excludeMethodsByNameText: JTextField = JTextField()
    private val excludeMethodsByTypeText: JTextField = JTextField()
    private val enableMethodSelectionCheckBox: JCheckBox = JCheckBox()
    private val comboBoxSortElements: JComboBox<*> = com.intellij.openapi.ui.ComboBox<Any>()
    private val sortElementsCheckBox: JCheckBox = JCheckBox()
    private val topPane: JPanel = JPanel()
    private val allowMultipleSelectionCheckBox: JCheckBox = JCheckBox()
    private val allowEmptySelectionCheckBox: JCheckBox = JCheckBox()
    private val editor: Editor

    init {
        this.excludeConstantFieldsCheckBox.isSelected = config.filterConstantField
        this.excludeStaticFieldsCheckBox.isSelected = config.filterStaticModifier
        this.excludeTransientFieldsCheckBox.isSelected = config.filterTransientModifier
        this.excludeEnumFieldsCheckBox.isSelected = config.filterEnumField
        this.excludeLoggerFieldsLog4jCheckBox.isSelected = config.filterLoggers
        this.excludeFieldsNameText.text = config.filterFieldName
        this.excludeFieldsByTypeText.text = config.filterFieldType
        this.excludeMethodsByNameText.text = config.filterMethodName
        this.excludeMethodsByTypeText.text = config.filterMethodType
        this.enableMethodSelectionCheckBox.isSelected = config.enableMethods
        this.sortElementsCheckBox.addItemListener {
            this.comboBoxSortElements.isEnabled = this.sortElementsCheckBox.isSelected
        }
        this.comboBoxSortElements.selectedIndex = config.sortElements - 1
        this.sortElementsCheckBox.isSelected = config.sortElements != 0
        this.allowEmptySelectionCheckBox.isSelected = config.allowEmptySelection
        this.allowMultipleSelectionCheckBox.isSelected = config.allowMultiSelection
        val pair = getVmEditorAndGridConstraintsPair(config.providerTemplate!!)
        this.editor = pair.first
        this.editorPane.add(editor.component, pair.second)
    }

    override fun getConfig(): PipelineStep = MemberSelectionConfig().apply {
        this.filterConstantField = this@MemberSelectionPane.excludeConstantFieldsCheckBox.isSelected
        this.filterEnumField = this@MemberSelectionPane.excludeEnumFieldsCheckBox.isSelected
        this.filterTransientModifier = this@MemberSelectionPane.excludeTransientFieldsCheckBox.isSelected
        this.filterStaticModifier = this@MemberSelectionPane.excludeStaticFieldsCheckBox.isSelected
        this.filterLoggers = this@MemberSelectionPane.excludeLoggerFieldsLog4jCheckBox.isSelected
        this.filterFieldName = this@MemberSelectionPane.excludeFieldsNameText.text
        this.filterFieldType = this@MemberSelectionPane.excludeFieldsByTypeText.text
        this.filterMethodName = this@MemberSelectionPane.excludeMethodsByNameText.text
        this.filterMethodType = this@MemberSelectionPane.excludeMethodsByTypeText.text
        this.enableMethods = this@MemberSelectionPane.enableMethodSelectionCheckBox.isSelected
        this.providerTemplate = this@MemberSelectionPane.editor.document.text
        this.allowEmptySelection = this@MemberSelectionPane.allowEmptySelectionCheckBox.isSelected
        this.allowMultiSelection = this@MemberSelectionPane.allowMultipleSelectionCheckBox.isSelected
        this.sortElements = this@MemberSelectionPane.sortElements()
    }

    override fun getComponent(): JComponent = this.topPane

    private fun getVmEditorAndGridConstraintsPair(template: String): Pair<Editor, GridConstraints> {
        val factory = EditorFactory.getInstance()
        val velocityTemplate = factory.createDocument(template)
        val editor = factory.createEditor(
            velocityTemplate,
            null,
            FileTypeManager.getInstance().getFileTypeByExtension(VM),
            false
        )
        val constraints = GridConstraints(
            0,
            0,
            1,
            1,
            GridConstraints.ANCHOR_WEST,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_FIXED,
            null,
            Dimension(0, 0),
            null,
            0,
            true
        )
        return Pair(editor, constraints)
    }

    private fun sortElements(): Int = when (!this.sortElementsCheckBox.isSelected) {
        true -> 0
        else -> this.comboBoxSortElements.selectedIndex + 1
    }
}