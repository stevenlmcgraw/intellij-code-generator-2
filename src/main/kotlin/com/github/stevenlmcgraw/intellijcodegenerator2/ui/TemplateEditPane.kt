package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.uiDesigner.core.GridConstraints
import org.jetbrains.java.generate.config.DuplicationPolicy
import org.jetbrains.java.generate.config.InsertWhere
import java.awt.Dimension
import javax.swing.*

class TemplateEditPane(private val codeTemplate: CodeTemplate) {

    private val templateEdit: JPanel = JPanel()
    private val templateTypeCombo: JComboBox<*> = com.intellij.openapi.ui.ComboBox<Any>()
    private val templateIdText: JTextField = JTextField()
    private val templateNameText: JTextField = JTextField()
    private val editorPane: JPanel = JPanel()
    private val fileEncodingText: JTextField = JTextField()
    private val jumpToMethodCheckBox: JCheckBox = JCheckBox()
    private val askRadioButton: JRadioButton = JRadioButton()
    private val replaceExistingRadioButton: JRadioButton = JRadioButton()
    private val generateDuplicateMemberRadioButton: JRadioButton = JRadioButton()
    private val atCaretRadioButton: JRadioButton = JRadioButton()
    private val atEndOfClassRadioButton: JRadioButton = JRadioButton()
    private val settingsPanel: JScrollPane = com.intellij.ui.components.JBScrollPane()
    private val templateEnabledCheckBox: JCheckBox = JCheckBox()
    private val templateTabbedPane: JTabbedPane = com.intellij.ui.components.JBTabbedPane()
    private val addMemberButton: JButton = JButton()
    private val addClassButton: JButton = JButton()
    private val classNameVmText: JTextField = JTextField()
    private val alwaysPromptForPackageCheckBox: JCheckBox = JCheckBox()
    private val defaultTargetPackageText: JTextField = JTextField()
    private val defaultTargetModuleText: JTextField = JTextField()
    private val editor: Editor
    private val pipeline: MutableList<SelectionPane> = mutableListOf()

    init {
        settingsPanel.verticalScrollBar.unitIncrement = 16
        templateIdText.text = codeTemplate.getIdString()
        templateNameText.text = codeTemplate.name
        templateEnabledCheckBox.isSelected = codeTemplate.enabled
        fileEncodingText.text = StringUtil.notNullize(codeTemplate.fileEncoding, DEFAULT_ENCODING)
        templateTypeCombo.selectedItem = codeTemplate.type
        jumpToMethodCheckBox.isSelected = codeTemplate.jumpToMethod
        classNameVmText.text = codeTemplate.classNameVm
        defaultTargetPackageText.text = codeTemplate.defaultTargetPackage
        defaultTargetModuleText.text = codeTemplate.defaultTargetModule
        alwaysPromptForPackageCheckBox.isSelected = codeTemplate.alwaysPromptForPackage
        when (codeTemplate.whenDuplicatesOption) {
            DuplicationPolicy.ASK -> {
                askRadioButton.isSelected = true
                replaceExistingRadioButton.isSelected = false
                generateDuplicateMemberRadioButton.isSelected = false
            }
            DuplicationPolicy.REPLACE -> {
                askRadioButton.isSelected = false
                replaceExistingRadioButton.isSelected = true
                generateDuplicateMemberRadioButton.isSelected = false
            }
            DuplicationPolicy.DUPLICATE -> {
                askRadioButton.isSelected = false
                replaceExistingRadioButton.isSelected = false
                generateDuplicateMemberRadioButton.isSelected = true
            }
        }
        when (codeTemplate.insertNewMethodOption) {
            InsertWhere.AT_CARET -> {
                atCaretRadioButton.isSelected = true
                atEndOfClassRadioButton.isSelected = false
            }
            InsertWhere.AT_THE_END_OF_A_CLASS -> {
                atCaretRadioButton.isSelected = false
                atEndOfClassRadioButton.isSelected = true
            }
            else -> {
                atCaretRadioButton.isSelected = false
                atEndOfClassRadioButton.isSelected = false
            }
        }
        codeTemplate.pipeline.forEach(this::addMemberSelection)
        addMemberButton.addActionListener {
            val config = MemberSelectionConfig().apply {
                this.postfix = "${this@TemplateEditPane.findMaxStepPostfix(pipeline, MEMBER) + 1}"
            }
            addMemberSelection(config)
        }
        addClassButton.addActionListener {
            val config = ClassSelectionConfig().apply {
                this.postfix = "${this@TemplateEditPane.findMaxStepPostfix(pipeline, CLASS) + 1}"
            }
            addMemberSelection(config)
        }
        val pair = getVmEditorAndGridConstraintsPair(codeTemplate.template)
        editor = pair.first
        editorPane.add(editor.component, pair.second)
    }

