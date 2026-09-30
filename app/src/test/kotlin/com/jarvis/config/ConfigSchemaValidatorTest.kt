package com.jarvis.config

import org.junit.Test

class ConfigSchemaValidatorTest {
    @Test
    fun bundledConfigAcceptsRemoteInferenceFields() {
        val raw = """
            {
              "schema_version":1,
              "agent":{"name":"JARVIS","user_name":"Sir","default_language":"en","auto_language_detect":true,"address_style":"respectful","max_tool_rounds":8,"max_context_tokens":4096,"max_output_tokens":700,"temperature":0.2},
              "voice":{"wake_words":["hey jarvis"],"wake_threshold":0.5,"tts_voice_hi":"hi-IN-default","tts_voice_en":"en-IN-default","tts_speed":1.0,"tts_pitch":1.0,"barge_in":true,"vad_silence_ms":800,"max_command_seconds":20},
              "llm":{"provider":"local","model":"qwen.gguf","context_size":4096,"threads":4,"gpu_layers":0,"remote_enabled":true,"remote_base_url":"https://jarvis.example/v1","remote_model":"JARVIS-2T-MoE","remote_timeout_ms":60000},
              "stt":{"provider":"local","model":"ggml-small.bin","language":"auto","threads":4},
              "tts":{"provider":"android","fallback":"piper"},
              "autonomy":{"enabled":true,"battery_poll_seconds":600,"notification_poll_seconds":300,"morning_briefing_hour":7,"morning_briefing_minute":0,"nightly_summary_hour":21,"max_active_goals":10,"max_tool_calls_per_goal":200},
              "safety":{"communication_require_confirmation":true,"file_delete_requires_confirmation":true,"http_post_requires_confirmation":true,"uninstall_requires_confirmation":true,"shell_tool_enabled":false,"notification_cloud_ok":false,"cloud_fallback_enabled":false,"pin_required_for_high_risk":true,"kill_phrase_enabled":true},
              "rate_limits":{"sms_per_hour":20,"calls_per_hour":20,"camera_per_hour":60,"location_per_hour":120,"http_per_hour":100},
              "paths":{"models_dir":"models","audio_dir":"audio","logs_dir":"logs","audit_dir":"audit","exports_dir":"exports"},
              "cloud":{"provider":"none"}
            }
        """.trimIndent()

        ConfigSchemaValidator.validate(raw)
    }
}
