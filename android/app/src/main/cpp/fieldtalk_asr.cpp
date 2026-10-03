#include <jni.h>
#include <string>
#include "whisper.h"

extern "C" JNIEXPORT jlong JNICALL
Java_org_fieldtalk_backup_WhisperBridge_nativeLoad(JNIEnv *env, jclass, jstring path) {
    if (!path) return 0;
    const char *file = env->GetStringUTFChars(path, nullptr);
    if (!file) return 0;
    whisper_context_params options = whisper_context_default_params();
    options.use_gpu = false;
    whisper_context *context = whisper_init_from_file_with_params(file, options);
    env->ReleaseStringUTFChars(path, file);
    return reinterpret_cast<jlong>(context);
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_fieldtalk_backup_WhisperBridge_nativeTranscribe(
        JNIEnv *env, jclass, jlong handle, jfloatArray audio, jstring language) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (!context || !audio || !language) return nullptr;
    const jsize count = env->GetArrayLength(audio);
    if (count < 16000 || count > 16 * 16000) return nullptr;
    const char *lang = env->GetStringUTFChars(language, nullptr);
    if (!lang) return nullptr;
    const std::string selected(lang);
    env->ReleaseStringUTFChars(language, lang);
    if (selected != "en" && selected != "zh" && selected != "ru") return nullptr;
    jfloat *samples = env->GetFloatArrayElements(audio, nullptr);
    if (!samples) return nullptr;
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = selected.c_str();
    params.translate = false;
    params.no_context = true;
    params.no_timestamps = true;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.n_threads = 4;
    const int result = whisper_full(context, params, samples, count);
    env->ReleaseFloatArrayElements(audio, samples, JNI_ABORT);
    if (result != 0) return nullptr;
    std::string text;
    const int segments = whisper_full_n_segments(context);
    for (int i = 0; i < segments; ++i) {
        const char *part = whisper_full_get_segment_text(context, i);
        if (part) text += part;
    }
    return env->NewStringUTF(text.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_org_fieldtalk_backup_WhisperBridge_nativeClose(JNIEnv *, jclass, jlong handle) {
    if (handle) whisper_free(reinterpret_cast<whisper_context *>(handle));
}
