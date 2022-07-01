package com.github.stevenlmcgraw.intellijcodegenerator2.worker

import com.github.stevenlmcgraw.intellijcodegenerator2.config.CodeTemplate
import com.github.stevenlmcgraw.intellijcodegenerator2.config.DOT_JAVA_SUFFIX
import com.github.stevenlmcgraw.intellijcodegenerator2.config.EMPTY_STRING
import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.Include
import com.github.stevenlmcgraw.intellijcodegenerator2.data.PackageNameClassName
import com.github.stevenlmcgraw.intellijcodegenerator2.data.TargetPathTargetDirectory
import com.github.stevenlmcgraw.intellijcodegenerator2.util.displayErrorMessageAndRethrowRuntimeExceptionIfApplicable
import com.github.stevenlmcgraw.intellijcodegenerator2.util.findOrCreateDirectoryForPackage
import com.github.stevenlmcgraw.intellijcodegenerator2.util.findSourceDirectoryByModuleName
import com.github.stevenlmcgraw.intellijcodegenerator2.util.velocityEvaluate
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.psi.impl.file.PsiDirectoryImpl
import org.jetbrains.kotlin.konan.file.File

class JavaClassWorker {

    companion object {

        fun execute(
            codeTemplate: CodeTemplate,
            includes: List<Include>,
            psiJavaFile: PsiJavaFile,
            contextMap: MutableMap<String, Any>
        ) {
            try {
                val project = psiJavaFile.project
                val fqClass = velocityEvaluate(
                    project,
                    psiJavaFile,
                    contextMap,
                    contextMap,
                    codeTemplate.template,
                    includes
                ) ?: EMPTY_STRING
                val packageAndClassName = when (val idx = fqClass.lastIndexOf('.')) {
                    in 0..Int.MAX_VALUE -> PackageNameClassName(
                        packageName = fqClass.substring(0, idx),
                        className = fqClass.substring(idx + 1)
                    )
                    else -> PackageNameClassName(packageName = EMPTY_STRING, className = fqClass)
                }
                contextMap["PackageName"] = packageAndClassName.packageName
                contextMap["ClassName"] = packageAndClassName.className
                val content = velocityEvaluate(
                    project,
                    psiJavaFile,
                    contextMap,
                    null,
                    codeTemplate.template,
                    includes
                ) ?: EMPTY_STRING
                val selectedPackage = psiJavaFile.packageName
                val targetPackageName = packageAndClassName.packageName.ifEmpty { selectedPackage }
                val currentModule = ModuleUtilCore.findModuleForPsiElement(psiJavaFile)
                assert(currentModule != null)
                val moduleRoot = ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(psiJavaFile.virtualFile)
                assert(moduleRoot != null)
                val moduleRootDir = PsiDirectoryFactory.getInstance(project).createDirectory(moduleRoot!!)
                val manager = VirtualFileManager.getInstance()
                val targetPackagePsiDir = when (codeTemplate.defaultTargetModule?.isNotBlank()) {
                    true -> findSourceDirectoryByModuleName(project, codeTemplate.defaultTargetModule!!)
                    else -> findOrCreateDirectoryForPackage(
                        project,
                        currentModule!!,
                        targetPackageName,
                        moduleRootDir,
                        true,
                        codeTemplate.alwaysPromptForPackage
                    )
                } ?: return //bail out if null
                val targetFileName = "${packageAndClassName.className}$DOT_JAVA_SUFFIX"
                val targetPathAndDirectory = when (codeTemplate.defaultTargetPackage?.isBlank()) {
                    true -> {
                        val subDirectoryPath = (codeTemplate.defaultTargetPackage ?: EMPTY_STRING).replace(".", File.separator)
                        val targetDirectory = "${targetPackagePsiDir.virtualFile.path}${File.separator}$subDirectoryPath"
                        TargetPathTargetDirectory(
                            targetPath = "$targetDirectory${File.separator}$targetFileName",
                            targetDirectory = targetDirectory
                        )
                    }
                    else -> TargetPathTargetDirectory(
                        targetPath = "${targetPackagePsiDir.virtualFile}${File.separator}$targetFileName",
                        targetDirectory = targetPackagePsiDir.virtualFile.path
                    )
                }
                val targetVirtualFile = manager.refreshAndFindFileByUrl(VfsUtil.pathToUrl(targetPathAndDirectory.targetPath))
                if (targetVirtualFile != null && targetVirtualFile.exists() && !userConfirmedOverride()) return
                val targetDirVirtualFile = manager.refreshAndFindFileByUrl(VfsUtil.pathToUrl(targetPathAndDirectory.targetDirectory))
                val targetPsiDirectory = PsiDirectoryImpl(PsiManagerImpl(project), targetDirVirtualFile!!)
                val targetFile = PsiFileFactory.getInstance(project).createFileFromText(
                    "${packageAndClassName.className}$DOT_JAVA_SUFFIX",
                    JavaFileType.INSTANCE,
                    content
                )
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(targetFile)
                CodeStyleManager.getInstance(project).reformat(targetFile)
                handleWriteCommandAction(project, targetPsiDirectory, targetFile, targetFileName)
            }
            catch (exceptMe: Exception) { exceptMe.printStackTrace() }
        }

        private fun handleWriteCommandAction(
            project: Project,
            targetPsiDirectory: PsiDirectory,
            targetFile: PsiFile,
            targetFileName: String
        ) = WriteCommandAction.runWriteCommandAction(project) {
            try {
                targetPsiDirectory.findFile(targetFileName)?.delete()
                targetPsiDirectory.add(targetFile)
                val addedFile = targetPsiDirectory.findFile(targetFile.name)
                ApplicationManager.getApplication().invokeLater {
                    FileEditorManager.getInstance(project).openFile(addedFile!!.virtualFile, true, true)
                }
            }
            catch (exceptMe: Exception) {
                exceptMe.printStackTrace()
                displayErrorMessageAndRethrowRuntimeExceptionIfApplicable(project, exceptMe)
            }
        }

        private fun userConfirmedOverride(): Boolean =
            Messages.showYesNoDialog("Overwrite?", "File Exists", null) == Messages.OK
    }
}