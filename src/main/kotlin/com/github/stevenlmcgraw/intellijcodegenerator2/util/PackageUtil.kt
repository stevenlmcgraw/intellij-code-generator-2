package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.github.stevenlmcgraw.intellijcodegenerator2.data.AskToCreatePair
import com.github.stevenlmcgraw.intellijcodegenerator2.data.FoundPsiElementPair
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.impl.ProjectRootUtil
import com.intellij.openapi.roots.ModulePackageIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException
import com.intellij.util.Query
import java.io.File
import java.io.IOException


fun findOrCreateDirectoryForPackage(
    project: Project,
    module: Module,
    packageName: String,
    baseDir: PsiDirectory,
    shouldAskUserToCreate: Boolean? = true,
    shouldAlwaysPrompt: Boolean
): PsiDirectory? {
    val foundPsiDirectoryPair = when (!shouldAlwaysPrompt && packageName.isNotEmpty()) {
        true -> alwaysPromptFalseAndPackageNameNotEmpty(project, module, packageName, baseDir)
        false -> alwaysPromptTrueAndPackageNameMaybeEmpty(project, packageName, baseDir)
    }
    when (foundPsiDirectoryPair.found) {
        false -> return null
        true -> {
            var result: PsiDirectory? = null
            var restOfName = packageName
            var haveAskedToCreate = false
            while (restOfName.isNotEmpty()) {
                val name = getLeftPart(restOfName)
                val discoveredExistingDirectory = (foundPsiDirectoryPair.element as PsiDirectory).findSubdirectory(name)
                if (discoveredExistingDirectory != null) result = discoveredExistingDirectory
                else {
                    val askToCreatePair = handleAskToCreateIfDidNotFindExistingSubdirectory(
                        packageName,
                        shouldAskUserToCreate!!,
                        haveAskedToCreate
                    )
                    haveAskedToCreate = askToCreatePair.asked
                    if (!askToCreatePair.keepGoing) return null
                    else {
                        try {
                            result = WriteAction.compute<PsiDirectory, Exception> {
                                foundPsiDirectoryPair.element.createSubdirectory(name)
                            }
                        }
                        catch (ex: IncorrectOperationException) { throw ex }
                        catch (ex: IOException) { throw IncorrectOperationException(ex) }
                        catch (ex: Exception) { throw ex }
                    }
                }
                restOfName = cutLeftPart(restOfName)
            }
            return result
        }
    }
}

private fun handleAskToCreateIfDidNotFindExistingSubdirectory(
    packageName: String,
    shouldAskUserToCreate: Boolean,
    haveAskedUserToCreate: Boolean
): AskToCreatePair {
    return if (!haveAskedUserToCreate && shouldAskUserToCreate) {
        when (ApplicationManager.getApplication().isUnitTestMode) {
            true -> AskToCreatePair(asked = true, keepGoing = true)
            false -> {
                when (showYesNoDialog(packageName)) {
                    Messages.YES -> AskToCreatePair(asked = true, keepGoing = true)
                    else -> AskToCreatePair(asked = true, keepGoing = false)
                }
            }
        }
    }
    else return AskToCreatePair(asked = true, keepGoing = true)
}

private fun showYesNoDialog(packageName: String): Int = Messages.showYesNoDialog(
    IdeBundle.message("prompt.create.non.existing.package", packageName),
    IdeBundle.message("title.package.not.found"),
    Messages.getQuestionIcon()
)

fun alwaysPromptFalseAndPackageNameNotEmpty(
    project: Project,
    module: Module,
    packageName: String,
    baseDir: PsiDirectory
): FoundPsiElementPair {
    val rootPackage = findLongestExistingPackage(module, packageName)
        ?: findLongestExistingPackage(project, packageName)
    return when (rootPackage) {
        null -> FoundPsiElementPair(found = false, element = null)
        else -> {
            val beginIdx = rootPackage.qualifiedName.length + 1
            val packageNameSubstring = if (beginIdx < packageName.length) packageName.substring(beginIdx) else ""
            val packageNameSeparatorsReplaced = packageNameSubstring.replace('.', File.separatorChar)
            val postfixToShow =
                if (packageNameSeparatorsReplaced.isNotEmpty()) "${File.separatorChar}$packageNameSeparatorsReplaced"
                else packageNameSeparatorsReplaced
            val moduleDirectories = getPackageDirectoriesInModule(rootPackage, module)
            val initDir = findDirectory(moduleDirectories, baseDir)
            val psiDirectory = DirectoryChooserUtil.selectDirectory(project, moduleDirectories, initDir, postfixToShow)
            FoundPsiElementPair(found = psiDirectory != null, element = psiDirectory)
        }
    }
}

