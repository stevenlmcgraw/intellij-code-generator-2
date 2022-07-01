package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod

fun <T : PsiMember> getOnlyAsFieldAndMethodElements(
    members: Collection<T>,
    selectedNotNullMembers: Collection<T>,
    useAccessors: Boolean
): List<MemberEntry<*>?> = members.filter { it is PsiField || it is PsiMethod }
        .map {
            when (it) {
                is PsiField -> {
                    val fieldEntry = EntryFactory.of(it as PsiField, useAccessors)
                    if (selectedNotNullMembers.contains(it)) fieldEntry!!.isNotNull = true
                    fieldEntry
                }
                is PsiMethod -> {
                    val methodEntry = EntryFactory.of(it as PsiMethod)
                    if (selectedNotNullMembers.contains(it)) methodEntry!!.isNotNull = true
                    methodEntry
                }
                else -> null
            }
        }

fun <T : PsiMember> getOnlyAsFieldEntries(
    members: Collection<T>,
    selectedNotNullMembers: Collection<T>,
    useAccessors: Boolean
): List<FieldEntry> = members.filter { it is PsiField }
    .map {
        val fieldEntry = EntryFactory.of(it as PsiField, useAccessors)
        if (selectedNotNullMembers.contains(it)) fieldEntry!!.isNotNull = true
        fieldEntry!!
    }

fun <T : PsiMember> getOnlyAsMethodEntries(
    members: Collection<T>
): List<MethodEntry> = members.filter { it is PsiMethod }.map { EntryFactory.of(it as PsiMethod)!! }