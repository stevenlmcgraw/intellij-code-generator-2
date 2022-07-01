package com.github.stevenlmcgraw.intellijcodegenerator2.util

enum class ConflictResolutionPolicy {
    CANCEL,
    DUPLICATE,
    DUPLICATE_ALL,
    REPLACE,
    REPLACE_ALL,
}

fun ConflictResolutionPolicy.isDuplicateAllOrReplaceAll() =
    this == ConflictResolutionPolicy.DUPLICATE_ALL || this == ConflictResolutionPolicy.REPLACE_ALL