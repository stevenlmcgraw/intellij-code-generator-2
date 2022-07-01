package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.java.generate.element.ElementFactory
import org.jetbrains.java.generate.element.FieldElement

class EntryFactory {
    companion object {

        fun of(
            field: PsiField?,
            useAccessor: Boolean
        ): FieldEntry? = when (field) {
            null -> null
            else -> FieldEntry(field, ElementFactory.newFieldElement(field, useAccessor))
        }

        fun of(
            clazz: PsiClass?,
            element: FieldElement?
        ): FieldEntry? =
            if (clazz == null || element == null) null
            else FieldEntry(clazz.findFieldByName(element.name, true)!!, element)


        fun of(
            method: PsiMethod?
        ): MethodEntry? = when (method) {
            null -> null
            else -> MethodEntry(method, ElementFactory.newMethodElement(method))
        }

        fun of(
            clazz: PsiClass?
        ): ClassEntry? = when (clazz) {
            null -> null
            else -> ClassEntry(clazz, ElementFactory.newClassElement(clazz))
        }
    }
}