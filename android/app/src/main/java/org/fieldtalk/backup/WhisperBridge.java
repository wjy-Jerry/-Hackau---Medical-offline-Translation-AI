package org.fieldtalk.backup;

/** JNI boundary for pinned whisper.cpp 1.8.0. No network or platform recognition service. */
final class WhisperBridge {
    static { System.loadLibrary("fieldtalk_asr"); }
    private WhisperBridge() { }
    static native long nativeLoad(String modelPath);
    static native String nativeTranscribe(long context, float[] samples, String language);
    static native void nativeClose(long context);
}
