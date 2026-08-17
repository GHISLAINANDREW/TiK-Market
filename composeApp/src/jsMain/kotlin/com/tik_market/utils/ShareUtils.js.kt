package com.tik_market.utils

actual fun shareText(text: String, title: String) {
    js("""
        if (navigator.share) {
            navigator.share({ title: title, text: text }).catch(function(e) {});
        } else {
            navigator.clipboard.writeText(text).then(function() {
                alert('Lien copié dans le presse-papier !');
            }).catch(function(e) {});
        }
    """)
}
