package com.github.stevenlmcgraw.intellijcodegenerator2.action

import com.github.stevenlmcgraw.intellijcodegenerator2.config.*
import com.github.stevenlmcgraw.intellijcodegenerator2.util.*
import com.github.stevenlmcgraw.intellijcodegenerator2.worker.JavaBodyWorker
import com.github.stevenlmcgraw.intellijcodegenerator2.worker.JavaCaretWorker
import com.github.stevenlmcgraw.intellijcodegenerator2.worker.JavaClassWorker
import com.intellij.codeInsight.generation.PsiElementClassMember
import com.intellij.codeInsight.generation.PsiFieldMember
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.util.MemberChooser
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.util.IncorrectOperationException
import net.sf.cglib.core.CodeGenerationException
import org.jetbrains.java.generate.config.Config
import org.jetbrains.kotlin.konan.file.File
import java.awt.BorderLayout
import javax.swing.JPanel

class CodeGeneratorAction(): AnAction() {

    private val log: Logger = Logger.getInstance(CodeGeneratorAction::class.java)

    private val settings: CodeGeneratorSettings
    private var templateKey: String = EMPTY_STRING
    private var templateName: String = EMPTY_STRING

    constructor(templateKey: String, templateName: String): this() {
        this.templateKey = templateKey
        this.templateName = templateName
    }

    init {
        this.templatePresentation.apply {
            this.description = DESCRIPTION
            this.setText(this@CodeGeneratorAction.templateName, false)
        }
        this.settings = ServiceManager.getService(CodeGeneratorSettings::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val codeTemplate = this.settings.getCodeTemplateById(this.templateKey)
            ?: throw IllegalStateException("Invalid template key")
        val project = e.project
        assert(project != null)
        val file = e.dataContext.getData(CommonDataKeys.PSI_FILE)
        assert(file != null && file is PsiJavaFile)
        val javaFile = file as PsiJavaFile
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)
        val map = executePipeline(codeTemplate, javaFile, editor)
        if (map.isEmpty()) return //bail out
        when (codeTemplate.type) {
            CLASS -> JavaClassWorker.execute(codeTemplate, this.settings.getIncludes(), javaFile, map.toMutableMap())
            BODY -> {
                assert(editor != null)
                when (val clazz = getSubjectClass(editor, javaFile)) {
                    null -> HintManager.getInstance().showErrorHint(editor!!, "No parent class found for cursor position.")
                    else -> JavaBodyWorker.execute(codeTemplate, this.settings.getIncludes(), clazz, javaFile, editor!!, map)
                }
            }
            CARET -> {
                assert(editor != null)
                JavaCaretWorker.execute(codeTemplate, this.settings.getIncludes(), javaFile, editor!!, map)
            }
            else -> throw IllegalStateException("Unknown template type: ${codeTemplate.type}")
        }
    }

    @Suppress("Unchecked cast")
    private fun <T : PsiElementClassMember<*>> buildClassMember(members: List<PsiMember?>): Array<out T> {
        val test = members.filter { it is PsiField || it is PsiMethod }
            .map {
                when (it) {
                    is PsiField -> PsiFieldMember(it)
                    is PsiMethod -> PsiMethodMember(it)
                    else -> null
                }
            }.toTypedArray()
        return test as Array<out T>
    }

    private fun buildFakeClassForEmptyFile(psiJavaFile: PsiJavaFile): PsiClass? {
        val project = psiJavaFile.project
        val moduleRoot = ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(psiJavaFile.virtualFile)
        val fileName = psiJavaFile.name
        val className = fileName.replace(DOT_JAVA_SUFFIX, EMPTY_STRING)
        val packageName = psiJavaFile.virtualFile.path
            .substring(moduleRoot?.path?.length?.plus(1) ?: 0)
            .replace("${File.separator}$fileName", EMPTY_STRING)
            .replace(File.separator, PERIOD)
        return try {
            val element = PsiFileFactory.getInstance(project)
                .createFileFromText(
                    FILE_NAME,
                    JavaFileType.INSTANCE,
                    "$PACKAGE$packageName;\n$CLASS$className{}"
                )
            element.lastChild as PsiClass
        }
        catch (ignore: IncorrectOperationException) { null }
    }

    private fun executePipeline(
        codeTemplate: CodeTemplate,
        psiJavaFile: PsiJavaFile,
        editor: Editor?
    ): Map<String, Any> {
        val project = psiJavaFile.project
        val map = mutableMapOf<String, Any>()
        val clazz = when (val clazz2 = getSubjectClass(editor, psiJavaFile)) {
            null -> buildFakeClassForEmptyFile(psiJavaFile)
            else -> clazz2
        }
        map[CLASS_ZERO] = EntryFactory.of(clazz)!!
        if (editor != null) {
            val offset = editor.caretModel.offset
            val context = psiJavaFile.findElementAt(offset)
            val parentMethod = PsiTreeUtil.getParentOfType(context, PsiMethod::class.java, false)
            map[PARENT_METHOD] = EntryFactory.of(parentMethod)!!
        }
        codeTemplate.pipeline.filter { it.enabled() }
            .forEach {
                when (it.type()) {
                    CLASS_SELECTION -> {
                        when (val selectedClass = selectClass(psiJavaFile, it as ClassSelectionConfig, map)) {
                            null -> {} //do nothing
                            else -> map["$CLASS${it.postfix()}"] = EntryFactory.of(selectedClass)!!
                        }
                    }
                    MEMBER_SELECTION -> {
                        when (val selectedMembers = selectMember(psiJavaFile, it as MemberSelectionConfig, map)) {
                            null -> {} //do nothing
                            else -> insertMembersToContext(selectedMembers, listOf(), map, it.postfix, it.sortElements)
                        }
                    }
                    else -> throw IllegalStateException("Unrecognized step type: ${it.type()}")
                }
            }
        return map
    }

