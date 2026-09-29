#!/usr/bin/env python3
"""Fast, network-free source contract checks for the JARVIS release."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

wake = read("app/src/main/kotlin/com/jarvis/service/WakeWordDetector.kt")
prov = read("app/src/main/kotlin/com/jarvis/models/WakeWordAssetProvisioner.kt")
llm = read("app/src/main/kotlin/com/jarvis/brain/LocalLlmEngine.kt")
llama = read("app/src/main/kotlin/com/jarvis/brain/LlamaJNI.kt")
whisper = read("app/src/main/kotlin/com/jarvis/brain/WhisperJNI.kt")
cpp_llama = read("app/src/main/cpp/llama_jni.cpp")
cpp_whisper = read("app/src/main/cpp/whisper_jni.cpp")
bridge = read("app/src/main/kotlin/com/jarvis/brain/ToolBridgeKotlin.kt")
dispatcher = read("app/src/main/kotlin/com/jarvis/tools/ToolDispatcher.kt")
cmake = read("app/src/main/cpp/CMakeLists.txt")

assert 'Regex("\\\\s+")' in prov
assert 'melspectrogram.onnx' in wake and 'embedding_model.onnx' in wake
assert 'hey_jarvis_v0.1.onnx' in wake
assert 'models/wakeword/' in prov
assert 'System.loadLibrary("jarvis_native")' in llama
assert 'nativeGenerate(ctx: Long, prompt: String, maxTokens: Int, temperature: Float)' in llama
assert 'nativeCancel(ctx: Long)' in llama
assert 'java/com/jarvis' not in cmake
assert 'Java_com_jarvis_brain_LlamaJNI_nativeGenerate' in cpp_llama
assert 'jfloat temp' in cpp_llama
assert 'Java_com_jarvis_brain_WhisperJNI_nativeTranscribe' in cpp_whisper
assert 'const float' not in cpp_whisper  # JNI must convert PCM16 to float before whisper_full.
assert 'std::vector<float> pcm' in cpp_whisper
assert 'nativeTranscribe(handle: Long, pcm: ByteArray, language: String)' in whisper
assert llm.count('val entry = models.all().firstOrNull') == 1
assert llm.count('ModelChecksum.verify(path, entry.sha256)') == 1
assert 'authorizationMutex' in dispatcher
assert 'validateArgs' in dispatcher
assert 'elementToAny' in bridge
print("source contracts: PASS")

if __name__ == "__main__":
    pass
