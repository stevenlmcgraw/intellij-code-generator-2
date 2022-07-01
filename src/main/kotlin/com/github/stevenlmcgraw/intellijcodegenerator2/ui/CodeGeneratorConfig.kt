package com.github.stevenlmcgraw.intellijcodegenerator2.ui

import com.github.stevenlmcgraw.intellijcodegenerator2.config.*
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture
import javax.swing.*

@OptIn(ExperimentalStdlibApi::class)
class CodeGeneratorConfig(private val settings: CodeGeneratorSettings) {

    private val mainPane: JPanel = JPanel()
    private val addTemplateButton: JButton = JButton()
    private val splitPane: JSplitPane = JSplitPane()
    private val templateList: JList<TemplateEditPane> = com.intellij.ui.components.JBList()
    private val templateListModel: DefaultListModel<TemplateEditPane> = DefaultListModel()
    private val deleteTemplateButton: JButton = JButton()
    private val splitRightPane: JPanel = JPanel()
    private val scrollPane: JScrollPane = com.intellij.ui.components.JBScrollPane()
    private val importButton: JButton = JButton()
    private val exportButton: JButton = JButton()
    private val exportAllButton: JButton = JButton()
    private val duplicateTemplateButton: JButton = JButton()

    init {
        this.templateList.model = this.templateListModel
        this.templateList.addListSelectionListener {
            when (it.valueIsAdjusting) {
                true -> return@addListSelectionListener
                else -> {
                    val length = templateListModel.size
                    val idx = templateList.selectedIndex
                    if (length < 0 || idx < 0 || idx >= length) {
                        this.splitPane.rightComponent = this.splitRightPane
                        this.deleteTemplateButton.isEnabled = false
                        this.duplicateTemplateButton.isEnabled = false
                        return@addListSelectionListener
                    }
                    else {
                        val pane = templateListModel.get(templateList.selectedIndex)
                        this.deleteTemplateButton.isEnabled = true
                        this.duplicateTemplateButton.isEnabled = true
                        this.splitPane.rightComponent = pane.templateEdit()
                    }
                }
            }
        }
        this.addTemplateButton.addActionListener {
            val codeTemplate = CodeTemplate().apply { this.name = UNTITLED }
            val editPane = TemplateEditPane(codeTemplate)
            val model = (templateList.model as DefaultListModel<TemplateEditPane>).apply { this.addElement(editPane) }
            this.templateList.selectedIndex = model.size - 1
        }
        this.deleteTemplateButton.addActionListener {
            val idx = this.templateList.selectedIndex
            val size = this.templateListModel.size
            when (idx >= 0 && idx < size) {
                false -> return@addActionListener
                true -> {
                    val result = Messages.showYesNoDialog("Delete this template?", "Delete", null)
                    when (result == Messages.OK) {
                        false -> return@addActionListener
                        true -> {
                            val lastIdx = this.templateList.anchorSelectionIndex
                            this.templateListModel.remove(lastIdx)
                            val nextIdx =
                                if (lastIdx in 0 until idx || lastIdx == idx && idx < size -1) lastIdx
                                else if (lastIdx == idx || lastIdx > idx && lastIdx < size -1) lastIdx -1
                                else if (lastIdx >= idx) size - 2
                                else -1
                            this.templateList.selectedIndex = nextIdx
                        }
                    }
                }
            }
        }
        this.duplicateTemplateButton.addActionListener {
            when (val idx = this.templateList.selectedIndex) {
                in Int.MIN_VALUE..-1 -> return@addActionListener
                else -> {
                    val template = templateListModel.get(idx)
                    val xml = CodeTemplateList.toXML(template.getCodeTemplate())
                    val currentTemplates = getTabTemplates()
                    val templates = CodeTemplateList.fromXML(xml)
                    if (templates.isEmpty()) return@addActionListener
                    else {
                        templates[0].name = "Copy of ${templates[0].name}"
                        currentTemplates.addAll(templates)
                        refresh(currentTemplates)
                        this.templateList.selectedIndex = templateListModel.size + 1
                    }
                }
            }
        }
        this.exportButton.addActionListener {
            val idx = this.templateList.selectedIndex
            val template = this.templateListModel.get(idx)
            val xml = CodeTemplateList.toXML(template.getCodeTemplate())
            saveToFile(xml)
        }
        this.exportAllButton.addActionListener {
            val templates =
                if (this.templateListModel.size == 0) mutableListOf()
                else buildList {
                    for (i in 0 until this@CodeGeneratorConfig.templateListModel.size)
                        this.add(this@CodeGeneratorConfig.templateListModel.get(i).getCodeTemplate())
                }.toMutableList()
            val xml = CodeTemplateList.toXML(templates)
            saveToFile(xml)
        }
        this.importButton.addActionListener {
            readFromFile().thenAccept { xml ->
                try {
                    val templates = CodeTemplateList.fromXML(xml)
                    val currentTemplates = getTabTemplates().apply { addAll(templates) }
                    refresh(currentTemplates)
                    Messages.showMessageDialog(IMPORT_FINISHED, IMPORT, null)
                }
                catch (exceptMe: Exception) {
                    exceptMe.printStackTrace()
                    Messages.showMessageDialog(IMPORT_FAILED, IMPORT_ERROR, null)
                }
            }
        }
        resetTabPane(this.settings.getCodeTemplates())
    }

    fun getMainPane(): JPanel = this.mainPane

    fun getTabTemplates(): MutableList<CodeTemplate> =
        if(this.templateListModel.size == 0) mutableListOf()
        else buildList {
            for (i in 0 until templateListModel.size) this.add(templateListModel.get(i).getCodeTemplate())
        }.toMutableList()

    private fun readFromFile(): CompletableFuture<String> {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(XML)
        descriptor.title = CHOOSE_FILE_TO_IMPORT
        val result = CompletableFuture<String>()
        FileChooser.chooseFile(descriptor, null, mainPane, null) {
            result.complete(FileDocumentManager.getInstance().getDocument(it)?.text)
        }
        return result
    }

    fun refresh(templates: MutableList<CodeTemplate>) {
        this.templateListModel.removeAllElements()
        resetTabPane(templates)
    }

    private fun resetTabPane(templates: MutableList<CodeTemplate>) {
        templates.forEach { templateListModel.addElement(TemplateEditPane(it)) }
        templateList.selectedIndex = 0
    }

    private fun saveToFile(content: String) {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
        descriptor.title = CHOOSE_DIRECTORY_TO_EXPORT
        descriptor.description = "Save to directory $DEFAULT_XML_EXPORT_PATH or the file to overwrite"
        FileChooser.chooseFile(descriptor, null, mainPane, null) {
            val targetPath = when (it.isDirectory) {
                true -> "${it.path}$FORWARD_SLASH$DEFAULT_XML_EXPORT_PATH"
                false -> it.path
            }
            val path = Paths.get(targetPath)
            when (it.isDirectory && Files.exists(path)) {
                true -> {
                    val result = Messages.showYesNoDialog("Overwrite file?\n$path", "Overwrite", null)
                    if (result != Messages.OK) return@chooseFile
                }
                false -> try {
                    Files.write(path, content.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
                    Messages.showMessageDialog("Exported to \n$path", "Export Successful", null)
                } catch (exceptMe: IOException) {
                    exceptMe.printStackTrace()
                    Messages.showMessageDialog("Error occurred:\n${exceptMe.message}", "Export Error", null)
                }
            }
        }
    }
}