    private fun addMemberSelection(step: PipelineStep?) {
        val title = when (step) {
            is MemberSelectionConfig -> "Member"
            is ClassSelectionConfig -> "Class"
            else -> return
        }
        val pane = SelectionPane(step, this)
        this.pipeline.add(pane)
        templateTabbedPane.addTab(title, pane.getComponent())
    }

    fun alwaysPromptForPackage(): Boolean = this.alwaysPromptForPackageCheckBox.isSelected

    fun classNameVm(): String = classNameVmText.text

    fun defaultTargetPackage(): String = defaultTargetPackageText.text

    fun defaultTargetModule(): String = defaultTargetModuleText.text

    fun duplicationPolicy(): DuplicationPolicy =
        if (askRadioButton.isSelected) DuplicationPolicy.ASK
        else if (replaceExistingRadioButton.isSelected) DuplicationPolicy.REPLACE
        else if (generateDuplicateMemberRadioButton.isSelected) DuplicationPolicy.DUPLICATE
        else DuplicationPolicy.ASK

    fun enabled(): Boolean = templateEnabledCheckBox.isEnabled

    fun fileEncoding(): String = fileEncodingText.text

    private fun findMaxStepPostfix(pipelinePanes: List<SelectionPane>, type: String): Int {
        return pipelinePanes.asSequence().filter { it.type() == type }
            .map(SelectionPane::postfix)
            .filter { it.matches(Regex("\\d+")) }
            .map { it.toInt() }
            .maxWithOrNull(Comparator.naturalOrder()) ?: 0
    }

    fun getCodeTemplate(): CodeTemplate = CodeTemplate().apply {
        this.name = this@TemplateEditPane.name()
        this.type = this@TemplateEditPane.type()
        this.enabled = this@TemplateEditPane.enabled()
        this.fileEncoding = this@TemplateEditPane.fileEncoding()
        this.template = this@TemplateEditPane.template()
        this.jumpToMethod = this@TemplateEditPane.jumpToMethod()
        this.insertNewMethodOption = this@TemplateEditPane.insertWhere()
        this.whenDuplicatesOption = this@TemplateEditPane.duplicationPolicy()
        this.pipeline = this@TemplateEditPane.pipeline.map(PipelineStepConfig::getConfig)
        this.classNameVm = this@TemplateEditPane.classNameVm()
        this.defaultTargetPackage = this@TemplateEditPane.defaultTargetPackage()
        this.defaultTargetModule = this@TemplateEditPane.defaultTargetModule()
        this.alwaysPromptForPackage = this@TemplateEditPane.alwaysPromptForPackage()
    }

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

    fun id(): String = templateIdText.text

    fun insertWhere(): InsertWhere =
        if (atCaretRadioButton.isSelected) InsertWhere.AT_CARET
        else if (atEndOfClassRadioButton.isSelected) InsertWhere.AT_THE_END_OF_A_CLASS
        else InsertWhere.AT_CARET

    fun jumpToMethod(): Boolean = jumpToMethodCheckBox.isSelected

    fun name(): String = templateNameText.text

    fun removePipelineStep(stepToRemove: PipelineStepConfig) {
        val step = this.pipeline.removeAt(this.pipeline.indexOf(stepToRemove))
        this.templateTabbedPane.remove(step.getComponent())
    }

    fun template(): String = editor.document.text

    fun templateEdit(): JPanel = templateEdit

    override fun toString(): String = this.name()

    fun type(): String = templateTypeCombo.selectedItem as String
}