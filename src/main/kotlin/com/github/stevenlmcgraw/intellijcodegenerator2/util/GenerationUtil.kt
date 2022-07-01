package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.github.stevenlmcgraw.intellijcodegenerator2.config.EMPTY_STRING
import com.github.stevenlmcgraw.intellijcodegenerator2.config.LINE_SEPARATOR
import com.github.stevenlmcgraw.intellijcodegenerator2.config.include.Include
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.generation.PsiElementClassMember
import com.intellij.codeInsight.generation.PsiFieldMember
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.apache.velocity.VelocityContext
import org.jetbrains.java.generate.element.GenerationHelper
import org.jetbrains.java.generate.exception.GenerateCodeException
import org.jetbrains.java.generate.exception.PluginException
import org.jetbrains.java.generate.velocity.VelocityFactory
import java.io.StringWriter

fun combineToClassMemberList(
    filteredFields: Array<PsiField>,
    filteredMethods: Array<PsiMethod>
): Array<PsiElementClassMember<*>> =
    (filteredFields.map { PsiFieldMember(it) } + filteredMethods.map { PsiMethodMember(it) }).toTypedArray()

fun convertClassMembersToPsiMembers(
    classMemberList: List<PsiElementClassMember<*>>
): List<PsiMember> = classMemberList.map { it.element }

fun getAllFields(
    clazz: PsiClass
): List<FieldEntry> = clazz.allFields.map { EntryFactory.of(it, false)!! }

fun getAllMethods(
    clazz: PsiClass
): List<MethodEntry> = clazz.allMethods.map { EntryFactory.of(it)!! }

fun getFields(
    clazz: PsiClass
): List<FieldEntry> = clazz.fields.map { EntryFactory.of(it, false)!! }

fun getImportList(
    javaFile: PsiJavaFile
): List<String> = when (val list = javaFile.importList) {
    null -> listOf()
    else -> list.importStatements.map { it.qualifiedName!! }
}

fun getMethods(
    clazz: PsiClass
): List<MethodEntry> = clazz.methods.map { EntryFactory.of(it)!! }

fun getClassTypeParameters(clazz: PsiClass): List<String> = clazz.typeParameters.map { it.name!! }

private fun getParsedIncludeLookupItems(includes: List<Include>): List<IncludeLookupItem> {
    val includeLookups = includes.map { IncludeLookupItem(it.name, it.content, it.defaultInclude) }
    return includeLookups.map { IncludeLookupItem(it.name, replaceParseExpressions(it.content, includeLookups), it.defaultInclude) }
}

fun insertMembersToContext(
    members: List<PsiMember>,
    notNullMembers: List<PsiMember>,
    context: MutableMap<String, Any>,
    postfix: String,
    sortElements: Int
): Unit {
    val fieldElements = getOnlyAsFieldEntries(members, notNullMembers, false)
    context["fields$postfix"] = fieldElements
    context["fields"] = fieldElements
    if (fieldElements.size == 1) {
        context["field$postfix"] = fieldElements[0]
        context["field"] = fieldElements[0]
    }
    context["methods$postfix"] = getOnlyAsMethodEntries(members)
    context["methods"] = getOnlyAsMethodEntries(members)

}

private fun replaceParseExpression(
    line: String,
    includeLookupItems: List<IncludeLookupItem>
): String = when (line.trim().startsWith("#parse")) {
    false -> line
    true -> {
        val includeName = line.trim()
            .replace("#parse(", EMPTY_STRING)
            .replace(")", EMPTY_STRING)
            .replace("\"", EMPTY_STRING)
        val includeContent = includeLookupItems.filter { it.name == includeName }.map { it.content }.firstOrNull()
        if (includeContent.isNullOrEmpty()) line else includeContent
    }
}

private fun replaceParseExpressions(
    template: String,
    includeLookupItems: List<IncludeLookupItem>
): String = template.lines()
    .map { replaceParseExpression(it, includeLookupItems) }
    .joinToString { System.getProperty(LINE_SEPARATOR) }

