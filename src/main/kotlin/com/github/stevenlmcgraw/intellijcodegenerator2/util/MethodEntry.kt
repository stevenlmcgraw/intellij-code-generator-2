package com.github.stevenlmcgraw.intellijcodegenerator2.util

import com.intellij.psi.PsiMethod
import org.jetbrains.java.generate.element.MethodElement

data class MethodEntry(val raw: PsiMethod, val element: MethodElement): MemberEntry<PsiMethod> {

    override fun getRawMember(): PsiMethod = this.raw

    override fun getAccessor(): String = this.element.accessor

    override fun getName(): String = this.element.name

    override fun isArray(): Boolean = this.element.isArray

    override fun isNestedArray(): Boolean = this.element.isNestedArray

    override fun isCollection(): Boolean = this.element.isCollection

    override fun isMap(): Boolean = this.element.isMap

    override fun isPrimitive(): Boolean = this.element.isPrimitive

    override fun isString(): Boolean = this.element.isString

    override fun isPrimitiveArray(): Boolean = this.element.isPrimitiveArray

    override fun isObjectArray(): Boolean = this.element.isObjectArray

    override fun isNumeric(): Boolean = this.element.isNumeric

    override fun isObject(): Boolean = this.element.isObject

    override fun isDate(): Boolean = this.element.isDate

    override fun isSet(): Boolean = this.element.isSet

    override fun isList(): Boolean = this.element.isList

    override fun isStringArray(): Boolean = this.element.isStringArray

    override fun isCalendar(): Boolean = this.element.isCalendar

    override fun isBoolean(): Boolean = this.element.isBoolean

    override fun isLong(): Boolean = this.element.isLong

    override fun isShort(): Boolean = this.element.isShort

    override fun isChar(): Boolean = this.element.isChar

    override fun isFloat(): Boolean = this.element.isFloat

    override fun isDouble(): Boolean = this.element.isDouble

    override fun isByte(): Boolean = this.element.isByte

    override fun isVoid(): Boolean = this.element.isVoid

    override fun isNotNull(): Boolean = this.element.isNotNull

    override fun getTypeName(): String = this.element.typeName

    override fun getTypeQualifiedName(): String = this.element.typeQualifiedName

    override fun getType(): String = this.element.type

    fun setNotNull(truthy: Boolean) {
        this.element.isNotNull = truthy
    }
}
