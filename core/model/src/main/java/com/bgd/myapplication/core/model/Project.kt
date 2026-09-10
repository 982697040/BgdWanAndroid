package com.bgd.myapplication.core.model

data class ProjectCategory(val id: Int, val name: String)
data class Project(val id: Int, val title: String, val imageUrl: String, val url: String)
data class ProjectPage(val projects: List<Project>, val endReached: Boolean)
