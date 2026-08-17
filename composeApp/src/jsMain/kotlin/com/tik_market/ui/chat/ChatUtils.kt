package com.tik_market.ui.chat

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import org.w3c.files.get

actual fun playChatSound() {
    window.asDynamic().eval("new Audio('https://assets.mixkit.co/active_storage/sfx/2358/2358-preview.mp3').play().catch(function(){});")
}

actual fun playAudio(url: String, onProgress: (Float) -> Unit, onCompletion: () -> Unit) {
    stopAudio()
    if (url.isBlank()) {
        onCompletion()
        return
    }

    val playFunc = window.asDynamic().playAudioJs
    if (playFunc != null) {
        playFunc(url, onProgress, onCompletion)
    } else {
        window.fetch(url, js("{ headers: { 'bypass-tunnel-reminder': 'true' } }")).then { response ->
            if (!response.ok) throw Exception("HTTP ${response.status}")
            response.blob()
        }.then { blob ->
            val blobUrl = window.asDynamic().URL.createObjectURL(blob) as String
            val audio = window.asDynamic().Audio(blobUrl)
            window.asDynamic().__currentAudio = audio
            
            audio.ontimeupdate = {
                if (audio.duration > 0) onProgress((audio.currentTime as Double / audio.duration as Double).toFloat())
            }
            
            audio.onended = {
                onProgress(1.0f)
                onCompletion()
                window.asDynamic().URL.revokeObjectURL(blobUrl)
            }
            
            audio.onerror = { onCompletion() }
            audio.play().catch { onCompletion() }
        }.catch { onCompletion() }
    }
}

actual fun stopAudio() {
    val audio = window.asDynamic().__currentAudio
    if (audio != null) {
        try {
            audio.pause()
            audio.src = ""
        } catch (e: Exception) {}
        window.asDynamic().__currentAudio = null
    }
}

actual fun startVoiceRecording() {
    val startFunc = window.asDynamic().startVoiceRecordingJs
    if (startFunc != null) {
        startFunc()
    }
}

actual fun stopVoiceRecording(onResult: (String?, Int) -> Unit) {
    val stopFunc = window.asDynamic().stopVoiceRecordingJs
    if (stopFunc != null) {
        stopFunc(onResult)
    } else {
        onResult(null, 0)
    }
}

actual fun pickImage(onResult: (String?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = "image/*"
    input.onchange = {
        val file = input.files?.get(0)
        if (file != null) {
            val reader = FileReader()
            reader.onload = {
                onResult(reader.result.toString())
            }
            reader.readAsDataURL(file)
        } else {
            onResult(null)
        }
    }
    input.click()
}

actual fun takePhoto(onResult: (String?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = "image/*"
    input.setAttribute("capture", "environment")
    input.onchange = {
        val file = input.files?.get(0)
        if (file != null) {
            val reader = FileReader()
            reader.onload = {
                onResult(reader.result.toString())
            }
            reader.readAsDataURL(file)
        } else {
            onResult(null)
        }
    }
    input.click()
}

actual fun pickFile(onResult: (String?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.multiple = false
    var cancelled = true
    
    val onFocus = { _: Event ->
        window.setTimeout({
            if (cancelled) onResult(null)
        }, 500)
    }
    
    input.onchange = {
        cancelled = false
        val file = input.files?.get(0)
        if (file != null) {
            val reader = FileReader()
            reader.onload = {
                onResult(reader.result.toString())
            }
            reader.onerror = {
                onResult(null)
            }
            reader.readAsDataURL(file)
        } else {
            onResult(null)
        }
    }
    
    window.addEventListener("focus", onFocus.unsafeCast<(Event) -> Unit>())
    input.click()
}

actual fun openUrl(url: String) {
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.target = "_blank"
    a.rel = "noopener noreferrer"
    document.body?.appendChild(a)
    a.click()
    window.setTimeout({ document.body?.removeChild(a) }, 100)
}
