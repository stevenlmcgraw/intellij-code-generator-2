package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiJavaFile
import org.jetbrains.java.generate.element.ClassElement

data class ClassEntry(val raw: PsiClass, val element: ClassElement) {

    private val allFields: List<FieldEntry>
//    private val allMembers: List<MemberEntry>
    private val allMethods: List<MethodEntry>
    private val fields: List<FieldEntry>
    private val importList: List<String>
//    private val members: List<MemberEntry>
    private val methods: List<MethodEntry>
    private val packageName: String
    private val typeParamList: List<String>

    init {
        val psiFile = this.raw.containingFile
        this.allFields = getAllFields(this.raw)
        this.allMethods = getAllMethods(this.raw)
        this.fields = getFields(this.raw)
        this.importList = getImportList(psiFile as PsiJavaFile)
        this.methods = getMethods(this.raw)
        this.packageName = (psiFile as PsiClassOwner).packageName
        this.typeParamList = getClassTypeParameters(this.raw)
    }
}