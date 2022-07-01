package com.github.stevenlmcgraw.intellijcodegenerator2.worker

import com.github.stevenlmcgraw.intellijcodegenerator2.config.CodeTemplate
import com.github.stevenlmcgraw.intellijcodegenerator2.config.EMPTY_STRING
import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.Include
import com.github.stevenlmcgraw.intellijcodegenerator2.util.velocityEvaluate
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class JavaCaretWorker {

    companion object {

        fun execute(
            codeTemplate: CodeTemplate,
            includes: List<Include>,
            psiJavaFile: PsiJavaFile,
            editor: Editor,
            contextMap: Map<String, Any>
        ) {
            val project = psiJavaFile.project
            val content = velocityEvaluate(
                project,
                psiJavaFile,
                contextMap,
                null,
                codeTemplate.template,
                includes
            ) ?: EMPTY_STRING
            val document = editor.document
            val selectionModel = editor.selectionModel
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            handleWriteCommandAction(project, document, editor, psiJavaFile, start, end, content)
            selectionModel.removeSelection()
        }

        private fun handleWriteCommandAction(
            project: Project,
            document: Document,
            editor: Editor,
            psiJavaFile: PsiJavaFile,
            startOffset: Int,
            endOffset: Int,
            content: String
        ) = WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(startOffset, endOffset, content)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            val element = psiJavaFile.findElementAt(editor.caretModel.offset)
            val clazz = PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false)
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(clazz!!.containingFile)
        }
    }
}