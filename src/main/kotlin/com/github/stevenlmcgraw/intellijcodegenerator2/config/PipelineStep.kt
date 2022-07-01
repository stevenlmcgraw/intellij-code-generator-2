package com.github.stevenlmcgraw.intellijcodegenerator2.config

interface PipelineStep {
    fun type(): String
    fun postfix(): String
    fun postfix(postfix: String)
    fun enabled(): Boolean
    fun enabled(enabled: Boolean)
}