fun displayErrorMessageAndRethrowRuntimeExceptionIfApplicable(
    project: Project,
    throwable: Throwable
) {
    when (throwable) {
        is GenerateCodeException -> Messages.showMessageDialog(
            project,
            "Velocity error generating code - see IDEA log for more details " +
                    "(stacktrace should be in idea.log):\n ${throwable.message}",
            "Warning",
            Messages.getWarningIcon()
        )
        is PluginException -> Messages.showMessageDialog(
            project,
            "A PluginException was thrown while performing the action - see IDEA log for details " +
                    "(stacktrace should be in idea.log):\n ${throwable.message}",
            "Warning",
            Messages.getWarningIcon()
        )
        is RuntimeException -> {
            Messages.showMessageDialog(
                project,
                "An unrecoverable exception was thrown while performing the action - see IDEA log for details " +
                        "(stacktrace should be in idea.log):\n ${throwable.message}",
                "Warning",
                Messages.getErrorIcon()
            )
            throw throwable
        }
        else -> {
            Messages.showMessageDialog(
                project,
                "An unrecoverable exception was thrown while performing the action - see IDEA log for details " +
                        "(stacktrace should be in idea.log):\n ${throwable.message}",
                "Warning",
                Messages.getErrorIcon()
            )
            throw RuntimeException(throwable)
        }
    }
}

private fun updateTemplateWithIncludes(
    template: String,
    includes: List<Include>
): String? {
    val includeLookups: List<IncludeLookupItem> = getParsedIncludeLookupItems(includes)
    val defaultImportParseExpression = includeLookups.filter { it.defaultInclude }
        .map { "#parse(${it.name})" }
        .joinToString { System.getProperty(LINE_SEPARATOR) }
    val templateWithDefaultImports = "$defaultImportParseExpression${System.getProperty(LINE_SEPARATOR)}$template"
    return replaceParseExpressions(templateWithDefaultImports, includeLookups)
}

fun velocityEvaluate(
    project: Project,
    psiFile: PsiFile,
    contextMap: Map<String, Any>,
    outputContext: MutableMap<String, Any>?,
    template: String?,
    includes: List<Include>
): String? =
    when (template.isNullOrEmpty()) {
        true -> null
        false -> {
            val stringWriter = StringWriter()
            try {

                val velocityContext = VelocityContext()
                velocityContext.put("settings", CodeStyle.getLanguageSettings(psiFile, JavaLanguage.INSTANCE))
                velocityContext.put("project", project)
                velocityContext.put("helper", GenerationHelper::class.java)
                velocityContext.put("StringUtil", StringUtil::class.java)
                velocityContext.put("NameUtil", NameUtil::class.java)
                velocityContext.put("PsiShortNamesCache", PsiShortNamesCache::class.java)
                velocityContext.put("JavaPsiFacade", JavaPsiFacade::class.java)
                velocityContext.put("GlobalSearchScope", GlobalSearchScope::class.java)
                velocityContext.put("EntryFactory", EntryFactory::class.java)
                contextMap.entries.forEach { velocityContext.put(it.key, it.value) }
                val templateWithIncludes = updateTemplateWithIncludes(template, includes)
                val velocity = VelocityFactory.getVelocityEngine()
                velocity.evaluate(
                    velocityContext,
                    stringWriter,
                    "GenerationUtil::velocityEvaluate",
                    templateWithIncludes
                )
                if (outputContext != null) velocityContext.keys.filterIsInstance<String>()
                    .forEach { outputContext[it] = velocityContext.get(it) }
                StringUtil.convertLineSeparators(stringWriter.buffer.toString())
            }
            catch (exceptMe: ProcessCanceledException) { throw exceptMe }
            catch (exceptMe: Exception) { throw GenerateCodeException(exceptMe) }
        }
    }

private data class IncludeLookupItem(val name: String, val content: String, val defaultInclude: Boolean)
