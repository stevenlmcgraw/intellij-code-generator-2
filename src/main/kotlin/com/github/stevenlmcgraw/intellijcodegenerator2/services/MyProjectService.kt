package com.github.stevenlmcgraw.intellijcodegenerator2.services

import com.intellij.openapi.project.Project
import com.github.stevenlmcgraw.intellijcodegenerator2.CodeGenerator2Bundle

class MyProjectService(project: Project) {

    init {
        println(CodeGenerator2Bundle.message("projectService", project.name))
    }
}
