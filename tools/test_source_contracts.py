#!/usr/bin/env python3
"""Fast, network-free source contract checks for the JARVIS release."""
from pathlib import Path

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
memory = read("app/src/main/kotlin/com/jarvis/memory/MemoryStore.kt")
dao = read("app/src/main/kotlin/com/jarvis/memory/MemoryDao.kt")
entity = read("app/src/main/kotlin/com/jarvis/memory/MemoryEntity.kt")
activity_test = read("app/src/androidTest/kotlin/com/jarvis/WakeWordInstrumentedTest.kt")

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
assert 'const float' not in cpp_whisper
assert 'std::vector<float> pcm' in cpp_whisper
assert 'nativeTranscribe(handle: Long, pcm: ByteArray, language: String)' in whisper
assert 'models.assetForFilename("llm", modelName)' in llm
assert 'models.verify(asset)' in llm
assert 'class HybridLlmEngine' in read("app/src/main/kotlin/com/jarvis/brain/HybridLlmEngine.kt")
assert 'class RemoteLlmEngine' in read("app/src/main/kotlin/com/jarvis/brain/RemoteLlmEngine.kt")
assert 'authorizationMutex' in dispatcher
assert 'validateArgs' in dispatcher
assert 'elementToAny' in bridge
assert 'toJsonElement' in bridge
assert 'result.data.toJsonElement()' in bridge

assert 'version = 3' in memory
assert 'MIGRATION_1_2' in memory
assert 'MIGRATION_2_3' in memory
assert 'LearningLessonEntity::class' in memory
assert 'recordMistake' in memory
assert 'recordLessonSuccess' in memory
assert 'resumableGoals()' in memory
assert 'allGoals()' in memory
assert 'maxRetries' in entity and 'maxRuntimeSeconds' in entity
assert "status IN ('RUNNING', 'PENDING')" in dao
assert 'ActivityScenario.launch(MainActivity::class.java)' in activity_test
assert 'ConfigSchemaValidator.validate(raw)' in read("app/src/main/kotlin/com/jarvis/config/Defaults.kt")
assert 'schema_version' in read("app/src/main/assets/config.json")
assert 'response_too_large' in read("app/src/main/kotlin/com/jarvis/tools/impl/HttpTool.kt")
assert '64 * 1024L' in read("app/src/main/kotlin/com/jarvis/tools/impl/HttpTool.kt")
assert 'fun cosine(a: FloatArray, b: FloatArray)' in read("app/src/main/kotlin/com/jarvis/memory/MemorySearch.kt")
assert 'fun lexical(query: String, value: String)' in read("app/src/main/kotlin/com/jarvis/memory/MemorySearch.kt")
assert 'No unverified embedding or vision checkpoint added' in read("docs/APPLE_SYSTEMS_ADOPTION_AUDIT.md")
assert 'lastFailureByTool' in dispatcher
assert 'recordLessonSuccess' in dispatcher
assert 'learning_lessons' in dao
assert 'currentCoroutineContext().isActive' in wake
assert 'audio.isRunning()' in wake
assert 'Local STT unavailable; transcript dropped' in read("app/src/main/kotlin/com/jarvis/voice/SpeechRecognizerBridge.kt")
assert '\\${' not in memory
assert '\\${' not in read("app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt")

