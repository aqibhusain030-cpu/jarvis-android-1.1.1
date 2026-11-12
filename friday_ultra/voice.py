"""Voice I/O Subsystem: Neural British TTS (edge-tts), Offline TTS (pyttsx3), and Local Whisper STT."""
from __future__ import annotations

import asyncio
import platform
import re
import shutil
import subprocess
import tempfile
import threading
import wave
from pathlib import Path
from typing import Any

from friday_ultra.config import STATE, TTS_VOICE, WHISPER_MODEL, get_data_dir

try:
    import edge_tts  # type: ignore

    EDGE_TTS_AVAILABLE = True
except ImportError:
    edge_tts = None  # type: ignore
    EDGE_TTS_AVAILABLE = False

try:
    import pyttsx3  # type: ignore

    PYTTSX3_AVAILABLE = True
except ImportError:
    pyttsx3 = None  # type: ignore
    PYTTSX3_AVAILABLE = False

try:
    import numpy as np  # type: ignore
    import sounddevice as sd  # type: ignore

    AUDIO_CAPTURE_AVAILABLE = True
except ImportError:
    np = None  # type: ignore
    sd = None  # type: ignore
    AUDIO_CAPTURE_AVAILABLE = False

try:
    import whisper  # type: ignore

    WHISPER_AVAILABLE = True
except ImportError:
    whisper = None  # type: ignore
    WHISPER_AVAILABLE = False


def clean_text_for_speech(text: str) -> str:
    """Strip Markdown formatting, code blocks, and raw URLs so spoken output sounds natural."""
    cleaned = re.sub(r"```[\s\S]*?```", " Code block omitted. ", text)
    cleaned = re.sub(r"`([^`]+)`", r"\1", cleaned)
    cleaned = re.sub(r"https?://\S+", "", cleaned)
    cleaned = re.sub(r"\[([^\]]+)\]\([^\)]+\)", r"\1", cleaned)
    cleaned = re.sub(r"[*_#~>|]+", " ", cleaned)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned


