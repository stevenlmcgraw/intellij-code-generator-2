package com.github.stevenlmcgraw.intellijcodegenerator2.worker

import com.github.stevenlmcgraw.intellijcodegenerator2.config.CodeTemplate
import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.Include
import com.github.stevenlmcgraw.intellijcodegenerator2.util.ConflictResolutionPolicy
import com.github.stevenlmcgraw.intellijcodegenerator2.util.displayErrorMessageAndRethrowRuntimeExceptionIfApplicable
import com.github.stevenlmcgraw.intellijcodegenerator2.util.isDuplicateAllOrReplaceAll
import com.github.stevenlmcgraw.intellijcodegenerator2.util.velocityEvaluate
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.GenerationInfo
import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.util.IncorrectOperationException
import org.jetbrains.java.generate.config.DuplicationPolicy
import org.jetbrains.java.generate.config.InsertWhere

class JavaBodyWorker {

    companion object {

        fun execute(
            codeTemplate: CodeTemplate,
            includes: MutableList<Include>,
            parentClass: PsiClass,
            psiFile: PsiFile,
            editor: Editor,
            context: Map<String, Any>
        ) {
            val project = parentClass.project
            val body = velocityEvaluate(project, psiFile, context, null, codeTemplate.template, includes)
            val fakeClass: PsiClass = try {
                val element = PsiFileFactory.getInstance(parentClass.project)
                    .createFileFromText("filename", JavaFileType.INSTANCE, "class X {$body}").lastChild
                CodeStyleManager.getInstance(parentClass.project).reformat(element) as PsiClass

            }
            catch (exceptMe: IncorrectOperationException) {
                HintManager.getInstance().showErrorHint(editor, "Failed to generate code from template.")
                return
            }
            val membersToDelete = mutableListOf<PsiMember>()
            val generationInfoList = mutableListOf<GenerationInfo>()
            var doNotAskAgain = false
            var policy = ConflictResolutionPolicy.DUPLICATE
            val allMembersList = listOf(fakeClass.fields, fakeClass.methods, fakeClass.innerClasses)
            allMembersList.forEach {
                val existingMember = when (it) {
                    is PsiField -> parentClass.findFieldByName(it.name, false)
                    is PsiMethod -> parentClass.findMethodBySignature(it, false)
                    is PsiClass -> parentClass.findInnerClassByName(it.name, false)
                    else -> it as PsiMember //TODO find better thing to do here
                }
                if (!doNotAskAgain) {
                    policy = handleMemberExists(codeTemplate, it as PsiMember, existingMember)
                    doNotAskAgain = policy.isDuplicateAllOrReplaceAll()
                }
                when (policy) {
                    ConflictResolutionPolicy.CANCEL -> return
                    ConflictResolutionPolicy.REPLACE, ConflictResolutionPolicy.REPLACE_ALL -> {
                        if (existingMember != null) membersToDelete.add(existingMember)
                    }
                    else -> {}
                }
                generationInfoList.add(PsiGenerationInfo(it as PsiMember, false))
            }
            handleWriteCommandAction(project, editor, parentClass, generationInfoList, membersToDelete, codeTemplate)
        }

        private fun handleMemberExists(
            codeTemplate: CodeTemplate,
            member: PsiMember,
            existingMember: PsiMember?
        ): ConflictResolutionPolicy {
            return when (codeTemplate.whenDuplicatesOption) {
                DuplicationPolicy.ASK -> {
                    if (existingMember != null) handlePolicyAskAndExistingMemberNotNull(member)
                    else ConflictResolutionPolicy.DUPLICATE
                }
                DuplicationPolicy.REPLACE -> ConflictResolutionPolicy.REPLACE
                else -> ConflictResolutionPolicy.DUPLICATE
            }

        }

        private fun handlePolicyAskAndExistingMemberNotNull(
            member: PsiMember
        ): ConflictResolutionPolicy {
            val builder = DialogBuilder().title("Replace existing member: ${member.name}?")
            builder.addOkAction()
            builder.addCancelAction()
            val exitCode = Messages.showDialog(
                "Replace existing member: ${member.name}?",
                "Member Already Exists",
                arrayOf("Yes for All", "Yes", "Cancel", "No", "No for All"),
                1,
                3,
                Messages.getQuestionIcon(),
                null
            )
            return when (exitCode) {
                0 -> ConflictResolutionPolicy.REPLACE_ALL
                1 -> ConflictResolutionPolicy.REPLACE
                2 -> ConflictResolutionPolicy.CANCEL
                3 -> ConflictResolutionPolicy.DUPLICATE
                4 -> ConflictResolutionPolicy.DUPLICATE_ALL
                else -> ConflictResolutionPolicy.DUPLICATE
            }
        }

        private fun handleWriteCommandAction(
            project: Project,
            editor: Editor,
            parentClass: PsiClass,
            generationInfoList: List<GenerationInfo>,
            membersToDelete: List<PsiMember>,
            codeTemplate: CodeTemplate
        ) = try {
            membersToDelete.forEach { (it as PsiElement).delete() }
            val offset = when (codeTemplate.insertNewMethodOption) {
                InsertWhere.AT_CARET -> editor.caretModel.offset
                InsertWhere.AT_THE_END_OF_A_CLASS -> parentClass.textRange.endOffset - 1
                else -> 0
            }
            GenerateMembersUtil.insertMembersAtOffset(parentClass.containingFile, offset, generationInfoList)
            JavaCodeStyleManager.getInstance(parentClass.project).shortenClassReferences(parentClass.containingFile)
        } catch (exceptMe: Exception) {
            exceptMe.printStackTrace()
            displayErrorMessageAndRethrowRuntimeExceptionIfApplicable(project, exceptMe)
        }
    }


}