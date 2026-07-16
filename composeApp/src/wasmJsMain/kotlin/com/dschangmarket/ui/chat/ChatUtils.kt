package com.dschangmarket.ui.chat

actual fun playChatSound() {
    playChatSoundJs()
}

actual fun playAudio(url: String, onProgress: (Float) -> Unit, onCompletion: () -> Unit) {
    stopAudio() // Stop previous playback before starting new one
    playAudioJs(url, onProgress, onCompletion)
}

actual fun stopAudio() {
    stopAudioJs()
}

actual fun startVoiceRecording() {
    startVoiceRecordingJs()
}

actual fun stopVoiceRecording(onResult: (String?, Int) -> Unit) {
    stopVoiceRecordingJs(onResult)
}

actual fun pickImage(onResult: (String?) -> Unit) {
    pickImageJs(onResult)
}

actual fun takePhoto(onResult: (String?) -> Unit) {
    takePhotoJs(onResult)
}

actual fun pickFile(onResult: (String?) -> Unit) {
    pickFileJs(onResult)
}

@JsFun("() => { new Audio('https://assets.mixkit.co/active_storage/sfx/2358/2358-preview.mp3').play().catch(() => {}); }")
private external fun playChatSoundJs()

@JsFun("""(url, onProgress, onCompletion) => {
    try {
        if (!url || url === '') {
            onCompletion();
            return;
        }
        
        fetch(url, { headers: { 'bypass-tunnel-reminder': 'true' } })
            .then(response => {
                if (!response.ok) throw new Error('HTTP ' + response.status);
                return response.blob();
            })
            .then(blob => {
                const blobUrl = URL.createObjectURL(blob);
                const audio = new Audio(blobUrl);
                window.__currentAudio = audio;
                let ended = false;
                
                audio.ontimeupdate = () => {
                    if (ended) return;
                    if (audio.duration > 0) {
                        onProgress(audio.currentTime / audio.duration);
                    }
                };
                
                audio.onended = () => {
                    if (ended) return;
                    ended = true;
                    onProgress(1.0);
                    onCompletion();
                    URL.revokeObjectURL(blobUrl);
                    if (window.__currentAudio === audio) window.__currentAudio = null;
                };
                
                audio.onerror = () => {
                    if (ended) return;
                    ended = true;
                    onCompletion();
                    if (window.__currentAudio === audio) window.__currentAudio = null;
                };
                
                audio.play().catch(e => {
                    if (ended) return;
                    ended = true;
                    onCompletion();
                    if (window.__currentAudio === audio) window.__currentAudio = null;
                });
            })
            .catch(e => {
                console.error('Audio fetch error:', e);
                onCompletion();
            });
    } catch (e) {
        console.error('Audio creation error:', e);
        onCompletion();
    }
}""")
private external fun playAudioJs(url: String, onProgress: (Float) -> Unit, onCompletion: () -> Unit)

@JsFun("""() => {
    if (window.__currentAudio) {
        try {
            window.__currentAudio.pause();
            window.__currentAudio.src = '';
        } catch(e) {}
        window.__currentAudio = null;
    }
}""")
private external fun stopAudioJs()

@JsFun("""() => {
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
            window.mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' });
            window.audioChunks = [];
            window.mediaRecorder.ondataavailable = e => {
                if (e.data.size > 0) window.audioChunks.push(e.data);
            };
            window.mediaRecorder.start();
            window.startTime = Date.now();
        }).catch(e => {
            console.error('getUserMedia error:', e);
            navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
                window.mediaRecorder = new MediaRecorder(stream);
                window.audioChunks = [];
                window.mediaRecorder.ondataavailable = e => {
                    if (e.data.size > 0) window.audioChunks.push(e.data);
                };
                window.mediaRecorder.start();
                window.startTime = Date.now();
            }).catch(e2 => console.error('getUserMedia fallback error:', e2));
        });
    } else {
        console.error('mediaDevices not available');
    }
}""")
private external fun startVoiceRecordingJs()

@JsFun("""(callback) => {
    if (window.mediaRecorder && window.mediaRecorder.state !== 'inactive') {
        window.mediaRecorder.onstop = () => {
            const blob = new Blob(window.audioChunks, { type: window.mediaRecorder.mimeType || 'audio/webm' });
            const reader = new FileReader();
            reader.readAsDataURL(blob);
            reader.onloadend = () => {
                const dataUrl = reader.result;
                const duration = Math.round((Date.now() - window.startTime) / 1000);
                callback(dataUrl, duration);
                try {
                    window.mediaRecorder.stream.getTracks().forEach(track => track.stop());
                } catch(e) {}
                window.mediaRecorder = null;
                window.audioChunks = [];
            };
        };
        window.mediaRecorder.stop();
    } else {
        callback(null, 0);
    }
}""")
private external fun stopVoiceRecordingJs(callback: (String?, Int) -> Unit)

@JsFun("""(callback) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (ev) => callback(ev.target.result);
            reader.readAsDataURL(file);
        } else {
            callback(null);
        }
    };
    input.click();
}""")
private external fun pickImageJs(callback: (String?) -> Unit)

@JsFun("""(callback) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.capture = 'environment';
    input.onchange = (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (ev) => callback(ev.target.result);
            reader.readAsDataURL(file);
        } else {
            callback(null);
        }
    };
    input.click();
}""")
private external fun takePhotoJs(callback: (String?) -> Unit)

@JsFun("""(callback) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.multiple = false;
    let cancelled = true;
    const onFocus = () => {
        setTimeout(() => {
            if (cancelled) callback(null);
        }, 500);
    };
    input.onchange = (e) => {
        cancelled = false;
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (ev) => callback(ev.target.result);
            reader.onerror = () => callback(null);
            reader.readAsDataURL(file);
        } else {
            callback(null);
        }
    };
    window.addEventListener('focus', onFocus, { once: true });
    input.click();
}""")
private external fun pickFileJs(callback: (String?) -> Unit)

actual fun openUrl(url: String) {
    openUrlJs(url)
}

@JsFun("""(url) => {
    const a = document.createElement('a');
    a.href = url;
    a.target = '_blank';
    a.rel = 'noopener noreferrer';
    document.body.appendChild(a);
    a.click();
    setTimeout(() => document.body.removeChild(a), 100);
}""")
private external fun openUrlJs(url: String)
