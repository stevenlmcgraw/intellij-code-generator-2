package com.github.stevenlmcgraw.intellijcodegenerator2.ui.include

import com.github.stevenlmcgraw.intellijcodegenerator2.config.VM
import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.Include
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.uiDesigner.core.GridConstraints
import java.awt.Dimension
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTextField

class IncludeEditPane(private val include: Include) {

    private val templateEdit: JPanel = JPanel()
    private val templateIdText: JTextField = JTextField()
    private val templateNameText: JTextField = JTextField()
    private val editorPane: JPanel = JPanel()
    private var defaultInclude: JCheckBox = JCheckBox()
    private val editor: Editor

    init {
        this.templateIdText.text = "${include.id}"
        this.templateNameText.text = include.name
        this.defaultInclude.isSelected = include.defaultInclude
        val pair = getVmEditorAndGridConstraintsPair(include.content)
        this.editor = pair.first
        this.editorPane.add(this.editor.component, pair.second)
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

    fun name(): String = templateNameText.text

    fun content(): String = this.editor.document.text

    fun templateEdit(): JPanel = this.templateEdit

    fun getInclude(): Include {
        val include = Include(this.id())
        include.name = this.name()
        include.content = this.content()
        include.defaultInclude = this.defaultInclude.isSelected
        return include
    }
}