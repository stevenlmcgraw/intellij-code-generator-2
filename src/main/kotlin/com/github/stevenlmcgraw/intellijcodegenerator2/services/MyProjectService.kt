package com.github.stevenlmcgraw.intellijcodegenerator2.services

import com.intellij.openapi.project.Project
import com.github.stevenlmcgraw.intellijcodegenerator2.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
