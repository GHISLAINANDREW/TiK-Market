package com.tik_market.utils

actual fun shareText(text: String, title: String) {
    shareTextJs(text, title)
}

@JsFun("""(text, title) => {
    if (navigator.share) {
        navigator.share({ title: title, text: text })
            .catch(() => {});
    } else {
        // Fallback: copy to clipboard
        navigator.clipboard.writeText(text).catch(() => {});
        alert('Lien copié dans le presse-papier !');
    }
}""")
private external fun shareTextJs(text: String, title: String)