    private fun filterMembers(
        members: List<Any?>,
        config: MemberSelectionConfig
    ): List<PsiMember?> {
        val pattern = generatorConfigToConfig(config).filterPattern
        return members
            .map {
                when (it) {
                    is PsiMember -> it
                    is MemberEntry<*> -> it.getRawMember()
                    else -> null
                }
            }
            .filter {
                when (it) {
                    is PsiField -> !pattern.fieldMatches(it)
                    is PsiMethod -> !pattern.methodMatches(it)
                    else -> false
                }
            }
    }

    private fun generatorConfigToConfig(selectionConfig: MemberSelectionConfig): Config =
        Config().apply {
            this.useFullyQualifiedName = false
            this.filterConstantField = selectionConfig.filterConstantField
            this.filterEnumField = selectionConfig.filterEnumField
            this.filterTransientModifier = selectionConfig.filterTransientModifier
            this.filterStaticModifier = selectionConfig.filterStaticModifier
            this.filterFieldName = selectionConfig.filterFieldName
            this.filterFieldType = selectionConfig.filterFieldType
            this.filterMethodName = selectionConfig.filterMethodName
            this.filterMethodType = selectionConfig.filterMethodType
            this.filterLoggers = selectionConfig.filterLoggers
            this.enableMethods = selectionConfig.enableMethods
        }

    private fun getSubjectClass(
        editor: Editor?,
        psiJavaFile: PsiJavaFile
    ): PsiClass? =
        if (editor != null) {
            val offset = editor.caretModel.offset
            when (val context = psiJavaFile.findElementAt(offset)) {
                null -> null
                else -> PsiTreeUtil.getParentOfType(context, PsiClass::class.java, false)
            }
        }
        else if (psiJavaFile.classes.isNotEmpty()) psiJavaFile.classes[0]
        else null

    private fun selectClass(
        psiJavaFile: PsiJavaFile,
        config: ClassSelectionConfig,
        contextMap: MutableMap<String, Any>
    ): PsiClass? {
        val initialClassNameTemplate = config.initialClass
        val project = psiJavaFile.project
        return try {
            val className = velocityEvaluate(
                project,
                psiJavaFile,
                contextMap,
                contextMap,
                initialClassNameTemplate,
                this.settings.getIncludes()
            )
            val initialClass =
                if (className != null && className.isEmpty())
                    JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
                        ?: if (psiJavaFile.classes.isNotEmpty()) psiJavaFile.classes[0] else null
                else null
            val chooser = TreeClassChooserFactory.getInstance(project)
                .createProjectScopeChooser("Select a Class", initialClass)
            chooser.showDialog()
            when (chooser.selected) {
                null -> null
                else -> chooser.selected
            }

        }
        catch (exceptMe: CodeGenerationException) {
            Messages.showMessageDialog(project, exceptMe.message, "Generate Failed", null)
            null
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun selectMember(
        psiJavaFile: PsiJavaFile,
        config: MemberSelectionConfig,
        contextMap: MutableMap<String, Any>
    ): List<PsiMember> {
        val project = psiJavaFile.project
        velocityEvaluate(project, psiJavaFile, contextMap, contextMap, config.providerTemplate, settings.getIncludes())
        val availableMembers = buildList {
            if (contextMap.containsKey(AVAILABLE_MEMBERS)) this.add(contextMap[AVAILABLE_MEMBERS])
        }.toMutableList()
        val selectedMembers = buildList {
            if (contextMap.containsKey(AVAILABLE_MEMBERS)) this.add(contextMap[SELECTED_MEMBERS] ?: contextMap[AVAILABLE_MEMBERS])
        }.toMutableList()
        contextMap.remove(AVAILABLE_MEMBERS)
        contextMap.remove(SELECTED_MEMBERS)
        val dialogMembers = buildClassMember<PsiElementClassMember<*>>(filterMembers(availableMembers, config))
        val membersSelected = buildClassMember<PsiElementClassMember<*>>(filterMembers(selectedMembers, config))
        when (!config.allowEmptySelection && dialogMembers.isEmpty()) {
            true -> {
                Messages.showMessageDialog(
                    project,
                    "No members are provided for selection.\nTemplate does not allow for empty selection.",
                    "Warning",
                    Messages.getWarningIcon()
                )
                return emptyList()
            }
            else -> {
                val chooser = object : MemberChooser<PsiElementClassMember<*>>(
                    dialogMembers,
                    config.allowEmptySelection,
                    config.allowMultiSelection,
                    project,
                    PsiUtil.isLanguageLevel5OrHigher(psiJavaFile),
                    JPanel(BorderLayout())
                ) {
                    override fun getHelpId(): String = "editing.altInsert.codegenerator"
                }
                chooser.title = SELECTION_FIELDS_FOR_CODE_GENERATION
                chooser.setCopyJavadocVisible(false)
                chooser.selectElements(membersSelected)
                chooser.show()
                return if (DialogWrapper.OK_EXIT_CODE != chooser.exitCode) emptyList()
                else convertClassMembersToPsiMembers(chooser.selectedElements!!)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun startInTransaction(): Boolean = true

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project
        val psiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE)
        if (project == null) presentation.isEnabled = false
        else presentation.isEnabled = !(psiFile == null || psiFile !is PsiJavaFile)
    }
}