assert 'class CapabilityRegistry' in read("app/src/main/kotlin/com/jarvis/core/capability/CapabilityRegistry.kt")
assert 'class CapabilityExecutor' in read("app/src/main/kotlin/com/jarvis/core/capability/CapabilityExecutor.kt")
assert 'confirmationGranted' in read("app/src/main/kotlin/com/jarvis/core/capability/CapabilityExecutor.kt")
assert 'ToolCapabilityBackend' in read("app/src/main/kotlin/com/jarvis/di/JarvisModule.kt")
assert 'llama_memory_clear(llama_get_memory(h->ctx), true);' in cpp_llama
assert 'get_news' in read("app/src/main/kotlin/com/jarvis/tools/ToolDispatcher.kt")
assert 'api.gdeltproject.org' in read("app/src/main/kotlin/com/jarvis/security/SecurityPolicy.kt")
assert '<queries>' in read("app/src/main/AndroidManifest.xml")
assert 'canRetrieveWindowContent="true"' in read("app/src/main/res/xml/jarvis_accessibility_service.xml")
assert 'typeWindowContentChanged' in read("app/src/main/res/xml/jarvis_accessibility_service.xml")
assert 'class AppSchemaRegistry' in read("app/src/main/kotlin/com/jarvis/app/AppSchemaRegistry.kt")
assert 'name = "inspect_app_ui"' in read("app/src/main/kotlin/com/jarvis/tools/impl/AppSchemaTool.kt")
assert 'name = "execute_app_task"' in read("app/src/main/kotlin/com/jarvis/tools/impl/AppTaskTool.kt")
assert 'active_app_changed' in read("app/src/main/kotlin/com/jarvis/tools/impl/AppTaskTool.kt")
assert 'register(appSchema); register(appTask)' in read("app/src/main/kotlin/com/jarvis/di/JarvisModule.kt")
assert '<category android:name="android.intent.category.LAUNCHER"/>' in read("app/src/main/AndroidManifest.xml")
assert 'latest news' in read("app/src/main/kotlin/com/jarvis/voice/VoiceCommandRouter.kt").lower()

print("source contracts: PASS")

if __name__ == "__main__":
    pass

assert 'app/src/main/assets/models/manifest.v1.json' in __import__("pathlib").Path(".").joinpath("app/src/main/assets/models/manifest.v1.json").as_posix()
model_manager = read("app/src/main/kotlin/com/jarvis/models/ModelManager.kt")
model_manifest = read("app/src/main/kotlin/com/jarvis/models/ModelManifest.kt")
model_downloader = read("app/src/main/kotlin/com/jarvis/models/ModelDownloader.kt")
local_llm = read("app/src/main/kotlin/com/jarvis/brain/LocalLlmEngine.kt")
stt_bridge = read("app/src/main/kotlin/com/jarvis/voice/SpeechRecognizerBridge.kt")
assert 'models/manifest.v1.json' in model_manager
assert 'ModelManifestParser.parse' in model_manager
assert 'ModelManifestValidator.validate' in model_manifest
assert 'asset.downloadUrl.contains("/resolve/" + asset.revision + "/" + asset.filename)' in model_manifest
assert 'read == asset.sizeBytes' in model_downloader
assert 'models.verify(asset)' in local_llm
assert 'models.verify(asset)' in stt_bridge

assert 'remoteInferenceEnabled' in read("app/src/main/kotlin/com/jarvis/config/JarvisConfig.kt")
assert 'remote_enabled' in read("app/src/main/assets/config.json")
assert 'Json.encodeToJsonElement(tool.spec.parameters)' in read("app/src/main/kotlin/com/jarvis/brain/BrainBootstrap.kt")
assert 'LESSONS FROM PREVIOUS EXECUTION' in read("app/src/main/python/brain.py")
assert '_MAX_TOOL_ROUNDS = 8' in read("app/src/main/python/brain.py")
assert 'target_total_parameters' in read("model/2t/config.json")
assert 'exact parameter count' in read("model/2t/training/README.md")

remote_engine = read("app/src/main/kotlin/com/jarvis/brain/RemoteLlmEngine.kt")
credentials = read("app/src/main/kotlin/com/jarvis/brain/RemoteInferenceCredentialStore.kt")
assert 'https://' in remote_engine
assert 'chat/completions' in remote_engine
assert 'Authorization' in remote_engine
assert 'AndroidKeyStore' in credentials
assert 'AES/GCM/NoPadding' in credentials
assert 'Bearer ' in remote_engine
