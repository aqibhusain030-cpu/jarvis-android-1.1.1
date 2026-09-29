JARVIS Android 1.1.1 - CI compile repair overlay

Observed failing CI run: 36526708791

This overlay addresses the compiler errors shown in that run:
- restores com.jarvis.models source files
- restores Scheduler entryLogger helper
- fixes kotlinx.serialization JsonObject encoding calls
- imports JsonElement.jsonObject in AuditLogger
- fixes AuditLogger malformed string literals
- adds SpeechRecognizerBridge.close()
- uses the documented OrtSession.getInputNames() method
- adds the missing string resources referenced by Kotlin sources

Apply from the project root. Back up only the listed target files if you have
uncommitted edits in them. Do NOT run git clean -fd.

After extracting, run:
  git diff --check
  git status --short
  git diff --stat

Then commit and push the overlay, and wait for GitHub Actions.

Wake-word model weights are intentionally NOT included. They must remain
separately provisioned and SHA-256 verified.
