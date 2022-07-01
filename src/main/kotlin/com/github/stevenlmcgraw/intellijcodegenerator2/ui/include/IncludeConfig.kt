package com.github.stevenlmcgraw.intellijcodegenerator2.ui.include

import com.github.stevenlmcgraw.intellijcodegenerator2.config.*
import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.Include
import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.IncludeList
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
class IncludeConfig(private val settings: CodeGeneratorSettings) {

    private val mainPane: JPanel = JPanel()
    private val addTemplateButton: JButton = JButton()
    private val splitPane: JSplitPane = JSplitPane()
    private val includeList: JList<IncludeEditPane> = com.intellij.ui.components.JBList()
    private val includeListModel: DefaultListModel<IncludeEditPane> = DefaultListModel()
    private val deleteButton: JButton = JButton()
    private val splitRightPane: JPanel = JPanel()
    private val scrollPane: JScrollPane = com.intellij.ui.components.JBScrollPane()
    private val importButton: JButton = JButton()
    private val exportButton: JButton = JButton()
    private val exportAllButton: JButton = JButton()

    init {
        this.includeList.model = this.includeListModel
        includeList.addListSelectionListener {
            when (it.valueIsAdjusting) {
                true -> return@addListSelectionListener
                false -> {
                    val length = includeListModel.size
                    val idx = includeList.selectedIndex
                    if (length < 0 || idx < 0 || idx >= length) {
                        splitPane.rightComponent = splitRightPane
                        deleteButton.isEnabled = false
                        return@addListSelectionListener
                    }
                    val pane = includeListModel.get(includeList.selectedIndex)
                    deleteButton.isEnabled = true
                    splitPane.rightComponent = pane.templateEdit()
                }
        }
        }
        addTemplateButton.addActionListener {
            val template = Include()
            template.name = UNTITLED
            val editPane = IncludeEditPane(template)
            val model = includeList.model as DefaultListModel<IncludeEditPane>
            model.addElement(editPane)
            includeList.selectedIndex = model.size - 1
        }
        exportButton.addActionListener {
            val idx = includeList.selectedIndex
            val includeModel = includeListModel.get(idx)
            val xml = IncludeList.toXML(includeModel.getInclude())
            saveToFile(xml, this.mainPane)
        }
        exportAllButton.addActionListener {
            val templates =
                if (this.includeListModel.size == 0) mutableListOf()
                else buildList<Include> {
                    for (i in 0 until includeListModel.size) includeListModel.get(i).getInclude()
                }.toMutableList()
            val xml = IncludeList.toXML(templates)
            saveToFile(xml, mainPane)
        }
        importButton.addActionListener {
            readFromFile().thenAccept { xml ->
                try {
                    val templates = IncludeList.fromXML(xml)
                    val currentTemplates = getIncludes().apply { addAll(templates) }
                    refresh(currentTemplates)
                    Messages.showMessageDialog(IMPORT_FINISHED, IMPORT, null)
                }
                catch (exceptMe: Exception) {
                    exceptMe.printStackTrace()
                    Messages.showMessageDialog(IMPORT_FAILED, IMPORT_ERROR, null)
                }
            }
        }
        resetTabPane(settings.getIncludes())
    }

    fun getIncludes(): MutableList<Include> =
        if (this.includeListModel.size == 0) mutableListOf()
        else buildList {
            for (i in 0 until includeListModel.size) this.add(includeListModel.get(i).getInclude())
        }.toMutableList()

    fun getMainPane(): JPanel = this.mainPane

    private fun readFromFile(): CompletableFuture<String> {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(XML)
        descriptor.title = CHOOSE_FILE_TO_IMPORT
        val result = CompletableFuture<String>()
        FileChooser.chooseFile(descriptor, null, mainPane, null) {
            result.complete(FileDocumentManager.getInstance().getDocument(it)?.text)
        }
        return result
    }

    fun refresh(templates: MutableList<Include>) {
        includeListModel.removeAllElements()
        resetTabPane(templates)
    }

    private fun resetTabPane(includes: MutableList<Include>) {
        includes.forEach { includeListModel.addElement(IncludeEditPane(it)) }
        includeList.selectedIndex = 0
    }

    private fun saveToFile(
        content: String,
        mainPane: JPanel
    ) {
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