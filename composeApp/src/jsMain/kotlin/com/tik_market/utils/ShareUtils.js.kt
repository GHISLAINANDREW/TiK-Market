package com.tik_market.utils

import kotlinx.browser.window

actual fun shareText(text: String, title: String) {
    val nav = window.navigator.asDynamic()
    if (nav.share != null) {
        nav.share(js("{ title: title, text: text }")).catch { }
    } else {
        window.navigator.clipboard.writeText(text).then({
            window.alert("Lien copié dans le presse-papier !")
        }, {
            // handle error
        })
    }
}