def _play_audio_file(file_path: Path) -> bool:
    """Play an audio file using available cross-platform audio players or torchaudio+sounddevice."""
    if not file_path.is_file():
        return False

    system_name = platform.system().lower()
    if system_name == "darwin" and shutil.which("afplay"):
        res = subprocess.run(["afplay", str(file_path)], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return res.returncode == 0

    for cmd in (
        ["ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet", str(file_path)],
        ["mpg123", "-q", str(file_path)],
        ["mpv", "--no-video", "--really-quiet", str(file_path)],
        ["aplay", "-q", str(file_path)],
    ):
        if shutil.which(cmd[0]):
            res = subprocess.run(cmd, check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if res.returncode == 0:
                return True

    if AUDIO_CAPTURE_AVAILABLE and sd is not None:
        try:
            import torchaudio  # type: ignore

            waveform, sr = torchaudio.load(str(file_path))
            audio_np = waveform.numpy().T
            sd.play(audio_np, sr)
            sd.wait()
            return True
        except Exception:
            pass
    return False


class VoiceEngine:
    """Thread-safe speech synthesis (edge-tts + pyttsx3) and Whisper speech recognition engine."""

    def __init__(self, tts_voice: str = TTS_VOICE, whisper_model_name: str = WHISPER_MODEL) -> None:
        self.tts_voice = tts_voice
        self.whisper_model_name = whisper_model_name
        self._tts_lock = threading.Lock()
        self._stt_lock = threading.Lock()
        self._whisper_model: Any = None

    def _load_whisper(self) -> Any:
        if not WHISPER_AVAILABLE or whisper is None:
            raise RuntimeError("openai-whisper is not installed. Run: pip install openai-whisper")
        with self._stt_lock:
            if self._whisper_model is None:
                self._whisper_model = whisper.load_model(self.whisper_model_name)
            return self._whisper_model

    def synthesize_to_file(self, text: str, output_path: Path | None = None) -> Path | None:
        """Generate neural speech audio file via edge-tts (useful for both local playback and Gradio UI)."""
        spoken = clean_text_for_speech(text)
        if not spoken or not EDGE_TTS_AVAILABLE or edge_tts is None:
            return None

        target = output_path or (get_data_dir() / "tts_response.mp3")
        target.parent.mkdir(parents=True, exist_ok=True)

        async def _generate() -> None:
            communicator = edge_tts.Communicate(text=spoken[:2500], voice=self.tts_voice)
            await communicator.save(str(target))

        try:
            try:
                loop = asyncio.get_running_loop()
            except RuntimeError:
                loop = None

            if loop and loop.is_running():
                done_err: list[Exception] = []

                def _runner() -> None:
                    try:
                        asyncio.run(_generate())
                    except Exception as exc:
                        done_err.append(exc)

                t = threading.Thread(target=_runner, daemon=True)
                t.start()
                t.join(timeout=25)
                if done_err or not target.is_file():
                    return None
            else:
                asyncio.run(_generate())
            return target if target.is_file() else None
        except Exception:
            return None

    def speak(self, text: str, force: bool = False) -> bool:
        """Speak text aloud using edge-tts primary voice and pyttsx3 offline fallback."""
        if not force and not STATE.voice_enabled:
            return False

        spoken = clean_text_for_speech(text)
        if not spoken:
            return False

        with self._tts_lock:
            mp3_file = self.synthesize_to_file(spoken)
            if mp3_file is not None and _play_audio_file(mp3_file):
                return True

            if PYTTSX3_AVAILABLE and pyttsx3 is not None:
                try:
                    engine = pyttsx3.init()
                    engine.setProperty("rate", 185)
                    engine.say(spoken[:2500])
                    engine.runAndWait()
                    return True
                except Exception:
                    return False
        return False

    def transcribe_file(self, audio_path: str | Path) -> str:
        """Transcribe a recorded WAV/MP3/OGG file locally using OpenAI Whisper."""
        path = Path(audio_path)
        if not path.is_file():
            return f"[STT Error] Audio file not found: {path}"
        try:
            model = self._load_whisper()
            result = model.transcribe(str(path), fp16=False)
            return str(result.get("text", "")).strip()
        except Exception as exc:
            return f"[STT Error] {exc}"

    def listen(self, duration: float = 5.0, sample_rate: int = 16000) -> str:
        """Capture microphone audio via sounddevice and transcribe locally with Whisper."""
        if not AUDIO_CAPTURE_AVAILABLE or sd is None or np is None:
            return "[STT Error] sounddevice/numpy not available for microphone capture."
        if not WHISPER_AVAILABLE:
            return "[STT Error] openai-whisper is not installed."

        frames = int(max(1.0, duration) * sample_rate)
        try:
            recording = sd.rec(frames, samplerate=sample_rate, channels=1, dtype="int16")
            sd.wait()
        except Exception as exc:
            return f"[STT Error] Microphone capture failed: {exc}"

        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
            wav_path = Path(tmp.name)

        try:
            with wave.open(str(wav_path), "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)  # 16-bit PCM
                wf.setframerate(sample_rate)
                wf.writeframes(recording.tobytes())
            return self.transcribe_file(wav_path)
        finally:
            try:
                wav_path.unlink(missing_ok=True)
            except OSError:
                pass


VOICE = VoiceEngine()


def speak(text: str, force: bool = False) -> bool:
    """Module-level convenience function to speak text using FRIDAY's VoiceEngine."""
    return VOICE.speak(text=text, force=force)


def listen(duration: float = 5.0, sample_rate: int = 16000) -> str:
    """Module-level convenience function to record from mic and transcribe with Whisper."""
    return VOICE.listen(duration=duration, sample_rate=sample_rate)


def transcribe_audio_file(audio_path: str | Path) -> str:
    """Module-level convenience function to transcribe an existing audio file."""
    return VOICE.transcribe_file(audio_path=audio_path)
