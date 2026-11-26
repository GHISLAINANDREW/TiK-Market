package com.tik_market.utils

import androidx.compose.runtime.Composable
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(htmlContent, jobName) => {
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
        doc.write(htmlContent);
        doc.close();

        setTimeout(() => {
            iframe.contentWindow.focus();
            iframe.contentWindow.print();
            setTimeout(() => {
                try { document.body.removeChild(iframe); } catch(e){}
            }, 2000);
        }, 500);
    } catch(e) {
        console.error('[PdfPrinter]', e);
    }
}""")
private external fun jsPrintHtml(htmlContent: String, jobName: String)

@Composable
actual fun rememberPdfPrinter(): (htmlContent: String, jobName: String) -> Unit {
    return { htmlContent, jobName ->
        jsPrintHtml(htmlContent, jobName)
    }
}
