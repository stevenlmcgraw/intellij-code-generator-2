package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.*
import com.intellij.openapi.ui.Messages
import com.intellij.ui.layout.selected
import javax.swing.*

class SelectionPane(config: PipelineStep, parent: TemplateEditPane): PipelineStepConfig {

    private val postfixText: JTextField = JTextField()
    private val enableStepCheckBox: JCheckBox = JCheckBox()
    private val removeThisStepButton: JButton = JButton()
    private val topPanel: JPanel = JPanel()
    private val contentPane: JScrollPane = com.intellij.ui.components.JBScrollPane()
    private var selectionPane: Any

    init {
        postfixText.text = config.postfix()
        enableStepCheckBox.isSelected = config.enabled()
        removeThisStepButton.addActionListener {
            when (Messages.showYesNoDialog("Are you sure you want to move this step?", "Delete", null)) {
                Messages.OK -> parent.removePipelineStep(this)
                else -> return@addActionListener
            }
        }
        val pane = when (config) {
            is MemberSelectionConfig -> MemberSelectionPane(config)
            is ClassSelectionConfig -> ClassSelectionPane(config)
            else -> MemberSelectionPane(config as MemberSelectionConfig)
        }
        contentPane.setViewportView(pane.getComponent())
        selectionPane = pane
    }

    fun enabled(): Boolean = enableStepCheckBox.isSelected

    override fun getConfig(): PipelineStep = when (selectionPane) {
        is MemberSelectionPane -> {
            val step = (selectionPane as MemberSelectionPane).getConfig()
            step.postfix(this.postfix())
            step.enabled(this.enabled())
            step
        }
        is ClassSelectionPane -> {
            val step = (selectionPane as ClassSelectionPane).getConfig()
            step.postfix(this.postfix())
            step.enabled(this.enabled())
            step
        }
        else -> {
            val step = (selectionPane as MemberSelectionPane).getConfig()
            step.postfix(this.postfix())
            step.enabled(this.enabled())
            step
        }
    }

    override fun getComponent(): JComponent = this.topPanel

    fun postfix(): String = postfixText.text

    fun type(): String = when (selectionPane) {
        is MemberSelectionPane -> MEMBER
        is ClassSelectionPane -> CLASS
        else -> EMPTY_STRING
    }
}