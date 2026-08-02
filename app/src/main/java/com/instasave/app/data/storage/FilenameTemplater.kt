package com.instasave.app.data.storage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilenameTemplater @Inject constructor() {
    fun format(
        template: String,
        author: String,
        shortcode: String,
        index: Int,
        extension: String
    ): String {
        var name = template
            .replace("{author}", author)
            .replace("{shortcode}", shortcode)
            .replace("{index}", (index + 1).toString())

        // Sanitize illegal filesystem characters
        name = name.replace(Regex("""[\\/:*?"<>|]"""), "_")
        if (name.isEmpty()) name = "${author}_${shortcode}_${index + 1}"

        return "$name.$extension"
    }
}
