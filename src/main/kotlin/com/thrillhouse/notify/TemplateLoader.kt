package com.thrillhouse.notify

import java.io.File

/**
 * Loads notification body templates from the templates directory.
 *
 * Templates are cached in memory after the first load, so repeated sends
 * for the same template name do not touch the filesystem again.
 */
class TemplateLoader(private val templatesDir: File) {

    fun load(templateName: String): String {
        val file = File(templatesDir, "$templateName.txt")
        if (!file.exists()) {
            throw IllegalArgumentException("Unknown template: $templateName")
        }
        return file.readText()
    }
}
