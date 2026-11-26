package com.tik_market.utils

import androidx.compose.runtime.Composable
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(filename, content) => {
    try {
        const iframe = document.createElement('iframe');
        iframe.style.position = 'fixed';
        iframe.style.right = '0';
        iframe.style.bottom = '0';
        iframe.style.width = '0';
        iframe.style.height = '0';
        iframe.style.border = '0';
        document.body.appendChild(iframe);

        const doc = iframe.contentWindow.document;
        doc.open();
        doc.write(content);
        doc.close();

        setTimeout(() => {
            iframe.contentWindow.focus();
            iframe.contentWindow.print();
            setTimeout(() => {
                try { document.body.removeChild(iframe); } catch(e){}
            }, 2000);
        }, 500);
        return true;
    } catch(e) {
        console.error('[FileExporter]', e);
        return false;
    }
}""")
private external fun jsExportFile(filename: String, content: String): Boolean

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    return { filename, content ->
        jsExportFile(filename, content)
    }
}
