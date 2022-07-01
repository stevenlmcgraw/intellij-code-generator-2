package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.intellij.psi.PsiJvmModifiersOwner
import com.intellij.psi.PsiMember
import org.jetbrains.java.generate.element.Element

interface MemberEntry<T : PsiMember>: Element {
    fun getRawMember(): T

    fun isAnnotatedWith(
        psiJvmModifierOwner: PsiJvmModifiersOwner,
        qualifiedName: String
    ): Boolean = psiJvmModifierOwner.annotations.filter { it.qualifiedName != null }.any { it.qualifiedName == qualifiedName }
}