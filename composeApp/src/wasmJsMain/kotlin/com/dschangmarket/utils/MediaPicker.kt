package com.dschangmarket.utils

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

actual object MediaPicker {
    actual fun pickImageOrVideo(onResult: (String?, String?, Boolean) -> Unit) {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/*,video/*"
        
        input.onchange = {
            val file = input.files?.get(0)
            if (file != null) {
                val reader = FileReader()
                reader.onload = {
                    val result = reader.result
                    // result is a Data URL string in JS when using readAsDataURL
                    onResult(result.toString(), file.name, file.type.startsWith("video"))
                }
                reader.readAsDataURL(file)
            }
        }
        input.click()
    }
}
