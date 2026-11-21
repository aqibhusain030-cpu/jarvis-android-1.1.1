#!/usr/bin/env python3
"""Regression tests for the Python-side multilingual language router."""
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "python"))
from language import detect_language, style_for


class MultilingualLanguageTests(unittest.TestCase):
    def test_hindi(self):
        self.assertEqual(detect_language("मुझे पाँच मिनट का टाइमर लगाना है"), "hi")

    def test_english(self):
        self.assertEqual(detect_language("Set a timer for five minutes"), "en")

    def test_hinglish(self):
        self.assertEqual(detect_language("मेरा फोन silent कर do"), "hinglish")

    def test_arabic(self):
        self.assertEqual(detect_language("اضبط مؤقتاً لمدة خمس دقائق"), "ar")

    def test_arabic_english_code_switch(self):
        self.assertEqual(detect_language("افتح YouTube من فضلك"), "ar")

    def test_arabic_style(self):
        self.assertIn("Arabic", style_for("ar"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
