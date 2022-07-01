package com.github.stevenlmcgraw.intellijcodegenerator2.action

import com.github.stevenlmcgraw.intellijcodegenerator2.config.CLASS
import com.github.stevenlmcgraw.intellijcodegenerator2.config.CODE_MAKER_MENU_ACTION_PREFIX
import com.github.stevenlmcgraw.intellijcodegenerator2.config.CodeGeneratorSettings
import com.github.stevenlmcgraw.intellijcodegenerator2.config.CodeTemplate
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil

class CodeGeneratorGroup(): ActionGroup(), DumbAware {

    private val settings: CodeGeneratorSettings = ServiceManager.getService(CodeGeneratorSettings::class.java)

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return AnAction.EMPTY_ARRAY //bail out
        PlatformDataKeys.PROJECT.getData(e.dataContext) ?: return AnAction.EMPTY_ARRAY //bail out if null
        val file = e.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return AnAction.EMPTY_ARRAY //bail out if null
        val caret = e.dataContext.getData(CommonDataKeys.CARET)
        val getChildrenToArray: (String, MutableList<CodeTemplate>) -> Array<AnAction> = { fileName, list ->
            list
                .filter { it.type == CLASS && it.enabled && fileName.matches(Regex(it.fileNamePattern)) }
                .map { getOrCreateAction(it) }
                .toTypedArray()
        }
        return when (caret == null) {
            false -> {
                val element = file.findElementAt(caret.offset)
                if (PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false) == null) AnAction.EMPTY_ARRAY
                else getChildrenToArray(file.name, this.settings.getCodeTemplates())
            }
            true -> getChildrenToArray(file.name, this.settings.getCodeTemplates())
        }
    }

    private fun getOrCreateAction(template: CodeTemplate): AnAction {
        val actionId = "$CODE_MAKER_MENU_ACTION_PREFIX${template.getIdString()}"
        return when (val action = ActionManager.getInstance().getAction(actionId)) {
            null -> {
                val newAction = CodeGeneratorAction(template.getIdString(), template.name)
                ActionManager.getInstance().registerAction(actionId, newAction)
                newAction
            }
            else -> action
        }
    }

    override fun hideIfNoVisibleChildren() = false
}