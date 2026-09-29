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
assert llm.count('val entry = models.all().firstOrNull') == 1
assert llm.count('ModelChecksum.verify(path, entry.sha256)') == 1
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
assert 'lastFailureByInvocation' in dispatcher
assert 'recordLessonSuccess' in dispatcher
assert 'learning_lessons' in dao
wake_detector = read("app/src/main/kotlin/com/jarvis/service/WakeWordDetector.kt")
assert "enum class WakeRecognitionState" in wake_detector
assert "WakeTriggerEvent" in wake_detector
assert "WakeRecognitionState.LISTENING" in wake_detector
assert "WakeRecognitionState.TRIGGERED" in wake_detector
assert "WakeRecognitionState.ERROR" in wake_detector
assert "SystemClock.elapsedRealtime()" in wake_detector

assert 'lockPlan' in dispatcher
assert 'confirmPlan' in dispatcher
assert 'invokeLockedPlanAction' in dispatcher
command_engine = read("app/src/main/kotlin/com/jarvis/command/UniversalCommandEngine.kt")
action_plan = read("app/src/main/kotlin/com/jarvis/command/ActionPlan.kt")
cap_registry = read("app/src/main/kotlin/com/jarvis/capability/CapabilityRegistry.kt")
cap_model = read("app/src/main/kotlin/com/jarvis/capability/Capability.kt")
cap_backends = read("app/src/main/kotlin/com/jarvis/capability/DefaultCapabilityBackends.kt")
python_brain = read("app/src/main/python/brain.py")
brain_bridge = read("app/src/main/kotlin/com/jarvis/brain/BrainBridge.kt")
service = read("app/src/main/kotlin/com/jarvis/service/JarvisForegroundService.kt")
assert 'UniversalCommandEngine' in command_engine
assert 'ToolArgsCodec.canonical' in action_plan
assert 'planCommand' in brain_bridge
assert 'FutureAppFunctionsBackend' in cap_backends
assert 'requiresPlatformIntegration' in cap_model
assert 'JarvisAccessibilityService' in read("app/src/main/kotlin/com/jarvis/accessibility/JarvisAccessibilityService.kt")
assert 'accessibility_action' in read("app/src/main/kotlin/com/jarvis/tools/impl/AccessibilityTool.kt")
assert 'BIND_ACCESSIBILITY_SERVICE' in read("app/src/main/AndroidManifest.xml")
assert 'UniversalCommandEngine' in service
assert '_run_tool' not in python_brain
assert '_tool_bridge' not in python_brain

assert 'currentCoroutineContext().isActive' in wake
assert 'audio.isRunning()' in wake
assert 'Local STT unavailable; transcript dropped' in read("app/src/main/kotlin/com/jarvis/voice/SpeechRecognizerBridge.kt")
assert '\\${' not in memory
assert '\\${' not in read("app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt")

print("source contracts: PASS")

if __name__ == "__main__":
    pass
