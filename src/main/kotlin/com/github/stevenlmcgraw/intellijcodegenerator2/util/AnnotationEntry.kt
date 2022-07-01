package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.intellij.psi.PsiAnnotation

data class AnnotationEntry(val qualifiedName: String, val psiAnnotation: PsiAnnotation)
