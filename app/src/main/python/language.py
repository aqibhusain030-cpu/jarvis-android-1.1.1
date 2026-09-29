"""Language detection for Hindi, English, and code-switched Hinglish."""
import re

_DEV = re.compile(r"[\u0900-\u097F]")
_LAT = re.compile(r"[A-Za-z]")


def detect_language(text: str) -> str:
    if not text.strip():
        return "hi"
    dev = len(_DEV.findall(text)); lat = len(_LAT.findall(text))
    total = dev + lat or 1
    ratio = dev / total
    if dev == 0 and lat > 0:
        return "en"
    if ratio > 0.85:
        return "hi"
    if 0.10 <= ratio <= 0.60 and lat > 0:
        return "hinglish"
    return "hi" if ratio > 0.5 else "en"


def style_for(lang: str) -> str:
    return {
        "hi": "respectful Hindi, address the user as 'सर', use 'जी' and 'कृपया'",
        "en": "polite English, address the user as 'Sir'",
        "hinglish": "natural Hinglish, mixing Hindi grammar with English technical terms",
    }[lang]
