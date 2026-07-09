package me.padi.qqlite.revived.shared.model.module

data class MainUiState(
    val binderText: String = "Loading",
    val apiText: String = "",
    val frameworkText: String = "",
    val frameworkVersionText: String = "",
    val frameworkVersionCodeText: String = "",
    val frameworkPropertiesText: String = "",
    val scopeText: String = "",
    val processText: String = "",
    val reloadEnabled: Boolean = false
)