fun alwaysPromptTrueAndPackageNameMaybeEmpty(
    project: Project,
    packageName: String,
    baseDir: PsiDirectory
): FoundPsiElementPair {
    val sourceDirectories = ProjectRootUtil.getSourceRootDirectories(project)
    val initDir = findDirectory(sourceDirectories, baseDir)
    val postfixToShow = when (packageName.isEmpty()) {
        true -> ""
        false -> "${File.separatorChar}${packageName.replace('.', File.separatorChar)}"
    }
    val psiDirectory = DirectoryChooserUtil.selectDirectory(project, sourceDirectories, initDir, postfixToShow)
    return FoundPsiElementPair(found = psiDirectory != null, element = psiDirectory)
}

fun findSourceDirectoryByModuleName(
    project: Project,
    moduleName: String
): PsiDirectory = ProjectRootUtil.getSourceRootDirectories(project).first { it.virtualFile.path.contains(moduleName) }

fun getPackageDirectoriesInModule(
    rootPackage: PsiPackage,
    module: Module
): Array<PsiDirectory> = rootPackage.getDirectories(GlobalSearchScope.moduleScope(module))



private fun cutLeftPart(packageName: String): String {
    val idx = packageName.indexOf('.')
    return if (idx > -1) packageName.substring(idx + 1..idx)
    else ""
}

private fun findDirectory(
    directories: Array<PsiDirectory>,
    baseDir: PsiDirectory
): PsiDirectory? = directories.find { it.virtualFile == baseDir.virtualFile }


private fun findLongestExistingPackage(
    module: Module?,
    packageName: String
): PsiPackage? {
    when (module) {
        null -> return null
        else -> {
            val manager = PsiManager.getInstance(module.project)
            var nameToMatch = packageName
            while (true) {
                val vFiles = ModulePackageIndex.getInstance(module).getDirsByPackageName(nameToMatch, false)
                val directory = getWritableModuleDirectory(vFiles, module, manager)
                if (directory != null) return JavaDirectoryService.getInstance().getPackage(directory)
                val lastDotIndex = nameToMatch.lastIndexOf('.')
                if (lastDotIndex > -1) nameToMatch = nameToMatch.substring(0, lastDotIndex)
                else return null
            }
        }
    }
}

private fun findLongestExistingPackage(
    project: Project,
    packageName: String
): PsiPackage? {
    val manager = PsiManager.getInstance(project)
    var nameToMatch = packageName
    while (true) {
        val somePackage = JavaPsiFacade.getInstance(manager.project) as PsiPackage?
        when (somePackage != null && isWritablePackage(somePackage)) {
            true -> return somePackage
            false -> {
                val lastDotIndex = nameToMatch.lastIndexOf('.')
                if (lastDotIndex > -1) nameToMatch = nameToMatch.substring(0, lastDotIndex)
                else return null
            }
        }
    }
}

private fun getLeftPart(packageName: String): String {
    val idx = packageName.indexOf('.')
    return if (idx > -1) packageName.substring(0..idx)
    else packageName
}

private fun getWritableModuleDirectory(
    vFiles: Query<VirtualFile>,
    module: Module,
    manager: PsiManager
): PsiDirectory? {
    val file = vFiles.find { ModuleUtil.findModuleForFile(it, module.project) == module }
    val directory = manager.findDirectory(file!!)
    return when (directory != null && directory.isValid && directory.isWritable) {
        true -> directory
        false -> null
    }
}

private fun isWritablePackage(somePackage: PsiPackage): Boolean = somePackage.directories.any { it.isValid && it.isWritable }