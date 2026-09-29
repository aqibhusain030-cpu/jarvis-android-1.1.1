# Baseline Forensic Report — Pre-Remediation

> This document is retained as baseline evidence for the source archive. It describes the pre-remediation state and must not be interpreted as the post-remediation security assessment.

# JARVIS Android Production-Grade ZIP Forensic Analysis Report

**Analysis mode:** Static-only. No extracted project file was executed, rendered, or dynamically run. The archive was handled through a read-only analysis copy. Sandbox execution was not authorized, so Phase 11 was skipped.

**Evidence object:** the current uploaded archive `/mnt/data/jarvis_forensic_work_final2/evidence.zip`; analysis copy hash matched the upload byte-for-byte.

## 1. Executive Summary

The supplied archive is a source distribution for an on-device Android voice assistant using Kotlin/Compose, Hilt, Chaquopy/Python, JNI bindings to llama.cpp/whisper.cpp, Room-backed memory, a tool dispatcher, autonomy/goals, and Android foreground services. Structurally the archive is clean: there are no nested archives, encrypted entries, traversal paths, symlinks, suspicious binaries, or ZIP-bomb conditions. The principal risk comes from application control-plane design and production readiness, not from evidence of malware. The most significant confirmed issues are an unreachable voice-confirmation input path, a configured PIN requirement implemented only as voice affirmation, an open_url allowlist bypass, sensitive read tools without a separate data-sensitivity authorization gate, an unreachable/non-persistent kill-switch control path, microphone FGS startup from boot, a native JNI out-of-bounds-write condition for odd-length PCM, an overbroad permission surface, missing Gradle wrapper artifacts, unpinned external/native/model sources, and a licensing notice that does not distinguish openWakeWord code from pretrained-model licensing.

**Overall archive risk: HIGH (7.5/10), confidence MEDIUM.** The score reflects confirmed source-level security/control weaknesses and release blockers. Confidence is not HIGH overall because the archive does not contain a complete build environment, model artifacts, native third-party sources, or the Gradle wrapper, and signature scanners (YARA/ClamAV/capa/DIE/floss/binwalk/exiftool) were unavailable.

|Metric|Verified value|
|---|---|
|Archive size|140,404 bytes|
|Archive SHA-256|2bb1b9f01527d4fd78c10591a7fd6470dab6977fe524d156c8862a9f85013aae|
|Archive SHA-1|42c292c2d84aefd6b28dd97f46bb8f84fc6a1b36|
|Archive MD5|b45f88631d3e9aa6101940133713f09e|
|Archive CRC32|e8b029ee|
|ZIP entries|197 (148 files + 49 directory entries)|
|Declared uncompressed footprint|230,717 bytes|
|Overall compression ratio|2.4739|
|Encrypted entries|0|
|Nested archives|0|
|Symlinks|0|
|Project languages|Kotlin, Python, C++, Kotlin DSL, XML, Shell, JSON/YAML/Markdown/Properties/ProGuard|
|Critical risk score|7.5 / 10|

## 2. Archive Metadata

|Field|Value|
|---|---|
|Original upload path|/mnt/data/jarvis_forensic_work_final2/evidence.zip|
|Analysis copy|/mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip|
|Archive mtime|2026-09-29T00:10:31.603943+00:00|
|Analysis copy mode|0o444|
|Magic bytes|504b0304 (`PK\x03\x04`)|
|EOCD offset|140382|
|Central directory offset|115639|
|Central directory size|24743|
|ZIP64|False|
|Spanned|False|
|Self-extracting|False|
|ZIP comment|empty|
|Encryption|none|
|Path traversal|none detected|
|Overlapping entries|none detected|

### Chain of custody

|Event|Path/Object|Result|
|---|---|---|
|Evidence received|`/mnt/data/jarvis_forensic_work_final2/evidence.zip`|140,404 bytes; source mtime preserved|
|Forensic copy created|`work/evidence.original.zip`|byte-for-byte SHA/MD5/SHA-1/CRC32 match to source|
|Protection|forensic copy|mode 0444; not modified during analysis|
|Safe extraction|`work/extracted/`|successful; no path escape/overwrite errors|
|Extracted manifest|`report/file_inventory.csv` + JSON|148 file payloads individually hashed|
|Report generation|`/mnt/data/...`|generated from verified evidence/analysis records|

## 3. Methodology

### Controls and limits

The prompt placeholders for authorization level, maximum recursion depth, maximum uncompressed size, maximum compression ratio, password, and sandbox authorization were not populated. No password was necessary because the archive has zero encrypted entries. Because no nested archives exist, recursion depth is immaterial. For bomb controls, the analysis used **1 GiB** as a conservative uncompressed stop threshold and **1000:1** as the compression-ratio alert threshold. Neither condition was reached. Sandbox execution was treated as **NO**, so Phase 11 was not performed.

### Static workflow actually performed

1. Read-only byte-level validation of magic bytes, EOCD, central-directory counts/offsets and archive hashes.
2. Per-entry ZIP metadata extraction, including sizes, CRC32, compression, timestamps, permissions, extra fields, encryption flags and header offsets.
3. Safe extraction under a dedicated directory after validating archive paths. No extracted program was executed.
4. Per-file SHA-256/SHA-1/MD5, MIME/type identification with `file`, entropy calculation, extension/type comparison and structural anomaly detection.
5. Static code/config scans across all textual payloads for secrets, process execution, WebView bridges, TLS bypasses, weak cryptography, database injection patterns, dynamic loading, obfuscation indicators, downloaders, permissions, URLs and error-swallowing.
6. Focused source review of Android lifecycle, tool dispatch, authorization, model supply chain, JNI memory handling, privacy/logging, autonomy, CI/CD and licensing paths.
7. Python syntax, shell syntax and XML/JSON parsing validation. Kotlin was not compiled because the project lacks its wrapper artifacts and external/native sources.

### Tool versions

- `unzip -v`: UnZip 6.00 of 20 April 2009, by Debian. Original by Info-ZIP.
- `zipinfo -v`: ZipInfo 3.00 of 20 April 2009, by Greg Roelofs and the Info-ZIP group.
- `file --version`: file-5.46
- `strings --version`: GNU strings (GNU Binutils for Debian) 2.44
- `python3 --version`: Python 3.13.5
- `bash --version`: GNU bash, version 5.2.37(1)-release (x86_64-pc-linux-gnu)
- `7z i`: NOT AVAILABLE
- `binwalk --version`: NOT AVAILABLE
- `exiftool -ver`: NOT AVAILABLE
- `clamscan --version`: NOT AVAILABLE
- `yara --version`: NOT AVAILABLE
- `diec -h`: NOT AVAILABLE
- `capa --version`: NOT AVAILABLE
- `floss --version`: NOT AVAILABLE
- `trid`: NOT AVAILABLE

Unavailable signature/triage tools: `7z`, `binwalk`, `exiftool`, `clamscan`, `yara`, `diec`, `capa`, `floss`, `trid`; associated confidence is reduced accordingly.

## 4. File Inventory Table

All **148** file payloads are included below. Directory entries are omitted from this table but are fully represented in the directory tree. SHA-256/SHA-1/MD5 are of the uncompressed extracted file payload. Offsets are archive byte offsets where recorded.

|#|Path|Comp|Uncomp|Ratio|Method|CRC32|SHA-256|SHA-1|MD5|MIME|Type|Entropy|Mode|Timestamp|Encrypted|CD offset|Local data start|Local data end|
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
|1|jarvis-android/.github/workflows/ci.yml|388|774|1.9948|deflate|2981351a|dd63fe6cfaaefcd6ea1e97dfe09d36d355c5007caf4d42b1341169615ae655d7|674ef2db70c013546c526bc2a888c742eb7f6e5a|b61d4c0b5f277fa1c5ad1d1b2617c2e4|text/plain|ASCII text|4.8573|0o100644|None|False|245|342|730|
|2|jarvis-android/.gitignore|179|293|1.6369|deflate|fe150d27|b74970c30b12354d346622a537221953760c29ff3511c40b883351c4945f01c4|350a17efa6146f764f31e89e1fa2b150e54e1964|03b1dc2b56fc7d50f63901d69214a4f7|text/plain|ASCII text|4.5557|0o100644|None|False|730|813|992|
|3|jarvis-android/ARCHITECTURE.md|491|741|1.5092|deflate|fa0581a7|1ceae8d47423cc7b730ff352324d286fa4d2b08ab61d91dc016baacd909b660d|662499e030c520d9d8ebdbdaf2bbeb866e507216|e1a0719e25259e807dd57463e526fb24|text/plain|Unicode text, UTF-8 text|5.1698|0o100644|None|False|992|1080|1571|
|4|jarvis-android/LICENSE|490|895|1.8265|deflate|4d4a115d|2855df1c07519837b60a2008bd65e221b463a585f7dcce055b921d74314f9e9a|1a9936bc90e9c607dddeb4e1cc37b80adc1f2c81|f2eb79c09a9cf6a433388905748e4865|text/plain|Unicode text, UTF-8 text|4.7786|0o100644|None|False|1571|1651|2141|
|5|jarvis-android/PRODUCTION.md|1271|2267|1.7836|deflate|464897f9|308c6cd9e236760be6a1e44426d2d9f29bfd8ba7ce84613198dbb9111f5389d4|4f154742f0a97d7d00a904ea036f05b4c37f9cae|b80feb9ef75061d071c44f25a17e8648|text/plain|Unicode text, UTF-8 text|5.1640|0o100644|None|False|2141|2227|3498|
|6|jarvis-android/README.md|2050|3799|1.8532|deflate|862f4720|78a17b71c00ad737325bef2e9ce4c890678fe87421e1f1a29a70068e236718ae|1c9f287c0d88be128a9528c6408f99916019a463|4f6db0d64784429e73f238fa7201883d|text/plain|Unicode text, UTF-8 text|5.3448|0o100644|None|False|3498|3580|5630|
|7|jarvis-android/SECURITY.md|1266|2248|1.7757|deflate|75ce8562|f7f098febb5d09f90ac34b2caab08fa1b40f79db436c98695200ecf3f66c4dc3|5ce8b62d0825efed86d6b1cbecf4db858886f7a9|222c24505baa373688988de602db28d9|text/plain|Unicode text, UTF-8 text|4.9357|0o100644|None|False|5630|5714|6980|
|8|jarvis-android/app/build.gradle.kts|1383|4232|3.0600|deflate|17a1a9c2|6cc6284ac2ba0ed25630c8343cec313d0a5a0b48870fc91c2f46a585fc099c8a|5deb2fffaebf09495888ccb648c2938bcef6ef5c|27d0b0f5c689afa9aea520dae0861fce|text/plain|ASCII text|4.9719|0o100644|None|False|7057|7150|8533|
|9|jarvis-android/app/config.json|776|1960|2.5258|deflate|a98bdf8a|f91ae4f69bab281bf9bf490b86da7667e31958065f30fb3aaac8aadd02e717d3|9240ca2e7210ec10af14de583d7576273da0a0f6|c9ca531d88d8ab2f5948a99c310c8a2f|application/json|JSON text data|4.7990|0o100644|None|False|8533|8621|9397|
|10|jarvis-android/app/proguard-rules.pro|334|982|2.9401|deflate|1e553b83|8e311edd7e01f6c0beef97eb5d60a8fe620681b1bdf13e2c95f85523fc59f762|41fc3a09bde5f10f91c1a851c651195bd4034086|0b62e974852efe09229fa377a76da1f7|text/plain|ASCII text|4.7617|0o100644|None|False|9397|9492|9826|
|11|jarvis-android/app/src/androidTest/kotlin/com/jarvis/ToolInstrumentedTest.kt|290|581|2.0034|deflate|be3ba02f|92dbe6e9d3e22224261bb1321fa80d4096e011b852f747c6d730411a96ab2207|c9f7cc2a9c07d3922a4b83de0c6edd87f03557ef|6fa35c6745c5b55d260a8979968c3e34|text/x-c++|C++ source, ASCII text|4.7247|0o100644|None|False|10315|10449|10739|
|12|jarvis-android/app/src/androidTest/kotlin/com/jarvis/WakeWordInstrumentedTest.kt|293|541|1.8464|deflate|2b73d88b|86b6d7ce30a0de7b2d34effef1ad7270cca517457082f1369694025083875839|5df410f79ea294e94f057b879ab019ebe8506a1f|c8732995e491fc2b75b2d23c695184e6|text/x-c++|C++ source, ASCII text|4.7569|0o100644|None|False|10739|10877|11170|
|13|jarvis-android/app/src/main/AndroidManifest.xml|1505|7070|4.6977|deflate|91775aa6|698b3a7eabc7971f898faa2d65a9721ce2e25eed91072a1c701d8f24005d3f5e|f879b790abd324474c1974208716edbd3d781b7d|6eb1349372525258c7301cd2c1548da2|text/xml|XML 1.0 document, ASCII text|4.9774|0o100644|None|False|11256|11361|12866|
|14|jarvis-android/app/src/main/assets/config.json|776|1960|2.5258|deflate|a98bdf8a|f91ae4f69bab281bf9bf490b86da7667e31958065f30fb3aaac8aadd02e717d3|9240ca2e7210ec10af14de583d7576273da0a0f6|c9ca531d88d8ab2f5948a99c310c8a2f|application/json|JSON text data|4.7990|0o100644|None|False|12959|13063|13839|
|15|jarvis-android/app/src/main/assets/licenses/THIRD_PARTY.txt|187|466|2.4920|deflate|67f09059|5ea3b7a789700f2d1adac74752f6a4e4ca6dc3eae519ada4a95bcd5adbce7d94|f3e77732828738ce2e0cbf1ef0286eb13dccfe4b|f4b04d85b1743d098824801159daf1b3|text/plain|Unicode text, UTF-8 text|3.9965|0o100644|None|False|13941|14058|14245|
|16|jarvis-android/app/src/main/assets/wakeword/.gitkeep|0|0|1.0000|stored|00000000|e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855|da39a3ee5e6b4b0d3255bfef95601890afd80709|d41d8cd98f00b204e9800998ecf8427e|inode/x-empty|empty|0.0000|0o100644|None|False|14347|14457|14457|
|17|jarvis-android/app/src/main/cpp/CMakeLists.txt|361|874|2.4211|deflate|a2772c00|13736c0f8b5dde9f96b9d420df8b8b844eea3d2a91354b95bd3b4950e7ffbc58|8fa68f0217ed6121330e1db88e64df8d4521644c|61cd3ba9b684879b6f10945b63af9725|text/plain|ASCII text|5.4366|0o100644|None|False|14547|14651|15012|
|18|jarvis-android/app/src/main/cpp/llama_jni.cpp|1261|3970|3.1483|deflate|97bae9c7|26c3a94ae0ced4b2890820034fbbdeaf1141c54e25b615b6a803ed0750f0ae21|e9594c1d0d5cafba2156160710f0a285e922a1f4|4f0c31c00c79e027ce76a5e041a8b192|text/x-c|C source, ASCII text|5.0286|0o100644|None|False|15012|15115|16376|
|19|jarvis-android/app/src/main/cpp/whisper_jni.cpp|1043|2682|2.5714|deflate|59ed5360|6cef8b07bc1e391d3633d9bb410e012a8756ebe7e35f061023a5643496bba165|f5b93a87604f02ecb91d3743af8165aae55b8e00|002f7c4f131c63b9ecc70cb4361dae45|text/x-c|C source, ASCII text|5.2006|0o100644|None|False|16376|16481|17524|
|20|jarvis-android/app/src/main/kotlin/com/jarvis/JarvisApplication.kt|1163|3288|2.8272|deflate|82b850b0|ec0839cd891665e8c7fb3794988231d1931fe6b7fb7ff79a0a51dbe0c80e2db7|67ac39b8ba711f7874f4de066216340507947285|0601e77ed185e1b613703dfab0cc3333|text/plain|Unicode text, UTF-8 text|4.7843|0o100644|None|False|17818|17942|19105|
|21|jarvis-android/app/src/main/kotlin/com/jarvis/MainActivity.kt|657|1855|2.8234|deflate|f74b17dd|4988bb37eec35ebfb30cbde5e347e327a528d1416c92032ffad2372f7a09df49|db9a91f42f045a2758b0ae74e8f53249957003ed|eb87b45d80dddabcc9761deba1c26102|text/plain|ASCII text|4.8257|0o100644|None|False|19105|19224|19881|
|22|jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt|1746|5911|3.3855|deflate|5fde4343|93293adbbaedccc179eca38eefbc52127bda07fbc73d2103e8103b069ec8a54f|8e8af6372495ee4d7df8d78c3a72cf517beceda8|367a62071cc75fe2de9b330a6cc9b0fc|text/plain|Nim source code, Unicode text, UTF-8 text|4.4123|0o100644|None|False|19994|20120|21866|
|23|jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/GoalState.kt|502|1284|2.5578|deflate|0d44234a|f161a31550d63ab3f16adf78da243c6039f47ea25d691373f6823ec89819af6a|724214348f3798f63b33f14877deda643cb84338|90b9163fd8f3591d1f1a0b42e0c94a18|text/plain|ASCII text|4.8812|0o100644|None|False|21866|21991|22493|
|24|jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/GoalStore.kt|266|558|2.0977|deflate|e4bcb959|ff7e2d937882c7341cc5ecbc1906859f16041461c4ab7a8a078bd2273270a651|23df16b595fe4cf68850c83e55c6f5c81bd44c66|e3a2ed04b17abae0d3109d8bc3fc95b1|text/plain|ASCII text|4.7259|0o100644|None|False|22493|22618|22884|
|25|jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/Planner.kt|514|1001|1.9475|deflate|ea9062b5|08fe66f4bc8c361e62d8dd1b20260464d8dc43caa0f84926352f784c9ff8b678|f79c2aa2f0e16d4a7a3a12dacee7d4bf39ec5821|ecb11311cf56d098f0d3c1eea3b64147|text/plain|ASCII text|4.8659|0o100644|None|False|22884|23007|23521|
|26|jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/ProgressReporter.kt|469|851|1.8145|deflate|8da70f68|2fed4bd1b1945f85635197aefa09806e3324cbd54fa54d53261a8ff04f71f70c|2e259c10925f2d4baece82ceedcff35a76fb7128|a8b171eeb34a2f2cc7102a5871bf29c1|text/plain|Unicode text, UTF-8 text|4.9261|0o100644|None|False|23521|23653|24122|
|27|jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/Scheduler.kt|1261|3710|2.9421|deflate|35b656e1|01f360fa4ac7a07e6e09718f3aad4690ee23fea427104aec1421708ea6a6a4c4|eb02e5522b7323165737c02604e3ca7cc3b0963b|3f892b0350b420a08189444e0dee4270|text/plain|ASCII text|4.5956|0o100644|None|False|24122|24247|25508|
|28|jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/TriggerManager.kt|780|1993|2.5551|deflate|ced455a2|0f915e0c0f4f5e1f814a827c3c1eee09e4833767c08774b78e31f2f44c676cd8|0d5826694b7f38455f6436ec52f4bb33d9196452|7a6245fda2cd054e311cf75864fcd57d|text/plain|ASCII text|4.8146|0o100644|None|False|25508|25638|26418|
|29|jarvis-android/app/src/main/kotlin/com/jarvis/brain/BrainBootstrap.kt|1201|3278|2.7294|deflate|8c18144d|3bc66e30229c6af881768f797a417188e8af1cabc7f5ca0339315aef3fa483cc|59063d06e489f45d0937e110cfdfe7fb5513d06d|68dd9cabe9faa276e03b6d2619030ec8|text/plain|Unicode text, UTF-8 text|4.5446|0o100644|None|False|26528|26655|27856|
|30|jarvis-android/app/src/main/kotlin/com/jarvis/brain/BrainBridge.kt|1076|2765|2.5697|deflate|b246cbda|b53469e53ab18e2ac38767f0f17363c76f4c9dd0062e9a9fda75d19aa1857593|0519f2b369291dbb9fa55d4f62275ff84de339d0|b41b1f71f3615dc1209245a4e8632691|text/plain|Unicode text, UTF-8 text|4.6790|0o100644|None|False|27856|27980|29056|
|31|jarvis-android/app/src/main/kotlin/com/jarvis/brain/LlamaJNI.kt|241|430|1.7842|deflate|b1843210|90942839c7ca849d6ea91b0b257491744e89b222acfd5f0d5255706fa6a91fd5|9d789cc550f551c80c566a3bce5f1f461b9db766|3c129d788b1e2350a61bbf74a33b668f|text/plain|ASCII text|4.7697|0o100644|None|False|29056|29177|29418|
|32|jarvis-android/app/src/main/kotlin/com/jarvis/brain/LocalLlmEngine.kt|1073|2979|2.7763|deflate|1f043694|44e9c4a84c1d85480e34cfc4221c32ae4906faaf9a84d5f2a947ebe0e63f8434|6a51f177b522331a6f4cd5bba74ed59f8ccc97a0|eb7adc738d60c6ba08b922f4730bc2b9|text/plain|Unicode text, UTF-8 text|4.5489|0o100644|None|False|29418|29545|30618|
|33|jarvis-android/app/src/main/kotlin/com/jarvis/brain/PythonRuntime.kt|512|1081|2.1113|deflate|1679ad5c|2c4216689451598fd7fbb65e76ad5627e459e97a1705898400640f5fc2cd6c41|216b90f6deb020a1383be464e81e08269a1a0390|a2194f8cff804c6cd1749c98cc57ada1|text/plain|ASCII text|4.8773|0o100644|None|False|30618|30744|31256|
|34|jarvis-android/app/src/main/kotlin/com/jarvis/brain/ToolBridgeKotlin.kt|1035|3048|2.9449|deflate|24a845da|ff43a9cef0f809f0894a23cb8d5ea3ac54e70b6c91e371507eee163abd2fc486|97941c36b529f1da7ea34ac50c76c6629dbcca25|6a3c27442e22ae3976314e5e79a45bed|text/plain|Unicode text, UTF-8 text|4.4330|0o100644|None|False|31256|31385|32420|
|35|jarvis-android/app/src/main/kotlin/com/jarvis/brain/WhisperJNI.kt|410|986|2.4049|deflate|30dffc40|5e07c48206938b33303471bdbf4002d9c423a2cb52a01aed3a55c2cc70508525|22eab4a38904ebf32b5c5fb72e2aabb3d6242913|839d66bca51d07f87dacda22b7290804|text/plain|ASCII text|4.7448|0o100644|None|False|32420|32543|32953|
|36|jarvis-android/app/src/main/kotlin/com/jarvis/config/ConfigRepository.kt|1610|5530|3.4348|deflate|e823cf4b|8c9b943da5e4e674336007a101a8c1e745e01eb095b5aec885e9775517e8974b|e9591997e652828c627df46219880e63a1d43847|346f154ab906dcf932e39a24d61f5a74|text/plain|ASCII text|4.9468|0o100644|None|False|33064|33194|34804|
|37|jarvis-android/app/src/main/kotlin/com/jarvis/config/Defaults.kt|850|2570|3.0235|deflate|5b3196b2|5619e7727a9db341b5e9bbe44e1e83b07fef73ea724419f00f54128e37e67737|c57eabf71406b6e4b5ee3cb33717f285dae96f96|ece0e9c82bb4f0533e557995b5ef29ad|text/plain|ASCII text|4.7849|0o100644|None|False|34804|34926|35776|
|38|jarvis-android/app/src/main/kotlin/com/jarvis/config/JarvisConfig.kt|445|1106|2.4854|deflate|e28b6fe6|60f528dbc9332203d66c35b11796029e8c0d0c39bce53c8398c77b9e6e26153a|da653f877d395129cc9cf15850d95d396ba46211|ed0cf5e2d707ba822cd5675c8e7904cb|text/plain|ASCII text|4.7845|0o100644|None|False|35776|35902|36347|
|39|jarvis-android/app/src/main/kotlin/com/jarvis/di/JarvisModule.kt|619|1681|2.7157|deflate|6817cd69|90c5ac7d1c28417e1aba05d2a1fb65ea53046e5a7454f9dde2b6424033c2b68e|d78890e09a056ea13045392eb3a85a7269845043|44dca3e80332e19f5c482c695a6384a1|text/plain|ASCII text|4.6890|0o100644|None|False|36454|36576|37195|
|40|jarvis-android/app/src/main/kotlin/com/jarvis/logging/JarvisLogger.kt|681|1548|2.2731|deflate|1924513a|6f1e35a9e2ce18c9c9396641379104c4b877d2d744c6280e3a90727daf34868b|9a821d8ad625ddf274537fb63133f498545612dd|afbd2ca8f53ba3148824921a1b8d01f3|text/plain|ASCII text|4.8419|0o100644|None|False|37307|37434|38115|
|41|jarvis-android/app/src/main/kotlin/com/jarvis/logging/LogRotator.kt|217|321|1.4793|deflate|009391d2|1cf4e895e4391a0d7b5a2fe3b168684554ff6e3769ec05c2ea7aa7a7d30dc8f5|2a06a8bdb400008c00ab6ef4b0dc7796d23762b1|8c27433917cfd413596857ce19aa64c8|text/plain|ASCII text|4.7532|0o100644|None|False|38115|38240|38457|
|42|jarvis-android/app/src/main/kotlin/com/jarvis/memory/MemoryDao.kt|579|1759|3.0380|deflate|86964081|d17ac4d1b0ab7dc0464372a9fc634b5effef5ba002c309f13f5957b2b54f4162|9c1fddbeef5e1ddf0958aa7f01be6ccbe501e5a4|bc0ee33f2d41fdee1258b3aabb85f2ba|text/plain|ASCII text|5.1107|0o100644|None|False|38568|38691|39270|
|43|jarvis-android/app/src/main/kotlin/com/jarvis/memory/MemoryEntity.kt|408|1462|3.5833|deflate|9e9cb698|aad9cf902588ae2b344a7de69adcf7355caa9935390eeb5b77c6ff2c2feee592|6e2970f435ce77daff8282ae4b2510540fd320e3|574583e4e29df7885aa9bd7583fdff05|text/plain|ASCII text|4.6564|0o100644|None|False|39270|39396|39804|
|44|jarvis-android/app/src/main/kotlin/com/jarvis/memory/MemoryStore.kt|1351|4025|2.9793|deflate|d0b9b3b9|1b3e5e1a0a8383ba84d3567ffc5e5fa7759ecbcbbf844f51ef438dd9a1ea5a90|a92a4f5bab3b2efd40fe215cb8c2dc159ecf95d8|b06acc164e48fd11a2ee683430bcddcb|text/plain|Unicode text, UTF-8 text|4.7870|0o100644|None|False|39804|39929|41280|
|45|jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelChecksum.kt|576|1131|1.9635|deflate|74f9009b|a3f75104eddabaa6c7fb0ee044a91a6f03199a4d4c852f47ebf218fb2af9d076|1b5fea0d9c3398ee205cfbecb52e352930052ef1|299534848080faf1647bf28caa9beab3|text/plain|ASCII text|4.6331|0o100644|None|False|41391|41518|42094|
|46|jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelDownloader.kt|1117|2925|2.6186|deflate|95faf452|9eb77b6e98743770a21d9e06e6afd3a9530b79baa02eb3d666cb0b16e10fcd5d|4d2162132dcc5933d7f9fd40f537fbd2857de71a|1b7d4af2e21e8932b64fed75e5d8f973|text/plain|ASCII text|4.4834|0o100644|None|False|42094|42223|43340|
|47|jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelManager.kt|634|1414|2.2303|deflate|4f3005f0|91510647a35e7465650c07a8507baa0c222c9703570e50968693b8dbf41375a3|a6fbb9522338e55a855d5bfd4426bcd1dd1597e6|75f0769bfc8f2149913505e93fb9ebe4|text/plain|ASCII text|4.8373|0o100644|None|False|43340|43466|44100|
|48|jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelManifest.kt|765|2135|2.7908|deflate|27e5e7d5|b43cba0d4473b7295742c8839b245c7d9f8b29898241b214d8c3fbd1477cf565|f7768e373dffcbaaba4456a03ec6caad85172df1|684ea8b3d08c493ade00091a8c19ff47|text/plain|ASCII text|4.6472|0o100644|None|False|44100|44227|44992|
|49|jarvis-android/app/src/main/kotlin/com/jarvis/notifications/NotificationListener.kt|685|1804|2.6336|deflate|bba82fb9|98e26b12c2e6029a5d5cdf12db435890105fd49c13974df1308202b03af9bc29|086f7343454f6db9eecd62c3487e80f4abbc5558|2f0c31a7dc6f80369bb89c815f0f9e57|text/plain|ASCII text|4.6802|0o100644|None|False|45110|45251|45936|
|50|jarvis-android/app/src/main/kotlin/com/jarvis/notifications/NotificationReply.kt|796|1890|2.3744|deflate|522f32e9|00337cebfb08e942261386665db77a4000965392a08625ce3d2ad26a8b88593b|0748e2234526bdc84dec12320f846e532e965e16|d5721ac41827be731d720fdc41dc784d|text/plain|ASCII text|4.7636|0o100644|None|False|45936|46074|46870|
|51|jarvis-android/app/src/main/kotlin/com/jarvis/notifications/NotificationTriage.kt|403|640|1.5881|deflate|37c85dee|a078ecc993841a62bc1ed727a898bc70a4e1502e31ba23b867a5dbd295626bb8|4bbcf53d8dea6d5086112e85750cd7a5816c9062|18f3fe560f4f9cc7a5561b57351f1a50|text/plain|Unicode text, UTF-8 text|5.0337|0o100644|None|False|46870|47009|47412|
|52|jarvis-android/app/src/main/kotlin/com/jarvis/security/AuditLogger.kt|763|1783|2.3368|deflate|342ea163|275d7a7a9b781381f57b8de99671ac67aa1bf3e7686d1dc7e9482b83fcff4b04|e1c9aec7832668f93d4caab4977a16f86085cb31|e760718700617c263504208425c90508|text/plain|ASCII text|4.7342|0o100644|None|False|47525|47652|48415|
|53|jarvis-android/app/src/main/kotlin/com/jarvis/security/KillSwitch.kt|228|386|1.6930|deflate|cff210b5|5c8009bf2b4dfb9a2842303e9bcfcd60955ffe4289072cf854be1688620225d4|780c7c74028be06fd55d00d9c8130921c805b3e0|30ed63c1e1ea9be437dadc4477975816|text/plain|ASCII text|4.7778|0o100644|None|False|48415|48541|48769|
|54|jarvis-android/app/src/main/kotlin/com/jarvis/security/SecretRedactor.kt|410|723|1.7634|deflate|3a0d83df|73e94e0754866a204e52936f741f1a6f8868e289a21f39046615bb0661afe9aa|bf4046fea3b1eac5d51057eb012acf8276843b2e|1e4a47cdf5711f92ae47817cbd348d05|text/plain|ASCII text|5.0814|0o100644|None|False|48769|48899|49309|
|55|jarvis-android/app/src/main/kotlin/com/jarvis/security/SecurityPolicy.kt|1347|3829|2.8426|deflate|9ef0ce41|a70ec7c29debaf909d6107b7c232cfe8b81d7a2b50bde26aff50c80c66c1e408|9c5d0b2eb69fec472aab5d4e269a8afe43843812|1791bdf6a210f3685a10758de2328ed1|text/plain|Nim source code, Unicode text, UTF-8 text|4.6346|0o100644|None|False|49309|49439|50786|
|56|jarvis-android/app/src/main/kotlin/com/jarvis/service/AudioCapture.kt|929|2255|2.4273|deflate|cf9dbc77|05a67785a8bd1452f39f0c8724167581aa0465403149ca56e32f9b5e8ba7c6e5|8d187f5a95ada5c412ce1e3e1ac3264d75f66208|07bfa367eb4ef67d3dc6824c58b114f2|text/plain|ASCII text|4.8642|0o100644|None|False|50898|51025|51954|
|57|jarvis-android/app/src/main/kotlin/com/jarvis/service/BootReceiver.kt|842|2082|2.4727|deflate|78d0764e|d2ba236167961a9af3c4d2f989b46c6958196c2252ce7241aa9bace5d5110453|c3e93a291ff12e2a9a6973906bdf77a61ea4c5f5|cc874461b95d8abcf2bc722c1de1e18c|text/plain|ASCII text|4.7257|0o100644|None|False|51954|52081|52923|
|58|jarvis-android/app/src/main/kotlin/com/jarvis/service/JarvisForegroundService.kt|2046|6453|3.1540|deflate|8488cd26|e0eae34a0bac0f1d0c633929d3ab093a7a47400e009ba175a6211b37464ebcdf|5312f74fa9e221f81d0ba7be6737ea689c94a5b3|94a72ca74368abef2d74c8386953f2f5|text/plain|ASCII text|4.8224|0o100644|None|False|52923|53061|55107|
|59|jarvis-android/app/src/main/kotlin/com/jarvis/service/JarvisWatchdog.kt|487|1080|2.2177|deflate|c632986f|d37d255f33dbff53680359c2ad48d258d363c9a5d679c51d24485fca0833b348|3140088eb3a1b79fd4124f8dbc327a2b5f1ccbbc|0ab11a7fef64a0ff7b37478354cf677e|text/plain|ASCII text|4.7148|0o100644|None|False|55107|55236|55723|
|60|jarvis-android/app/src/main/kotlin/com/jarvis/service/VoiceActivityDetector.kt|903|2148|2.3787|deflate|64b1e9ee|82eb1afaa3dd18f07eb49d43dc5ac2067a206ae7d2d40c35b42b494a7ea8a6d2|c36e4046fb40b7de991c9bc5d406b48d33875b28|ee02598aa5d6a94e9272f117ed8d8df6|text/plain|ASCII text|4.6718|0o100644|None|False|55723|55859|56762|
|61|jarvis-android/app/src/main/kotlin/com/jarvis/service/WakeWordDetector.kt|1112|2681|2.4110|deflate|fdf19ecd|6343e16dee1b5083b80a261b47d1a10d57ab04af8686e339c8642651d2e6f3b1|b3301bc954150ff347cb86303023f8487bc715cf|f4184152fda3a661b3bf24200a813330|text/plain|ASCII text|4.6117|0o100644|None|False|56762|56893|58005|
|62|jarvis-android/app/src/main/kotlin/com/jarvis/tools/ConfirmationManager.kt|1112|2620|2.3561|deflate|ceaf5501|57e3636ec6031937b2119b33fed3ed41cd459872321c8addd02980253195c132|2de836c503275aa903ae1e1a1274052206cebcc9|e3102683dd5f58a678a9fe0b1b0b9359|text/plain|Unicode text, UTF-8 text|4.8134|0o100644|None|False|58115|58247|59359|
|63|jarvis-android/app/src/main/kotlin/com/jarvis/tools/RateLimiter.kt|452|847|1.8739|deflate|beb52b11|342d64933f1624f4409298ee593ecd0194ab52f4acaab7dafb9e35eadd5775ca|b2d8b807dabd3168e1a8b991fe2cbc5c8e3c217b|685fa6a34f32b9f147caceb886141847|text/plain|ASCII text|4.8564|0o100644|None|False|59359|59483|59935|
|64|jarvis-android/app/src/main/kotlin/com/jarvis/tools/ToolDispatcher.kt|1584|4274|2.6982|deflate|dc8a1a68|b7169788190a1641815109bf70b8e5187dfe01ee078c12949bce94f15c9a5475|df2fb0884473f6fe91b4f648a22428bb3268749e|93ef845d561caa5bbcdc716fd75338aa|text/plain|Unicode text, UTF-8 text|4.9715|0o100644|None|False|59935|60062|61646|
|65|jarvis-android/app/src/main/kotlin/com/jarvis/tools/ToolRegistry.kt|238|411|1.7269|deflate|899da056|2e29aa61beb84331ec3e25d12468515f861a2fd479afc31dabde09b9b29c6a95|e8cbc4bdfd1fb6a02be8a775786d6d7c87475835|962daa79a70a1f508d8b3898080a473c|text/plain|ASCII text|4.7086|0o100644|None|False|61646|61771|62009|
|66|jarvis-android/app/src/main/kotlin/com/jarvis/tools/ToolSpec.kt|604|1631|2.7003|deflate|d1788f3e|9f2241c738e7c03f373a8708bd42c321ceb7d933e6495b7d3957db33b581961f|ca17486a21632e3b9babddde89da42817761a9ec|00cee27a9b70dbf7578113a05323c572|text/plain|ASCII text|4.6964|0o100644|None|False|62009|62130|62734|
|67|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/AlarmTool.kt|691|1523|2.2041|deflate|e21b77f4|49268d1062ba730a636d616df7e74be47a9a497214796aa3e8730eef9d7362ae|529ab0cd9adbe994bf2cbe0ac1c75b8fb45ce755|b3d86bb8c4cffbe2f62ebcd440d3edac|text/plain|Unicode text, UTF-8 text|5.0327|0o100644|None|False|62849|62976|63667|
|68|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/AppLauncherTool.kt|704|1581|2.2457|deflate|2402e76d|d643a56b85103199138aefadb82373cfe9f4b60af47a65285f77742bb3a560ef|1f36b110ac7dc25497d7d046233e0f80bb8b046f|429010a705ab06dd226a934cef2f51ed|text/plain|Unicode text, UTF-8 text|4.9196|0o100644|None|False|63667|63800|64504|
|69|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/BatteryTool.kt|655|1439|2.1969|deflate|e20addeb|f6fbc8f4a225fb1aed1d45fb808d5659b43b88d36e9fcbe8fe24428d2cbedade|6f4e05da2eeefa2970a94a57e6e9cdcfcc75effb|f70339e6287d333d6569e9e63e461dff|text/plain|Unicode text, UTF-8 text|5.1097|0o100644|None|False|64504|64633|65288|
|70|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/BluetoothTool.kt|541|1056|1.9519|deflate|922ed882|7a9f8215d0728efbfe21232e7e968c899d35ed7e019aabf5a523f50f6179238b|4d7ea8556a156f64850474d15669dbd7aedaf1d9|5ad6be3975651eb636b7ade810d83ad2|text/plain|Unicode text, UTF-8 text|4.9799|0o100644|None|False|65288|65419|65960|
|71|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/CalendarTool.kt|806|1663|2.0633|deflate|6f9e4697|c7bbef365ac39af8bcd61d9940d267c6b540c70a65a0fd6c164483b55ba4b9f5|538860e213a3692b18832a55529ee064771bcac4|6aa9b152beb06cb923d6b75be97c4944|text/plain|Unicode text, UTF-8 text|5.0014|0o100644|None|False|65960|66090|66896|
|72|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/CallTool.kt|680|1273|1.8721|deflate|de407e7b|df664670dfd52cf1a8c6a3135fd17371f9a3ea49194f981310e47cd0d3ee5cff|27bbd1000cb12093798f855e5989722715fb4ac6|7f4b6fd7636a2455bff543fe7749047e|text/plain|Unicode text, UTF-8 text|5.0312|0o100644|None|False|66896|67022|67702|
|73|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/CameraTool.kt|658|1219|1.8526|deflate|e24fd7ca|aed0b3a5afab19eae4fb58f31ee88bc076939fe5d5561df9efbf632a2e140b7b|6701876d01d6a069bec91fdbbff398fbe63d621b|3770be15b049922e75a5b5e95f291400|text/plain|Unicode text, UTF-8 text|5.0264|0o100644|None|False|67702|67830|68488|
|74|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/ClipboardTool.kt|485|889|1.8330|deflate|59fefaff|4debb8b7de25e570a35884b291c78f4481efe270d60aab54a6f80c4c67777f90|f20d0c12c045a9a87a3170058fa1c63de6e8aa7c|bb98a1042b27e71413d80191a2c15474|text/plain|Unicode text, UTF-8 text|5.0127|0o100644|None|False|68488|68619|69104|
|75|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/ContactsTool.kt|847|1799|2.1240|deflate|8303acb1|058e7447f16f1457ccf19599ccdd06779fd7ae7336aa0723dc2162264d1730a6|ef558a66b09ba2fad5dfbfb9afa8d960b0715713|69153706a9f61b481b08e46c62731cf9|text/plain|Unicode text, UTF-8 text|5.0347|0o100644|None|False|69104|69234|70081|
|76|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/FileTool.kt|1057|2234|2.1135|deflate|a021719f|35e939dbab58fb10b65cb32fc470a4e3efcdbb239702be6e3a4916eb93ec8765|113c5045af9767196d075029b3ee21470ae88dea|8e051a3e51d73d15a23a98516708e7c5|text/plain|Unicode text, UTF-8 text|4.8940|0o100644|None|False|70081|70207|71264|
|77|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/GoalTool.kt|617|1275|2.0665|deflate|9719f900|1281a969aa4ef390a7f620f3e8ef466d046864b3e6ef9eb4a331a5fcf5bf06ab|54bc6bc8620367d1c0c22b468ee21750386ffcf9|daa858a0fd16c14cced93532ec18fc75|text/plain|Unicode text, UTF-8 text|4.7887|0o100644|None|False|71264|71390|72007|
|78|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/HttpTool.kt|877|1845|2.1038|deflate|fbd9ee8a|a8ca90d013762d49ad2929a054e6cb552a04b6941cd3c1f0569a556b4cf41595|ca1739d8de58d8af90e407cebc6bbf2c3e57ef0c|23d4fd450d8d4affdbcc6a88366a4aab|text/plain|Unicode text, UTF-8 text|4.9249|0o100644|None|False|72007|72133|73010|
|79|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/LocationTool.kt|722|1518|2.1025|deflate|d0e5533d|24bc62064300c7b24300522fc5060c05a8300c94ccbbcbb30297b8d48d50eec9|aa45f799df1945015d2f82086d7ed9381ff27517|c41e0b59a6db602da4ff03ef885ecbdb|text/plain|Unicode text, UTF-8 text|5.0926|0o100644|None|False|73010|73140|73862|
|80|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/MediaTool.kt|743|1633|2.1978|deflate|23d1ac38|053d02fc6ec5db5d8539e96f8ed89439a197053aa48eb7789a579de2880dfcbf|f537ac4ebdbfb6e3cd3286890c72e30fb7363db6|c0596b7c974e0757903828383b4cb5c5|text/plain|Unicode text, UTF-8 text|5.1339|0o100644|None|False|73862|73989|74732|
|81|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/MemoryTool.kt|518|1097|2.1178|deflate|f763a7de|4b57c2549cd7ca91f613ab12e156223be71380fd614a0493834150b966a40632|d5e315392f0dfed8b20b6c86a392221d8495cd2c|10f89bd083e00e2cb39ed13336bba833|text/plain|Unicode text, UTF-8 text|4.8376|0o100644|None|False|74732|74860|75378|
|82|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/NotificationTool.kt|568|1067|1.8785|deflate|11d6e991|9b72b4e833f18fb342c827538e7f1a4a2e1608eba647d9c6fb0edb019c54ec98|2e517676143dc8dc94e1c71da32e13e69bd1167b|fa8000a110bc14b7e76e1753496556cd|text/plain|Unicode text, UTF-8 text|5.0342|0o100644|None|False|75378|75512|76080|
|83|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/NotifyTool.kt|667|1428|2.1409|deflate|70ec9a99|fe6a41a9808bd2c20914845dcd2ac0e699bd4aaaff0d6bf210917904fe47ee89|fb5daa07ecd2b21eea4a730b49934a007c037c9f|af56eef4078c98f192776eadf21d77a3|text/plain|Unicode text, UTF-8 text|4.9163|0o100644|None|False|76080|76208|76875|
|84|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/ScreenshotTool.kt|682|1330|1.9501|deflate|90b6e807|67c10511b3fb5a440fd75f1401556e7fa319c57a7ba9e4392d8b1775d5058a41|b792113986d3c17528efedea198ed89c2961aab4|651029739c2eb835d461b7d5238402d4|text/plain|Unicode text, UTF-8 text|4.8535|0o100644|None|False|76875|77007|77689|
|85|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/SmsTool.kt|686|1385|2.0190|deflate|4b936dd9|3755cd75ca22a3eeb00e30e04ee28fdf123c3aa8b78ff4f5996d7b1ef633a13d|ec53f7d3418459edad4eb88ea70de3b3c09f15c5|35da07c502a6f443e49ece0fbb579aa9|text/plain|Unicode text, UTF-8 text|4.8747|0o100644|None|False|77689|77814|78500|
|86|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/SystemTool.kt|675|1347|1.9956|deflate|c8fad10a|fab879a915b438ecd8316c3ee172d669351a3e7dee3b15900fbac6b5e43ec52d|fc6862be257c5cf00af0b57d9f21372ae5c7e4a6|2eb03c0c10662e6f03ecde3dd1926db9|text/plain|Unicode text, UTF-8 text|5.1031|0o100644|None|False|78500|78628|79303|
|87|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/TimerTool.kt|646|1303|2.0170|deflate|45adbad6|6745cce14087cbde334bfbb6ed26644e4f4a480c59bc6eb5cb3351ef60f36254|8227a09513e43bd9a9b376ac3c706d88d350c2f9|998d8c035fab5bca8c92092b2477a708|text/plain|Unicode text, UTF-8 text|4.9891|0o100644|None|False|79303|79430|80076|
|88|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/TorchTool.kt|779|1600|2.0539|deflate|bc21db82|d898de5fb4874d5f7064652ee35a1dbe528ba2e9624ea24aa4a8b8f1e53f6e6c|5ada04e7efdcf6db624e64c3fb37ade125354d07|9b16ec4d052466c3d7d6cdfccd68cf2e|text/plain|Unicode text, UTF-8 text|4.9644|0o100644|None|False|80076|80203|80982|
|89|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/UrlTool.kt|829|1749|2.1098|deflate|1448b73e|346f61a986747a3aba1c176a137dd02917c0fdf58196f7a8d3353acd36f3bea1|a28cfbd50163ec00474096abb2f9a0639a234370|6edd0c698a14b30de93d3a0399c9af09|text/plain|Unicode text, UTF-8 text|4.8205|0o100644|None|False|80982|81107|81936|
|90|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/VolumeTool.kt|719|1477|2.0542|deflate|791e02d6|861c3628e2a4a17f7320724e43f9001195775a75f21e4e741c33289211877a95|974bac51d0ac7139d91e2dededc816c1e8511e3c|8ccf7335e588c93be589d56afd8d26d8|text/plain|Unicode text, UTF-8 text|4.9633|0o100644|None|False|81936|82064|82783|
|91|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/WeatherTool.kt|842|1739|2.0653|deflate|63b093b1|9934b593f7fa5a57be9bc21a6bf1be24ae12264e14aa6f36c2007d7bf1f88b23|99f15c305254b5cd1defd2a30a4d5a11305dabee|727a9700f036e2f97584b5d46ab76f4a|text/plain|Unicode text, UTF-8 text|4.9912|0o100644|None|False|82783|82912|83754|
|92|jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/WifiTool.kt|527|950|1.8027|deflate|68c9aa65|e600d8e5f09fdf15f6aa158a5bf242d5335419cff5ec346fab1a3197b8c2851b|3c13195685b93fed90966942fe83f1b57f5a1f2b|6a23f125fd0f48a6cc6cd8db0f616e9c|text/plain|Unicode text, UTF-8 text|5.0354|0o100644|None|False|83754|83880|84407|
|93|jarvis-android/app/src/main/kotlin/com/jarvis/ui/GoalsScreen.kt|280|569|2.0321|deflate|deee991f|aa99403a85a44259ffc975192c4c3959530491dfd6944e57e8f515852adf7355|3b29565bb7cca0ad1f772667bcebeb7c85fccd3b|66195e1063a370a384a5c6de58cd4dd1|text/plain|ASCII text|4.8237|0o100644|None|False|84514|84635|84915|
|94|jarvis-android/app/src/main/kotlin/com/jarvis/ui/HomeScreen.kt|841|3104|3.6908|deflate|9808c0d3|3b8ddac2838292801d4144e34a8e2fc70b09f21c4775cb29e9836b80b7bc1172|d219abd59b99983c1992f9a7708eabe9f491d913|2ee072c73d56b571730803184be9ac0b|text/plain|ASCII text|4.4254|0o100644|None|False|84915|85035|85876|
|95|jarvis-android/app/src/main/kotlin/com/jarvis/ui/LogsScreen.kt|280|566|2.0214|deflate|4ca1e450|4bd02da33a822bfafff3e06e9dfbe548a0d481402ea3c9297b6ed09aa97b2424|e0be3669bf51e629aa6a18ae3f5b148eb816c6dd|112f34b36ff4a564181ded2a3f4de0db|text/plain|ASCII text|4.8279|0o100644|None|False|85876|85996|86276|
|96|jarvis-android/app/src/main/kotlin/com/jarvis/ui/MemoryScreen.kt|280|572|2.0429|deflate|95dfb207|a3ebff2ce57f0697b21c448f4970be76f7d7b4b425976e7ec0cbe78f01fa697d|23db7b4eb34ea70367eb83beb498c329f68a22d3|5e0ec613679ce3807e2cec2f33ff235c|text/plain|ASCII text|4.8186|0o100644|None|False|86276|86398|86678|
|97|jarvis-android/app/src/main/kotlin/com/jarvis/ui/OnboardingScreen.kt|1016|4064|4.0000|deflate|02bc5838|a28c2607ef37519f72aef45985711e6c0b6bf8b3d179a79e279240e2571771da|180f3cb9ee0a93a3dbf4bbc73d7fa98297a3b65a|e0b1f2fd60c9f84eb7abc277f6d7adc8|text/plain|Unicode text, UTF-8 text|4.5536|0o100644|None|False|86678|86804|87820|
|98|jarvis-android/app/src/main/kotlin/com/jarvis/ui/SettingsScreen.kt|394|940|2.3858|deflate|092c318a|81683eb5885300e61b8c36a2f75e7b30b54b869265ad8f1f7cf2a5a1bdfc16f7|ad4b8f5bd97b711c3eb7dd3fed8f42f8cc96f1e1|d0d9f2fa7a580b000fbcffa88d981ad2|text/plain|Unicode text, UTF-8 text|4.8267|0o100644|None|False|87820|87944|88338|
|99|jarvis-android/app/src/main/kotlin/com/jarvis/ui/theme/Color.kt|138|217|1.5725|deflate|0fe7d1b7|d06178817991b7a85a85a679f95628f8445e533ef2ce1d9d10b191c6b1be3780|c43f9b8d01aa5e6b5d9ea5369904d9bc9327c456|d23bf670c03262e9b44efeaf2a8443de|text/plain|ASCII text|4.8955|0o100644|None|False|88451|88572|88710|
|100|jarvis-android/app/src/main/kotlin/com/jarvis/ui/theme/Theme.kt|304|632|2.0789|deflate|e647cd6c|0a5692aa2d23e9c0e8fecafd10f3ffdc167aaa81cc45e491a7ba20998a85ee73|d8ddd983be800a1ff2298daf4b9d34b2c0187220|c918c9ce1ccb12d4a1c6d204b591d7b2|text/plain|ASCII text|4.9658|0o100644|None|False|88710|88831|89135|
|101|jarvis-android/app/src/main/kotlin/com/jarvis/ui/theme/Type.kt|218|547|2.5092|deflate|ca1d8031|650450de973c859750424fca7a2ea97cea2a30f842b25c51c099cd59ae4fa128|8c9beaa32958cd6972087840124d8e8242fe5afb|425bd3a3caf376dba8eed91860aa3a58|text/plain|ASCII text|4.8242|0o100644|None|False|89135|89255|89473|
|102|jarvis-android/app/src/main/kotlin/com/jarvis/util/Dispatchers.kt|161|350|2.1739|deflate|f8c63325|425ca98407978dbb19e4910814dd55a3c19af4caf810de3da25adcdfafd1705c|d840a37640b4655b4215b60ce1dc989f283e6a7a|91821efa37bcf4f72a9217c5da4a3a63|text/plain|ASCII text|4.5418|0o100644|None|False|89582|89705|89866|
|103|jarvis-android/app/src/main/kotlin/com/jarvis/util/Extensions.kt|263|536|2.0380|deflate|fdfea2ef|a43f745fe8c70cd27ecf3c82e8960524d69b540baf3713e68d78cab67a7acfcd|65eca059a0ca83e2dac55b0fa65ffe7317dce5bb|2c5fbdf1a3c8c7dfd67fe1f6400a6786|text/plain|ASCII text|5.0515|0o100644|None|False|89866|89988|90251|
|104|jarvis-android/app/src/main/kotlin/com/jarvis/util/Result.kt|249|441|1.7711|deflate|0ba80d15|6e4ea2f994595d458ce182308a91d8e0cf9d78d799dcee2034be5d417e4d828f|3873a1059a64792d0454ffe7389a485bb16bef34|438bfa0deafae440cde87c166862f3f5|text/plain|ASCII text|4.7991|0o100644|None|False|90251|90369|90618|
|105|jarvis-android/app/src/main/kotlin/com/jarvis/util/Time.kt|206|345|1.6748|deflate|843fd7b1|cbbbbe34311c5067c80f82fa1c4c3e543b0d775cf18cf22ad1e319ba3baacd59|9c73ae1d232de116907e3f321def5ccc7e5184e6|c8334940271417d82ab6cb3d0253a2cb|text/plain|ASCII text|4.8049|0o100644|None|False|90618|90734|90940|
|106|jarvis-android/app/src/main/kotlin/com/jarvis/voice/BargeInController.kt|326|556|1.7055|deflate|fdf911d3|a807f98fcb98264d2b2f614c81bd50e51e17890c0cb975e55aff07a7a80e0e2c|abc93fe319e2ae7d185f53af8ed154e9c60653dd|29e57c755e0e0c78fd84db4e19a89e08|text/plain|ASCII text|4.7476|0o100644|None|False|91050|91180|91506|
|107|jarvis-android/app/src/main/kotlin/com/jarvis/voice/LanguageDetector.kt|600|1300|2.1667|deflate|5e6a7495|a0b7f918af715226539b01f3ba1e66ff4b46469177432c084152768f3101ea19|b7629ca4aace6c36df50922813905e748274f3d2|3e8d0291179c9877f06113955f756c78|text/plain|Unicode text, UTF-8 text|4.8915|0o100644|None|False|91506|91635|92235|
|108|jarvis-android/app/src/main/kotlin/com/jarvis/voice/SpeechRecognizerBridge.kt|667|1545|2.3163|deflate|3eb8c6fc|d5d65ded658d6972365f08d6810edb97854c26588413e716853c2d90227245c7|6daa49e4a83687f4cf9a88d888100486d196da92|713ccbd31fb36523c1ca8a49367e37da|text/plain|Unicode text, UTF-8 text|4.8392|0o100644|None|False|92235|92370|93037|
|109|jarvis-android/app/src/main/kotlin/com/jarvis/voice/TextToSpeechBridge.kt|1324|3611|2.7273|deflate|aa6fae8b|5b17af70f8565311ffc9b53ade9434aafa53c1726179242df3fe1c30881e6447|7a67bc8552826a73c35f81f22f06502b9d61f6d0|b90be8e816655da0d26c5034d9642640|text/plain|Unicode text, UTF-8 text|4.6523|0o100644|None|False|93037|93168|94492|
|110|jarvis-android/app/src/main/python/brain.py|2583|6932|2.6837|deflate|69e39361|c0aab7e84567b589211258d9f8487c8dc3b29a59137b91a07ae8d43605cc8381|0675ae7b8bba4b1ec9001499b2335a1806cc086a|ee00e07c1450f1559b7c7801680695d1|text/plain|Python script, Unicode text, UTF-8 text executable|4.9581|0o100644|None|False|94585|94686|97269|
|111|jarvis-android/app/src/main/python/executor.py|421|948|2.2518|deflate|6ec65907|3f6fb8b9555bfca560b9a0f05ee700bd344273ca2417bf233ac0b62f4bb7a1f8|457f587af7e3207fdba7d3ce33f9f08e37c8e0af|74427c7c3e5a9ff1ed6902db5e731f21|text/plain|Python script, ASCII text executable|4.5702|0o100644|None|False|97269|97373|97794|
|112|jarvis-android/app/src/main/python/language.py|455|864|1.8989|deflate|738f8d63|cb6c7e2ff9f229b0ff7c5c233789d4136be815e7c30b976b0645678993bebe61|a3f641b6eba4d453994cc0c152c8bd825d2da7b6|0b1689f2d6d2734a718f2eea74c247c9|text/plain|Python script, Unicode text, UTF-8 text executable|4.8180|0o100644|None|False|97794|97898|98353|
|113|jarvis-android/app/src/main/python/memory.py|465|1011|2.1742|deflate|b1e56c61|3a8e3c49fc744f5b472307f2f3838a686ca3d016c7ec42137c5dcc5176b4cab6|0ecea68c69878b0bc30e06b92291dc9f545ad530|780db7b6eb1d70da18264463c3b7b49b|text/plain|Python script, Unicode text, UTF-8 text executable|4.6507|0o100644|None|False|98353|98455|98920|
|114|jarvis-android/app/src/main/python/planner.py|622|1335|2.1463|deflate|a64871e3|cc8a3a3bf8251257543cbfc4a8bb0e695522f2b23c1ddc8424cae5b34217c503|96dead07e34c4cd9f5765b1598f2d517ba61b17f|49e47035fa0f260ed38a6366bbbc56ba|text/plain|Python script, Unicode text, UTF-8 text executable|4.6684|0o100644|None|False|98920|99023|99645|
|115|jarvis-android/app/src/main/python/prompts/brief.txt|122|156|1.2787|deflate|d9351b2b|787551f21904e185f383fe7c56d55a7f730a1ba0167ab0217f70953ca1b170e3|747e33feb141ab919725b1e3a10b3053dfac7527|494cac02762916c51ae555e2200d20cb|text/plain|ASCII text|4.3945|0o100644|None|False|99746|99856|99978|
|116|jarvis-android/app/src/main/python/prompts/confirm.txt|0|0|1.0000|stored|00000000|e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855|da39a3ee5e6b4b0d3255bfef95601890afd80709|d41d8cd98f00b204e9800998ecf8427e|inode/x-empty|empty|0.0000|0o100644|None|False|99978|100090|100090|
|117|jarvis-android/app/src/main/python/prompts/planner.txt|232|320|1.3793|deflate|bd323d8b|c65d30bcd57def9bc0e70f154be85df404e559d6846a684dfa956951c41a002c|178a515f044bbe56c866deda4584c10d22a4f683|12e24e4d3d2d79e91e0b5c8ce4c37203|text/plain|ASCII text|4.8213|0o100644|None|False|100090|100202|100434|
|118|jarvis-android/app/src/main/python/prompts/reflector.txt|114|147|1.2895|deflate|d7d08086|c5a829e685ff00093bbbde8d7ba018d0720432bb35e335ca120e871f015059c4|a2e2dcc32f367485ac1245d21d3aa2f4b7d7bf6c|7938ade8cc2de3215579dec6ef1aafc2|text/plain|ASCII text|4.2139|0o100644|None|False|100434|100548|100662|
|119|jarvis-android/app/src/main/python/prompts/system_en.txt|302|446|1.4768|deflate|5aeff3a1|b294b88563b958bd720a96f8cfd6f343d0103c02310c9cfee6ded1dcf9532da4|d22a44f5a7ce91d354312534cd42c9484bba4b86|ca4088d969986edbf914856cc94e6289|text/plain|Unicode text, UTF-8 text|4.5892|0o100644|None|False|100662|100776|101078|
|120|jarvis-android/app/src/main/python/prompts/system_hi.txt|442|1060|2.3982|deflate|7f8c9130|fb23a106345680462ca0b5f544e42e52017844005d9a84ee9f28a893a3726c9a|ebc4b5dda711b9d46e71b366b42fe95916c3b415|ae8ef1edb19ac08dce4eb6d3bd362f73|text/plain|Unicode text, UTF-8 text|3.7190|0o100644|None|False|101078|101192|101634|
|121|jarvis-android/app/src/main/python/prompts/system_hinglish.txt|263|381|1.4487|deflate|e55642d3|9a56778de2f68e6d72a53ac0c0c1239e71034fb38d6bd7e5c737af4fce55c2b4|f91f44b7d00ce6bb0513d669d2afe8eaa276959c|e80bfb9b04900ff9d8501b5a25df69b5|text/plain|Unicode text, UTF-8 text|4.5619|0o100644|None|False|101634|101754|102017|
|122|jarvis-android/app/src/main/python/reflector.py|215|407|1.8930|deflate|305ce3d4|a63e6c3a43fc56d14faa772f3ea3de28c0d21d80560bd943d6c48d168ab41004|7b4ac6cef92c95720b0a43c202a8b12994676df9|cfd74ae6b41c43cd6f694541591b1e09|text/plain|Python script, Unicode text, UTF-8 text executable|4.4776|0o100644|None|False|102017|102122|102337|
|123|jarvis-android/app/src/main/python/tools_bridge.py|415|833|2.0072|deflate|91fd6b1b|014c429653335381a7af45c4806bfaae6e02b94d88f7f3acf81f108874b73b28|f55461e955a9a92709435220d3054f4424e46f89|db924cbeaa38a402bf4dcc033ec3d1fc|text/plain|Python script, ASCII text executable|4.7754|0o100644|None|False|102337|102445|102860|
|124|jarvis-android/app/src/main/python/utils.py|317|542|1.7098|deflate|39fa3f15|46a4d4c8ee04cdb63f390f120ca5e96156c06f4ae5438f2aab10f9aee3969a6a|0101e2c52e7954b56bc2bbe4b63c09e621ffd0bf|542f45401c6d20e4c3eaa37663a0ef94|text/plain|Python script, ASCII text executable|4.8001|0o100644|None|False|102860|102961|103278|
|125|jarvis-android/app/src/main/res/drawable/ic_jarvis.xml|229|424|1.8515|deflate|016a8aa3|11bc63dc8920839ad7cfab60920db036c553211d010bde4dfcf57fd4c6a1e07f|8489554b7c14bfa4c49e9ee578b31c8c84f3e0ce|bceb588278df71504a15a699ecd477bd|text/plain|ASCII text|5.0828|0o100644|None|False|103467|103579|103808|
|126|jarvis-android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml|145|245|1.6897|deflate|684f4b2d|1782b32fd6bc501feb69f85dba8d38b4336b183044f1a43b44f661c2a2721441|9cd7fb9b9be69750d3853f59bec717107e2d3355|349ffd81815843a73dcca967051f0113|text/xml|XML 1.0 document, ASCII text|4.8441|0o100644|None|False|103916|104039|104184|
|127|jarvis-android/app/src/main/res/raw/.gitkeep|0|0|1.0000|stored|00000000|e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855|da39a3ee5e6b4b0d3255bfef95601890afd80709|d41d8cd98f00b204e9800998ecf8427e|inode/x-empty|empty|0.0000|0o100644|None|False|104278|104380|104380|
|128|jarvis-android/app/src/main/res/values/strings.xml|648|2064|3.1852|deflate|8d331742|521d87dafb4dcaf13d2379a7089ca3ed107f5ac710f969477dcac845bf588229|36d99a5463a6aadf6e4d1873c6ade5839b9d2e0f|d7d90af972130f5acf5eb862a33aeff8|text/xml|XML 1.0 document, Unicode text, UTF-8 text|4.7522|0o100644|None|False|104477|104585|105233|
|129|jarvis-android/app/src/main/res/values/themes.xml|201|399|1.9851|deflate|7aa49dbc|f3ab473edebb38a7514823dcde6323306656337b7f9cbe5381f575cc5de470af|dd2a418aaea38ce34aae6e6487c310b9a030b002|5f3aec905312bfa5bbd91d7907cd9468|text/xml|XML 1.0 document, ASCII text|4.8449|0o100644|None|False|105233|105340|105541|
|130|jarvis-android/app/src/main/res/values-hi/strings.xml|890|3016|3.3888|deflate|a89559f8|f70fc76cc0e8c0ccfb3a12490d1f45dc1193fec245a4743f0e17f16128aa2094|df2c7ccfd9e48c1099e1c9b98e29ec660b031535|3f361bb0b7389da16143a194c15085de|text/xml|XML 1.0 document, Unicode text, UTF-8 text|4.9038|0o100644|None|False|105641|105752|106642|
|131|jarvis-android/app/src/main/res/xml/backup_rules.xml|126|193|1.5317|deflate|0c79a050|ad4f97a02c19cd39afe97fc113623a9c954e59874af6bb60fb3f5cfdf452ba08|77e9660255e043b3739b58fad30a54fc8f1fe8ff|646c6f31c3bc849950bf42552f85e0ac|text/xml|XML 1.0 document, ASCII text|4.8861|0o100644|None|False|106736|106846|106972|
|132|jarvis-android/app/src/main/res/xml/data_extraction_rules.xml|160|352|2.2000|deflate|eaab760d|c726953d8c1b138ccb409592e976cc40a051e478236868e8175f03382bed8c1f|82f303198777ecbfc7ea699da755a63e081c6ddc|913959b07e1537a66fda7c8994d9705a|text/xml|XML 1.0 document, ASCII text|4.7024|0o100644|None|False|106972|107091|107251|
|133|jarvis-android/app/src/main/res/xml/file_paths.xml|117|209|1.7863|deflate|1888d2ee|f82d78caa3b7725a6d24e4a84517c9a58104e3943b350e4b6f6418592d29f179|b6a5838156752c8f0c88468a058fb998e8382a35|0f746b9bc92f2b63107f6825a159031d|text/xml|XML 1.0 document, ASCII text|4.6756|0o100644|None|False|107251|107359|107476|
|134|jarvis-android/app/src/test/kotlin/com/jarvis/GoalEngineTest.kt|296|629|2.1250|deflate|2f187f55|8ec89ad943a22181b22e1ae31ffa40278154fbb02e2707d92fc6bebd0374145d|55691506e2c18db7a93b438f359bb7b4d7e9d710|cc9dbe6e800b0778a41cef9d0a17ed93|text/x-c++|C++ source, ASCII text|4.7421|0o100644|None|False|107856|107977|108273|
|135|jarvis-android/app/src/test/kotlin/com/jarvis/LanguageDetectorTest.kt|435|1123|2.5816|deflate|db920459|6745de6f7dca0f96e6c2077168e212e2b99bc3196313f332f4b01f8e129942bb|0e1025c7c6cfe1c4a9c6f77a01b03cf0f6c5e538|b741808e2896e631a37f95c5a253b047|text/x-c++|C++ source, Unicode text, UTF-8 text|5.0284|0o100644|None|False|108273|108400|108835|
|136|jarvis-android/app/src/test/kotlin/com/jarvis/MemoryStoreTest.kt|251|594|2.3665|deflate|1ece4a3d|d1efb29f12ae70320241c19904927cb29fe941622acc027ee7f1a7c1486e320e|9eb2065af41a6c040d878ee3eeed40997a23b9af|5a345ddec882d7100d73d36e731695ca|text/x-c++|C++ source, ASCII text|4.6443|0o100644|None|False|108835|108957|109208|
|137|jarvis-android/app/src/test/kotlin/com/jarvis/SecurityPolicyTest.kt|368|1065|2.8940|deflate|35cb0388|9776c0abdfa1d75972b1d7e376c8ad6b7542f1634e80363cac1e07977399b00c|f9f34fc603898519f580f5b0e3b4950ed2641d23|76d82c870548d0798d0f8d105a1e2e27|text/x-c++|C++ source, ASCII text|4.7384|0o100644|None|False|109208|109333|109701|
|138|jarvis-android/app/src/test/kotlin/com/jarvis/ToolDispatcherTest.kt|1172|4575|3.9036|deflate|670d6c13|a476ac4853553f73db6d821ac7e4b16cd0f28a5dadb2965afe469e8e294322cb|ea4f02814fcc6ecfd38fffdada3f48c617877ae4|e1c9d0e763704b5416fe493590d7af53|text/x-c++|C++ source, Unicode text, UTF-8 text|4.7572|0o100644|None|False|109701|109826|110998|
|139|jarvis-android/build.gradle.kts|162|406|2.5062|deflate|941ea533|3e1ebf594dfbb16539424ad6d286147863f49f43b19b55e370574f3637280993|c08c80479f39e9e0c3970c8718b82224f09abca3|1dbaf0177b652c98a1130544cf97a72c|text/plain|ASCII text|4.6166|0o100644|None|False|110998|111087|111249|
|140|jarvis-android/config.json|776|1960|2.5258|deflate|a98bdf8a|f91ae4f69bab281bf9bf490b86da7667e31958065f30fb3aaac8aadd02e717d3|9240ca2e7210ec10af14de583d7576273da0a0f6|c9ca531d88d8ab2f5948a99c310c8a2f|application/json|JSON text data|4.7990|0o100644|None|False|111249|111333|112109|
|141|jarvis-android/gradle/wrapper/gradle-wrapper.properties|151|250|1.6556|deflate|cc3d9394|712d9bc912d4b0d42b0faf8c6afeed13ae31ced75ca28463b1dfc4ced1a4bc9a|2ab50db15d600bf80a94044ec878aa18fbb8bd45|1c0fb9d246a28f2f5c596f60ad67f8c1|text/plain|ASCII text|4.9728|0o100644|None|False|112277|112390|112541|
|142|jarvis-android/gradle.properties|180|267|1.4833|deflate|57237d61|213d5bbdf4b2169d55e2c83ed5170a116abccaad048ddbe0c3977d78212183c1|0d50d0188e0a5519bd0e5c110a8079a0c43dc402|c013c251e1b0619c343fb9af6f391fa9|text/plain|ASCII text|4.8257|0o100644|None|False|112541|112631|112811|
|143|jarvis-android/scripts/build_llama.sh|464|757|1.6315|deflate|096fb349|3d64825b837aff69460888a2ef4b267b544a437cc5b52bea35b350d337c0ac98|db1d31e9fd658756340245df61b6ab59f5edffa5|89690240119833571cf5aa39c7fb9e56|text/x-shellscript|Bourne-Again shell script, Unicode text, UTF-8 text executable|5.6491|0o100644|None|False|112892|112987|113451|
|144|jarvis-android/scripts/build_whisper.sh|437|685|1.5675|deflate|d489409e|ed7d0c1a6e9d421a745d3239a4c45567f47c4fc904f314cf546e80d8a17c964f|fed9c23072b8cd43e1c491ef80ad06be38e4661a|ea95e19580a6466d5aac010f45f3ad7c|text/x-shellscript|Bourne-Again shell script, Unicode text, UTF-8 text executable|5.6773|0o100644|None|False|113451|113548|113985|
|145|jarvis-android/scripts/fetch_models.sh|568|1130|1.9894|deflate|2e000442|b77bbec2b4a6e7ff29558396d523e6327877990f353d195f9d331ad92205cd19|c77df290c3e458491842e19c5c174ca6679fe76d|17abe0dfc56a1393deba72bc39ada48a|text/x-shellscript|Bourne-Again shell script, Unicode text, UTF-8 text executable|5.4900|0o100644|None|False|113985|114081|114649|
|146|jarvis-android/scripts/install_debug.sh|198|241|1.2172|deflate|37cb230a|732a5f97c882ed820f55c27db3cb73f3645d30274ab977f758fcdb21c90f2a3e|58034ca0dcc6fa4a07e17b9544b1b73fed7eaf46|e0fb334828f616bb7d1ee7512db97b79|text/x-shellscript|Bourne-Again shell script, ASCII text executable|5.2071|0o100644|None|False|114649|114746|114944|
|147|jarvis-android/scripts/sign_release.sh|287|541|1.8850|deflate|959be670|25b8112ab3e14d86f405f8a80866151516b541844836e80846a9300b178dca0e|1cf19f452023931b25545ef4830de866e66c3752|9c8e2cf14500a354e75f8e15c0065f85|text/x-shellscript|Bourne-Again shell script, Unicode text, UTF-8 text executable|5.4466|0o100644|None|False|114944|115040|115327|
|148|jarvis-android/settings.gradle.kts|220|399|1.8136|deflate|14e8752c|187b0a512de20f0480883624b19320c5ac8871a9e5ca9fa9f2de64e36035e13c|4feeec3d5d1f0730d5e4a9c2277a1f8cb41ec550|12c7fe8a58225837ab23d9432a5e3ff7|text/plain|ASCII text|4.7912|0o100644|None|False|115327|115419|115639|

### Extension / language distribution

|Extension / category|Count|
|---|---|
|.kt|97|
|.xml|9|
|.txt|9|
|.py|8|
|.sh|5|
|[none]|4|
|.md|4|
|.kts|3|
|.json|3|
|.cpp|2|
|.properties|2|
|.yml|1|
|.pro|1|

### MIME distribution

|MIME|Count|
|---|---|
|text/plain|120|
|text/xml|8|
|text/x-c++|7|
|text/x-shellscript|5|
|application/json|3|
|inode/x-empty|3|
|text/x-c|2|

## 5. Directory Tree

```text
[D] jarvis-android
  [D] jarvis-android/.github
    [D] jarvis-android/.github/workflows
      [F] jarvis-android/.github/workflows/ci.yml
  [F] jarvis-android/.gitignore
  [F] jarvis-android/ARCHITECTURE.md
  [F] jarvis-android/LICENSE
  [F] jarvis-android/PRODUCTION.md
  [F] jarvis-android/README.md
  [F] jarvis-android/SECURITY.md
  [D] jarvis-android/app
    [F] jarvis-android/app/build.gradle.kts
    [F] jarvis-android/app/config.json
    [F] jarvis-android/app/proguard-rules.pro
    [D] jarvis-android/app/src
      [D] jarvis-android/app/src/androidTest
        [D] jarvis-android/app/src/androidTest/kotlin
          [D] jarvis-android/app/src/androidTest/kotlin/com
            [D] jarvis-android/app/src/androidTest/kotlin/com/jarvis
              [F] jarvis-android/app/src/androidTest/kotlin/com/jarvis/ToolInstrumentedTest.kt
              [F] jarvis-android/app/src/androidTest/kotlin/com/jarvis/WakeWordInstrumentedTest.kt
      [D] jarvis-android/app/src/main
        [F] jarvis-android/app/src/main/AndroidManifest.xml
        [D] jarvis-android/app/src/main/assets
          [F] jarvis-android/app/src/main/assets/config.json
          [D] jarvis-android/app/src/main/assets/licenses
            [F] jarvis-android/app/src/main/assets/licenses/THIRD_PARTY.txt
          [D] jarvis-android/app/src/main/assets/wakeword
            [F] jarvis-android/app/src/main/assets/wakeword/.gitkeep
        [D] jarvis-android/app/src/main/cpp
          [F] jarvis-android/app/src/main/cpp/CMakeLists.txt
          [F] jarvis-android/app/src/main/cpp/llama_jni.cpp
          [F] jarvis-android/app/src/main/cpp/whisper_jni.cpp
        [D] jarvis-android/app/src/main/kotlin
          [D] jarvis-android/app/src/main/kotlin/com
            [D] jarvis-android/app/src/main/kotlin/com/jarvis
              [F] jarvis-android/app/src/main/kotlin/com/jarvis/JarvisApplication.kt
              [F] jarvis-android/app/src/main/kotlin/com/jarvis/MainActivity.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/GoalState.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/GoalStore.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/Planner.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/ProgressReporter.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/Scheduler.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/autonomy/TriggerManager.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/brain
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/brain/BrainBootstrap.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/brain/BrainBridge.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/brain/LlamaJNI.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/brain/LocalLlmEngine.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/brain/PythonRuntime.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/brain/ToolBridgeKotlin.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/brain/WhisperJNI.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/config
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/config/ConfigRepository.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/config/Defaults.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/config/JarvisConfig.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/di
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/di/JarvisModule.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/logging
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/logging/JarvisLogger.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/logging/LogRotator.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/memory
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/memory/MemoryDao.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/memory/MemoryEntity.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/memory/MemoryStore.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/models
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelChecksum.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelDownloader.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelManager.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/models/ModelManifest.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/notifications
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/notifications/NotificationListener.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/notifications/NotificationReply.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/notifications/NotificationTriage.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/security
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/security/AuditLogger.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/security/KillSwitch.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/security/SecretRedactor.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/security/SecurityPolicy.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/service
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/service/AudioCapture.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/service/BootReceiver.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/service/JarvisForegroundService.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/service/JarvisWatchdog.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/service/VoiceActivityDetector.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/service/WakeWordDetector.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/tools
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/ConfirmationManager.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/RateLimiter.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/ToolDispatcher.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/ToolRegistry.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/ToolSpec.kt
                [D] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/AlarmTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/AppLauncherTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/BatteryTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/BluetoothTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/CalendarTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/CallTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/CameraTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/ClipboardTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/ContactsTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/FileTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/GoalTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/HttpTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/LocationTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/MediaTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/MemoryTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/NotificationTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/NotifyTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/ScreenshotTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/SmsTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/SystemTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/TimerTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/TorchTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/UrlTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/VolumeTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/WeatherTool.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/tools/impl/WifiTool.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/ui
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/GoalsScreen.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/HomeScreen.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/LogsScreen.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/MemoryScreen.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/OnboardingScreen.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/SettingsScreen.kt
                [D] jarvis-android/app/src/main/kotlin/com/jarvis/ui/theme
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/theme/Color.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/theme/Theme.kt
                  [F] jarvis-android/app/src/main/kotlin/com/jarvis/ui/theme/Type.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/util
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/util/Dispatchers.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/util/Extensions.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/util/Result.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/util/Time.kt
              [D] jarvis-android/app/src/main/kotlin/com/jarvis/voice
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/voice/BargeInController.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/voice/LanguageDetector.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/voice/SpeechRecognizerBridge.kt
                [F] jarvis-android/app/src/main/kotlin/com/jarvis/voice/TextToSpeechBridge.kt
        [D] jarvis-android/app/src/main/python
          [F] jarvis-android/app/src/main/python/brain.py
          [F] jarvis-android/app/src/main/python/executor.py
          [F] jarvis-android/app/src/main/python/language.py
          [F] jarvis-android/app/src/main/python/memory.py
          [F] jarvis-android/app/src/main/python/planner.py
          [D] jarvis-android/app/src/main/python/prompts
            [F] jarvis-android/app/src/main/python/prompts/brief.txt
            [F] jarvis-android/app/src/main/python/prompts/confirm.txt
            [F] jarvis-android/app/src/main/python/prompts/planner.txt
            [F] jarvis-android/app/src/main/python/prompts/reflector.txt
            [F] jarvis-android/app/src/main/python/prompts/system_en.txt
            [F] jarvis-android/app/src/main/python/prompts/system_hi.txt
            [F] jarvis-android/app/src/main/python/prompts/system_hinglish.txt
          [F] jarvis-android/app/src/main/python/reflector.py
          [F] jarvis-android/app/src/main/python/tools_bridge.py
          [F] jarvis-android/app/src/main/python/utils.py
        [D] jarvis-android/app/src/main/res
          [D] jarvis-android/app/src/main/res/drawable
            [F] jarvis-android/app/src/main/res/drawable/ic_jarvis.xml
          [D] jarvis-android/app/src/main/res/mipmap-anydpi-v26
            [F] jarvis-android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
          [D] jarvis-android/app/src/main/res/raw
            [F] jarvis-android/app/src/main/res/raw/.gitkeep
          [D] jarvis-android/app/src/main/res/values
            [F] jarvis-android/app/src/main/res/values/strings.xml
            [F] jarvis-android/app/src/main/res/values/themes.xml
          [D] jarvis-android/app/src/main/res/values-hi
            [F] jarvis-android/app/src/main/res/values-hi/strings.xml
          [D] jarvis-android/app/src/main/res/xml
            [F] jarvis-android/app/src/main/res/xml/backup_rules.xml
            [F] jarvis-android/app/src/main/res/xml/data_extraction_rules.xml
            [F] jarvis-android/app/src/main/res/xml/file_paths.xml
      [D] jarvis-android/app/src/test
        [D] jarvis-android/app/src/test/kotlin
          [D] jarvis-android/app/src/test/kotlin/com
            [D] jarvis-android/app/src/test/kotlin/com/jarvis
              [F] jarvis-android/app/src/test/kotlin/com/jarvis/GoalEngineTest.kt
              [F] jarvis-android/app/src/test/kotlin/com/jarvis/LanguageDetectorTest.kt
              [F] jarvis-android/app/src/test/kotlin/com/jarvis/MemoryStoreTest.kt
              [F] jarvis-android/app/src/test/kotlin/com/jarvis/SecurityPolicyTest.kt
              [F] jarvis-android/app/src/test/kotlin/com/jarvis/ToolDispatcherTest.kt
  [F] jarvis-android/build.gradle.kts
  [F] jarvis-android/config.json
  [D] jarvis-android/gradle
    [D] jarvis-android/gradle/wrapper
      [F] jarvis-android/gradle/wrapper/gradle-wrapper.properties
  [F] jarvis-android/gradle.properties
  [D] jarvis-android/scripts
    [F] jarvis-android/scripts/build_llama.sh
    [F] jarvis-android/scripts/build_whisper.sh
    [F] jarvis-android/scripts/fetch_models.sh
    [F] jarvis-android/scripts/install_debug.sh
    [F] jarvis-android/scripts/sign_release.sh
  [F] jarvis-android/settings.gradle.kts

```

**Tree notes:** 49 directory entries and 148 files are present. No `build/`, `dist/`, `target/`, `.gradle/`, nested archives, symlinks, or executable binaries were found. Hidden entries are the expected `.github`, `.gitignore`, `.gitkeep` items.

## 6. Detailed Findings

|ID|Severity|Confidence|Finding|File|Evidence offset|SHA-256|
|---|---|---|---|---|---|---|
|F-001|High|High|Voice confirmation input has no production call site|app/src/main/kotlin/com/jarvis/tools/ConfirmationManager.kt:61-69|2017|57e3636ec6031937b2119b33fed3ed41cd459872321c8addd02980253195c132|
|F-002|High|High|"PIN required" policy is implemented as voice affirmation, not an authentication factor|app/src/main/kotlin/com/jarvis/tools/ToolDispatcher.kt:58-64|2198|b7169788190a1641815109bf70b8e5187dfe01ee078c12949bce94f15c9a5475|
|F-003|Medium|High|open_url bypasses the centralized network host allowlist|app/src/main/kotlin/com/jarvis/security/SecurityPolicy.kt:63-71|2285|a70ec7c29debaf909d6107b7c232cfe8b81d7a2b50bde26aff50c80c66c1e408|
|F-004|High|Medium|Sensitive data read tools are not confirmation-gated|app/src/main/kotlin/com/jarvis/tools/impl/ClipboardTool.kt:13-22|381|4debb8b7de25e570a35884b291c78f4481efe270d60aab54a6f80c4c67777f90|
|F-005|High|High|Kill switch is not wired to a production control path|app/src/main/kotlin/com/jarvis/security/KillSwitch.kt:10-12|256|5c8009bf2b4dfb9a2842303e9bcfcd60955ffe4289072cf854be1688620225d4|
|F-006|High|High|BOOT_COMPLETED path attempts to start a microphone foreground service|app/src/main/kotlin/com/jarvis/service/BootReceiver.kt:28-40|925|d2ba236167961a9af3c4d2f989b46c6958196c2252ce7241aa9bace5d5110453|
|F-007|High|High|Target/compile SDK is below the current Google Play submission requirement|app/build.gradle.kts:12-16|265|6cc6284ac2ba0ed25630c8343cec313d0a5a0b48870fc91c2f46a585fc099c8a|
|F-008|High|High|Permission surface is substantially broader than the implemented source requires|app/src/main/AndroidManifest.xml:4-54|160|698b3a7eabc7971f898faa2d65a9721ce2e25eed91072a1c701d8f24005d3f5e|
|F-009|Medium|High|Exact-alarm scheduling does not check special access before using setExactAndAllowWhileIdle|app/src/main/kotlin/com/jarvis/autonomy/Scheduler.kt:33-35|1066|01f360fa4ac7a07e6e09718f3aad4690ee23fea427104aec1421708ea6a6a4c4|
|F-010|High|High|Odd-length PCM input can overrun native vector storage|app/src/main/cpp/whisper_jni.cpp:44-46|1526|6cef8b07bc1e391d3633d9bb410e012a8756ebe7e35f061023a5643496bba165|
|F-011|Medium|High|Raw speech transcripts are written to Logcat and persistent log files|app/src/main/kotlin/com/jarvis/service/JarvisForegroundService.kt:93|3610|e0eae34a0bac0f1d0c633929d3ab093a7a47400e009ba175a6211b37464ebcdf|
|F-012|High|High|Shipped CI workflow depends on a missing Gradle wrapper|.github/workflows/ci.yml:22-25|625|dd63fe6cfaaefcd6ea1e97dfe09d36d355c5007caf4d42b1341169615ae655d7|
|F-013|High|High|Default model manifest contains placeholder SHA-256 values|app/src/main/kotlin/com/jarvis/models/ModelManifest.kt:20-48|496|b43cba0d4473b7295742c8839b245c7d9f8b29898241b214d8c3fbd1477cf565|
|F-014|Medium|High|Model fetch script downloads mutable artifacts without checksum verification|scripts/fetch_models.sh:7-21|155|b77bbec2b4a6e7ff29558396d523e6327877990f353d195f9d331ad92205cd19|
|F-015|Medium|High|Native dependency builds clone mutable repository HEADs|scripts/build_llama.sh:4-15|94|3d64825b837aff69460888a2ef4b267b544a437cc5b52bea35b350d337c0ac98|
|F-016|Medium|High|Third-party license notice conflicts with upstream openWakeWord model licensing|app/src/main/assets/licenses/THIRD_PARTY.txt:5|139|5ea3b7a789700f2d1adac74752f6a4e4ca6dc3eae519ada4a95bcd5adbce7d94|
|F-017|Medium|High|Goal creation reports success before asynchronous persistence is confirmed|app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt:48-64|1819|93293adbbaedccc179eca38eefbc52127bda07fbc73d2103e8103b069ec8a54f|
|F-018|Medium|High|Goal resume path ignores maxActiveGoals|app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt:67-78|2390|93293adbbaedccc179eca38eefbc52127bda07fbc73d2103e8103b069ec8a54f|
|F-019|Medium|High|Alarm receiver silently discards all exceptions|app/src/main/kotlin/com/jarvis/autonomy/Scheduler.kt:82-85|3195|01f360fa4ac7a07e6e09718f3aad4690ee23fea427104aec1421708ea6a6a4c4|
|F-020|Medium|High|SpeechRecognizer fallback is a stub despite being advertised as a fallback path|app/src/main/kotlin/com/jarvis/voice/SpeechRecognizerBridge.kt:40-42|1333|d5d65ded658d6972365f08d6810edb97854c26588413e716853c2d90227245c7|
|F-021|Low|Medium|Python bridge initialization errors are swallowed|app/src/main/python/brain.py:42-54|878|c0aab7e84567b589211258d9f8487c8dc3b29a59137b91a07ae8d43605cc8381|
|F-022|Low|High|Release lint is configured not to block release builds|app/build.gradle.kts:56-56|1922|6cc6284ac2ba0ed25630c8343cec313d0a5a0b48870fc91c2f46a585fc099c8a|

### I-001 — No direct malicious payload indicators found in supplied archive

**Severity:** Informational  
**Confidence:** Medium  
**Primary evidence:** `N/A`  
**File SHA-256:** `2bb1b9f01527d4fd78c10591a7fd6470dab6977fe524d156c8862a9f85013aae`  
**Byte offset:** `N/A`  
**Category:** Malware assessment  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
Archive contains no PE/ELF/Mach-O/.NET binaries, no nested archives, no encrypted entries, no high-entropy executable blobs, and static pattern scans found no Runtime.exec/ProcessBuilder/WebView JS bridge/TLS bypass/weak crypto/base64 obfuscation patterns.
```

**Impact:** No direct evidence of malware was identified in the static artifact.  
**Likelihood:** Not assessable without runtime/threat-intel telemetry.  
**Remediation:** Run YARA, ClamAV, and reputation checks in an isolated environment if a higher-assurance malware determination is required. This audit did not upload the archive to third parties.

### F-001 — Voice confirmation input has no production call site

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/tools/ConfirmationManager.kt:61-69`  
**File SHA-256:** `57e3636ec6031937b2119b33fed3ed41cd459872321c8addd02980253195c132`  
**Byte offset:** `2017`  
**Category:** Safety control / authorization  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    /** Feed recognized speech into the active confirmation, if any. */
    fun submit(userText: String) {
        val pending = current.get() ?: return
        if (pending.deferred.isCompleted) return
        val first = userText.trim().lowercase().split(" ", ",", "।").firstOrNull() ?: return
        when {
            first in yes -> pending.deferred.complete(true)
            first in no -> pending.deferred.complete(false)
        }
```

**Impact:** Confirmation-gated operations depend on ConfirmationManager.submit(), but the repository contains no production call site that feeds recognized speech into it. Requests can therefore remain pending until timeout/cancellation rather than being affirmable by the user.  
**Likelihood:** High  
**Remediation:** Connect the voice/STT result path to submit() only while a confirmation request is active; bind the answer to the pending request ID and test yes/no, timeout, cancellation and concurrency paths.

**Static-analysis rationale:** Repository-wide static call-site search found only the submit() declaration; no invocation from the foreground voice loop, BrainBridge, or UI was found.

### F-002 — "PIN required" policy is implemented as voice affirmation, not an authentication factor

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/tools/ToolDispatcher.kt:58-64`  
**File SHA-256:** `b7169788190a1641815109bf70b8e5187dfe01ee078c12949bce94f15c9a5475`  
**Byte offset:** `2198`  
**Category:** Authentication / authorization  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
            needsPin -> {
                // PIN gate: require voice confirmation with PIN-level prompt.
                // Full biometric/PIN UI is a Part-2 extension; fail closed if
                // the user does not affirm the elevated prompt.
                confirmation.ask(pinPrompt(name, args))
            }
            else -> confirmation.ask(confirmPrompt(name, args))
```

**Additional evidence:**

- `SECURITY.md:34` SHA-256 `f7f098febb5d09f90ac34b2caab08fa1b40f79db436c98695200ecf3f66c4dc3`
  ```text
  - Implement biometric/PIN UI for `requiresPin` (currently elevated voice prompt).
  ```

**Impact:** High-risk tools governed by requiresPin() receive the same voice-confirmation mechanism as ordinary confirmations. The source explicitly states the full biometric/PIN UI is a later extension. A configured PIN requirement therefore does not provide a PIN/biometric factor.  
**Likelihood:** Medium  
**Remediation:** Replace the elevated voice prompt with Android BiometricPrompt or a real PIN/key-backed credential flow. Bind approval to the exact request and invalidate it after one use.

### F-003 — open_url bypasses the centralized network host allowlist

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/security/SecurityPolicy.kt:63-71`  
**File SHA-256:** `a70ec7c29debaf909d6107b7c232cfe8b81d7a2b50bde26aff50c80c66c1e408`  
**Byte offset:** `2285`  
**Category:** Network policy  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
        if (!config.current.value.cloudFallbackEnabled && name != "get_weather") {
            // Still allow explicitly allowlisted GETs only when cloud is on;
            // without cloud, deny arbitrary http_get/open_url.
            if (name == "http_get" || name == "open_url" || name == "http_post") {
                // Fail closed for arbitrary network unless cloud is enabled.
                // Exception: open_url still requires confirmation but is user-
                // initiated browser launch — keep confirmation gate only.
                if (name == "open_url") return true
                return config.current.value.cloudFallbackEnabled
```

**Impact:** When cloud fallback is disabled, open_url returns true before isAllowedUrl() is reached. UrlTool then applies only HTTPS plus simple localhost string checks, allowing arbitrary public HTTPS URLs once the confirmation gate is granted.  
**Likelihood:** Medium  
**Remediation:** Route open_url through isAllowedUrl() or create a separate explicit destination allowlist and stricter policy for model-originated URLs. Validate the parsed URI host rather than substring checks.

### F-004 — Sensitive data read tools are not confirmation-gated

**Severity:** High  
**Confidence:** Medium  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/tools/impl/ClipboardTool.kt:13-22`  
**File SHA-256:** `4debb8b7de25e570a35884b291c78f4481efe270d60aab54a6f80c4c67777f90`  
**Byte offset:** `381`  
**Category:** Privacy / authorization  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    override val spec = ToolSpec(
        name = "clipboard_get",
        descriptionHi = "क्लिपबोर्ड पढ़ें",
        descriptionEn = "Read the clipboard",
        parameters = emptyMap()
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString() ?: ""
        return ToolResult.Ok(text)
```

**Additional evidence:**

- `app/src/main/kotlin/com/jarvis/tools/impl/NotificationTool.kt:18-22` SHA-256 `9b72b4e833f18fb342c827538e7f1a4a2e1608eba647d9c6fb0edb019c54ec98`
  ```text
    18:     override suspend fun invoke(args: Map<String, Any?>): ToolResult {
    19:         val limit = (args["limit"] as? Number)?.toInt() ?: 10
    20:         val items = NotificationListener.active().take(limit)
    21:         if (items.isEmpty()) return ToolResult.Ok("no notifications")
    22:         return ToolResult.Ok(items.joinToString("\n") { "${it.pkg}: ${it.title} — ${it.text}" })
  ```
- `app/src/main/kotlin/com/jarvis/tools/impl/ContactsTool.kt:21-37` SHA-256 `058e7447f16f1457ccf19599ccdd06779fd7ae7336aa0723dc2162264d1730a6`
  ```text
    21:     override suspend fun invoke(args: Map<String, Any?>): ToolResult {
    22:         val q = args["query"] as? String ?: return ToolResult.Err("query required", "bad_arg")
    23:         val cr: ContentResolver = ctx.contentResolver
    24:         val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    25:         val proj = arrayOf(
    26:             ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
    27:             ContactsContract.CommonDataKinds.Phone.NUMBER)
    28:         val sel = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR " +
    29:                 "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
    30:         val out = mutableListOf<String>()
    31:         cr.query(uri, proj, sel, arrayOf("%$q%", "%$q%"), null)?.use { c ->
    32:             while (c.moveToNext() && out.size < 5) {
    33:                 out += "${c.getString(0)}: ${c.getString(1)}"
    34:             }
    35:         }
    36:         return if (out.isEmpty()) ToolResult.Err("No match for $q", "not_found")
    37:         else ToolResult.Ok(out.joinToString("; "))
  ```
- `app/src/main/kotlin/com/jarvis/tools/impl/LocationTool.kt:23-31` SHA-256 `24bc62064300c7b24300522fc5060c05a8300c94ccbbcbb30297b8d48d50eec9`
  ```text
    23:     @SuppressLint("MissingPermission")
    24:     override suspend fun invoke(args: Map<String, Any?>): ToolResult {
    25:         val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    26:         val provider = if ((args["highAccuracy"] as? Boolean ?: true))
    27:             LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
    28:         val loc = lm.getLastKnownLocation(provider)
    29:             ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    30:             ?: return ToolResult.Err("No location fix", "no_fix")
    31:         return ToolResult.Ok("lat=${loc.latitude}, lng=${loc.longitude}, acc=${loc.accuracy}m")
  ```
- `app/src/main/kotlin/com/jarvis/tools/impl/CalendarTool.kt:21-36` SHA-256 `c7bbef365ac39af8bcd61d9940d267c6b540c70a65a0fd6c164483b55ba4b9f5`
  ```text
    21:     override suspend fun invoke(args: Map<String, Any?>): ToolResult {
    22:         val days = (args["days"] as? Number)?.toInt() ?: 1
    23:         val now = System.currentTimeMillis()
    24:         val end = now + days * 86_400_000L
    25:         val proj = arrayOf(
    26:             CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)
    27:         val sel = "${CalendarContract.Events.DTSTART} BETWEEN ? AND ?"
    28:         val out = mutableListOf<String>()
    29:         ctx.contentResolver.query(CalendarContract.Events.CONTENT_URI,
    30:             proj, sel, arrayOf(now.toString(), end.toString()),
    31:             "${CalendarContract.Events.DTSTART} ASC")?.use { c ->
    32:             while (c.moveToNext() && out.size < 10) {
    33:                 out += "${c.getString(0)} @ ${c.getLong(1)}"
    34:             }
    35:         }
    36:         return if (out.isEmpty()) ToolResult.Ok("no events") else ToolResult.Ok(out.joinToString("\n"))
  ```
- `app/src/main/python/brain.py:140-156` SHA-256 `c0aab7e84567b589211258d9f8487c8dc3b29a59137b91a07ae8d43605cc8381`
  ```text
   140:     for _ in range(_MAX_TOOL_ROUNDS):
   141:         reply_text = _llm_generate(prompt, max_tokens=700, temperature=0.2)
   142:         if not reply_text.strip():
   143:             break
   144:         calls = _parse_tool_calls(reply_text)
   145:         if not calls:
   146:             # Strip any residual tool fences from final answer
   147:             clean = _TOOL_CALL_RE.sub("", reply_text)
   148:             clean = _TOOL_CALL_RE2.sub("", clean).strip()
   149:             reply_text = clean or reply_text
   150:             break
   151:         # Execute tools and continue
   152:         results = []
   153:         for c in calls:
   154:             r = _run_tool(c["name"], c.get("args") or {})
   155:             tool_trace.append(c["name"])
   156:             results.append({"tool": c["name"], "result": r})
  ```

**Impact:** Clipboard contents can be returned directly. Equivalent sensitive reads exist for notifications, contacts, location and calendar, while the Python brain can autonomously invoke tool calls. These reads are not in the dangerous confirmation set, so the authorization model is mutation-centric rather than data-sensitivity-centric.  
**Likelihood:** Medium  
**Remediation:** Introduce data-classification policies. Require explicit user authorization for clipboard, notifications, contacts, location and calendar reads, minimize fields, and prevent sensitive results from automatically entering long-lived model context.

### F-005 — Kill switch is not wired to a production control path

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/security/KillSwitch.kt:10-12`  
**File SHA-256:** `5c8009bf2b4dfb9a2842303e9bcfcd60955ffe4289072cf854be1688620225d4`  
**Byte offset:** `256`  
**Category:** Safety control  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    val isKilled = MutableStateFlow(false)
    fun kill() { isKilled.value = true }
    fun resume() { isKilled.value = false }
```

**Impact:** KillSwitch exposes kill()/resume(), and the dispatcher honors its in-memory state, but static repository search found no production call sites for either method. The configured kill_phrase_enabled flag also has no observed runtime consumer. The advertised emergency stop is therefore not reachable from the supplied source.  
**Likelihood:** High  
**Remediation:** Wire an explicit UI/voice/hardware emergency control to kill(), persist the killed state, check it before boot/service restart and before every tool call, and require deliberate authenticated resume.

### F-006 — BOOT_COMPLETED path attempts to start a microphone foreground service

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/service/BootReceiver.kt:28-40`  
**File SHA-256:** `d2ba236167961a9af3c4d2f989b46c6958196c2252ce7241aa9bace5d5110453`  
**Byte offset:** `925`  
**Category:** Android lifecycle / platform compliance  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action ?: return
        if (a !in ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val micOk = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (config.current.value.coreEnabled && micOk) {
                    logger.i(TAG, "Boot: starting service ($a)")
                    JarvisForegroundService.start(context)
                } else {
```

**Impact:** BootReceiver launches JarvisForegroundService when coreEnabled and RECORD_AUDIO are true. The service is declared as a microphone foreground service. Android prohibits starting a microphone foreground service from BOOT_COMPLETED for apps targeting the relevant modern API levels.  
**Likelihood:** High  
**Remediation:** Remove automatic microphone-FGS startup from boot. Use an Android-approved voice-assistant mechanism or explicit user interaction/scheduling path, and test cold boot, package replacement and locked-boot behavior.

### F-007 — Target/compile SDK is below the current Google Play submission requirement

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/build.gradle.kts:12-16`  
**File SHA-256:** `6cc6284ac2ba0ed25630c8343cec313d0a5a0b48870fc91c2f46a585fc099c8a`  
**Byte offset:** `265`  
**Category:** Release compliance  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    compileSdk = 34
    defaultConfig {
        applicationId = "com.jarvis"
        minSdk = 31
        targetSdk = 34
```

**Impact:** The supplied source targets Android API 34. Google Play requires new apps and app updates to target API 36 or higher starting August 31, 2026 (with an extension mechanism through November 1, 2026).  
**Likelihood:** High  
**Remediation:** Migrate to API 36, then retest foreground services, exact alarms, permissions, notifications, storage and Android 16 behavior before release.

### F-008 — Permission surface is substantially broader than the implemented source requires

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/src/main/AndroidManifest.xml:4-54`  
**File SHA-256:** `698b3a7eabc7971f898faa2d65a9721ce2e25eed91072a1c701d8f24005d3f5e`  
**Byte offset:** `160`  
**Category:** Permission / privacy  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
    <uses-permission android:name="android.permission.SEND_SMS"/>
    <uses-permission android:name="android.permission.READ_SMS"/>
    <uses-permission android:name="android.permission.RECEIVE_SMS"/>
    <uses-permission android:name="android.permission.CALL_PHONE"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.READ_CALL_LOG"/>
    <uses-permission android:name="android.permission.WRITE_CALL_LOG"/>
    <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS"/>
    <uses-permission android:name="android.permission.READ_CONTACTS"/>
    <uses-permission android:name="android.permission.WRITE_CONTACTS"/>
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-feature android:name="android.hardware.camera.any" android:required="false"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
    <uses-permission android:name="android.permission.READ_CALENDAR"/>
    <uses-permission android:name="android.permission.WRITE_CALENDAR"/>
    <uses-permission android:name="android.permission.SET_ALARM"/>
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
    <uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.FLASHLIGHT"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
        android:usesPermissionFlags="neverForLocation" tools:targetApi="33"/>
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage"/>
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:ignore="QueryAllPackagesPermission"/>
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" tools:ignore="ProtectedPermissions"/>
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" tools:ignore="ProtectedPermissions"/>
```

**Impact:** The manifest declares SMS, call-log, phone, contacts, camera, background location, calendar, exact alarms, all-files storage, broad package visibility, overlay, usage stats, notification-listener and accessibility surfaces. The onboarding screen also requests many high-sensitivity permissions together. Several are not needed by the current FileTool implementation, which is already app-sandboxed.  
**Likelihood:** High  
**Remediation:** Remove unused/restricted permissions. Request permissions just-in-time per feature and redesign broad-access features around narrower Android APIs. Complete Play permission declarations where a restricted permission is genuinely core.

### F-009 — Exact-alarm scheduling does not check special access before using setExactAndAllowWhileIdle

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/autonomy/Scheduler.kt:33-35`  
**File SHA-256:** `01f360fa4ac7a07e6e09718f3aad4690ee23fea427104aec1421708ea6a6a4c4`  
**Byte offset:** `1066`  
**Category:** Platform reliability  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    fun scheduleOnce(atMillis: Long, requestCode: Int, payload: String) {
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, atMillis, pending(requestCode, payload)
```

**Impact:** The code calls setExactAndAllowWhileIdle directly. Android documents that SCHEDULE_EXACT_ALARM is not pre-granted to most newly installed apps targeting Android 13+ and recommends canScheduleExactAlarms() before scheduling.  
**Likelihood:** Medium  
**Remediation:** Check AlarmManager.canScheduleExactAlarms(); route denied cases to Settings or an inexact/WorkManager fallback, and listen for permission-state changes.

### F-010 — Odd-length PCM input can overrun native vector storage

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/src/main/cpp/whisper_jni.cpp:44-46`  
**File SHA-256:** `6cef8b07bc1e391d3633d9bb410e012a8756ebe7e35f061023a5643496bba165`  
**Byte offset:** `1526`  
**Category:** Native memory safety  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    jsize n = env->GetArrayLength(jpcm);
    std::vector<int16_t> pcm(n / 2);
    env->GetByteArrayRegion(jpcm, 0, n, reinterpret_cast<jbyte*>(pcm.data()));
```

**Impact:** The native layer allocates n/2 int16_t elements but copies n bytes into that buffer. For an odd byte count, n bytes exceed 2*(n/2) allocated bytes by one byte, creating a native out-of-bounds write. Even-length PCM input avoids the issue, but the JNI boundary does not enforce even length.  
**Likelihood:** Low  
**Remediation:** Reject odd-length PCM arrays before allocation/copy, or allocate ceil(n/2) with explicit handling. Add native tests/fuzz cases for 0, 1, odd and very large lengths.

### F-011 — Raw speech transcripts are written to Logcat and persistent log files

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/service/JarvisForegroundService.kt:93`  
**File SHA-256:** `e0eae34a0bac0f1d0c633929d3ab093a7a47400e009ba175a6211b37464ebcdf`  
**Byte offset:** `3610`  
**Category:** Privacy / logging  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
                logger.i(TAG, "Heard: $text")
```

**Impact:** The recognized transcript is logged verbatim before being stored in memory. JarvisLogger tees messages to Logcat and a file with 30-day retention. Spoken content may contain personal or confidential information.  
**Likelihood:** Medium  
**Remediation:** Disable raw transcript logging in production, or aggressively redact/minimize content. Separate operational telemetry from user-content logging and make sensitive diagnostics opt-in with short retention.

### F-012 — Shipped CI workflow depends on a missing Gradle wrapper

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `.github/workflows/ci.yml:22-25`  
**File SHA-256:** `dd63fe6cfaaefcd6ea1e97dfe09d36d355c5007caf4d42b1341169615ae655d7`  
**Byte offset:** `625`  
**Category:** Build reproducibility  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
      - name: Gradle unit tests
        run: ./gradlew :app:testDebugUnitTest
      - name: Assemble debug
        run: ./gradlew :app:assembleDebug
```

**Impact:** CI invokes ./gradlew, but the archive contains gradle/wrapper/gradle-wrapper.properties without gradlew, gradlew.bat or gradle-wrapper.jar. A clean checkout of this archive cannot execute the declared CI commands.  
**Likelihood:** High  
**Remediation:** Commit the complete Gradle wrapper and pin the wrapper distribution. Re-run CI from a clean checkout.

### F-013 — Default model manifest contains placeholder SHA-256 values

**Severity:** High  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/models/ModelManifest.kt:20-48`  
**File SHA-256:** `b43cba0d4473b7295742c8839b245c7d9f8b29898241b214d8c3fbd1477cf565`  
**Byte offset:** `496`  
**Category:** Model provisioning / supply chain  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
         * Default catalog. SHA-256 digests must be filled before production use.
         * Empty digests cause ModelDownloader / ModelChecksum to refuse activation
         * (fail-closed). Populate from official release hashes before first run.
         */
        val DEFAULT = ModelManifest(
            listOf(
                ModelEntry(
                    id = "qwen3b",
                    kind = "llm",
                    filename = "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
                    url = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
                    sizeBytes = 2_000_000_000L,
                    sha256 = "PLACEHOLDER_FILL_BEFORE_DOWNLOAD_64_HEX_CHARS_SHA256_________"
                ),
                ModelEntry(
                    id = "whisper-small",
                    kind = "stt",
                    filename = "ggml-small.bin",
                    url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                    sizeBytes = 488_000_000L,
                    sha256 = "PLACEHOLDER_FILL_BEFORE_DOWNLOAD_64_HEX_CHARS_SHA256_________"
                ),
                ModelEntry(
                    id = "hey-jarvis",
                    kind = "wakeword",
                    filename = "hey_jarvis.onnx",
                    url = "https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/hey_jarvis_v0.1.onnx",
                    sizeBytes = 5_000_000L,
                    sha256 = "PLACEHOLDER_FILL_BEFORE_DOWNLOAD_64_HEX_CHARS_SHA256_________"
```

**Impact:** All default model entries contain placeholder digest strings. ModelChecksum correctly rejects blank/malformed hashes, so the default downloader cannot verify or activate these model entries until real SHA-256 values are populated.  
**Likelihood:** High  
**Remediation:** Populate authoritative 64-hex SHA-256 values from trusted release artifacts, preferably via a signed/immutable manifest. Do not allow release builds to ship placeholders.

### F-014 — Model fetch script downloads mutable artifacts without checksum verification

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `scripts/fetch_models.sh:7-21`  
**File SHA-256:** `b77bbec2b4a6e7ff29558396d523e6327877990f353d195f9d331ad92205cd19`  
**Byte offset:** `155`  
**Category:** Supply chain  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
declare -A URLS=(
  ["llm/Qwen2.5-3B-Instruct-Q4_K_M.gguf"]="https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
  ["llm/Qwen2.5-7B-Instruct-Q4_K_M.gguf"]="https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF/resolve/main/qwen2.5-7b-instruct-q4_k_m.gguf"
  ["stt/ggml-small.bin"]="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
  ["stt/ggml-medium.bin"]="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin"
  ["wakeword/hey_jarvis.onnx"]="https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/hey_jarvis_v0.1.onnx"
)

fetch() {
  local rel="$1" url="$2"
  local out="$DEST/$rel"
  [[ -s "$out" ]] && { echo "✓ $rel"; return; }
  echo "→ $rel"
  curl -fL --retry 3 --retry-delay 5 -o "$out.part" "$url"
  mv "$out.part" "$out"
```

**Impact:** The shell script downloads Qwen and Whisper assets from mutable Hugging Face resolve/main paths and moves them into place without checking a SHA-256 or signature. This bypasses the stronger verifier in ModelDownloader when the script is used as a build/setup path.  
**Likelihood:** Medium  
**Remediation:** Pin immutable revisions, publish hashes/signatures, verify before rename, and make the build fail on verification failure.

### F-015 — Native dependency builds clone mutable repository HEADs

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `scripts/build_llama.sh:4-15`  
**File SHA-256:** `3d64825b837aff69460888a2ef4b267b544a437cc5b52bea35b350d337c0ac98`  
**Byte offset:** `94`  
**Category:** Supply chain  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
TP="$ROOT/app/src/main/cpp/third_party"
mkdir -p "$TP"; cd "$TP"
[[ -d llama.cpp ]] || git clone --depth=1 https://github.com/ggerganov/llama.cpp.git
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
cd llama.cpp
cmake -B build-android-arm64 \
  -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-31 \
  -DBUILD_SHARED_LIBS=ON -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF \
  -DLLAMA_BUILD_SERVER=OFF -DLLAMA_CURL=OFF -DGGML_LLAMAFILE=ON -DGGML_OPENMP=OFF \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build-android-arm64 --config Release -j"$(nproc)"
```

**Additional evidence:**

- `scripts/build_whisper.sh:6` SHA-256 `ed7d0c1a6e9d421a745d3239a4c45567f47c4fc904f314cf546e80d8a17c964f`
  ```text
  [[ -d whisper.cpp ]] || git clone --depth=1 https://github.com/ggerganov/whisper.cpp.git
  ```

**Impact:** llama.cpp and whisper.cpp are cloned with depth=1 and no commit/tag/hash pinning. A clean build can therefore produce different native code over time, weakening reproducibility and supply-chain review.  
**Likelihood:** Medium  
**Remediation:** Pin exact commit IDs, record upstream SHAs, verify fetched content, and use a lock/manifest for third-party source revisions.

### F-016 — Third-party license notice conflicts with upstream openWakeWord model licensing

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/assets/licenses/THIRD_PARTY.txt:5`  
**File SHA-256:** `5ea3b7a789700f2d1adac74752f6a4e4ca6dc3eae519ada4a95bcd5adbce7d94`  
**Byte offset:** `139`  
**Category:** Licensing / IP  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
openWakeWord                       — Apache License 2.0
```

**Additional evidence:**

- `PRODUCTION.md:46` SHA-256 `308c6cd9e236760be6a1e44426d2d9f29bfd8ba7ce84613198dbb9111f5389d4`
  ```text
  - openWakeWord pretrained models may be CC BY-NC-SA — check before commercial use
  ```

**Impact:** The local notice states openWakeWord is Apache-2.0 without distinguishing code from pretrained models. Upstream openWakeWord states its code is Apache-2.0 while included pretrained models are CC BY-NC-SA 4.0. The repository itself also warns in PRODUCTION.md that openWakeWord pretrained models may be CC BY-NC-SA.  
**Likelihood:** High  
**Remediation:** Separate code and model license notices, preserve the model-specific license/attribution, and do not ship the pretrained model in a commercial product without confirming compatible licensing.

### F-017 — Goal creation reports success before asynchronous persistence is confirmed

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt:48-64`  
**File SHA-256:** `93293adbbaedccc179eca38eefbc52127bda07fbc73d2103e8103b069ec8a54f`  
**Byte offset:** `1819`  
**Category:** State consistency  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
        scope.launch {
            createMutex.withLock {
                val running = memory.runningGoals().size
                if (running >= max) {
                    logger.w(TAG, "maxActiveGoals=$max reached at persist time")
                    return@launch
                }
                memory.putGoal(goal)
                activeCount.incrementAndGet()
            }
            try {
                run(goal)
            } finally {
                activeCount.updateAndGet { (it - 1).coerceAtLeast(0) }
            }
        }
        return id
```

**Impact:** create() launches persistence asynchronously and immediately returns a non-empty UUID. If the in-lock running-goal check rejects the request at persist time, the caller still receives the ID and GoalTool reports the goal as created even though it was not stored.  
**Likelihood:** Medium  
**Remediation:** Make creation suspend/transactional or return a result that distinguishes accepted from rejected persistence. Only report success after the record is durably stored.

### F-018 — Goal resume path ignores maxActiveGoals

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/autonomy/GoalEngine.kt:67-78`  
**File SHA-256:** `93293adbbaedccc179eca38eefbc52127bda07fbc73d2103e8103b069ec8a54f`  
**Byte offset:** `2390`  
**Category:** Resource governance  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    suspend fun resumePersisted() {
        for (g in memory.runningGoals()) {
            logger.i(TAG, "Resuming goal ${g.id}")
            activeCount.incrementAndGet()
            scope.launch {
                try {
                    run(g)
                } finally {
                    activeCount.updateAndGet { (it - 1).coerceAtLeast(0) }
                }
            }
        }
```

**Impact:** resumePersisted() increments activeCount and launches every persisted running goal without applying maxActiveGoals. A database containing more running goals than configured can therefore restore more concurrent executions than the configured cap.  
**Likelihood:** Medium  
**Remediation:** Apply the same concurrency gate during resume, define a deterministic excess-goal policy, and make active-goal accounting transactional.

### F-019 — Alarm receiver silently discards all exceptions

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/autonomy/Scheduler.kt:82-85`  
**File SHA-256:** `01f360fa4ac7a07e6e09718f3aad4690ee23fea427104aec1421708ea6a6a4c4`  
**Byte offset:** `3195`  
**Category:** Observability / reliability  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
                } catch (t: Throwable) {
                    // Avoid crashing the process from a broadcast.
                } finally {
                    pending.finish()
```

**Impact:** The AlarmReceiver catches Throwable and does nothing. Permission failures, malformed payloads, dependency failures and other execution errors therefore disappear without an audit/log record, making automation failures difficult to detect and investigate.  
**Likelihood:** Medium  
**Remediation:** Log structured failure details (without sensitive payloads), increment a failure metric, and distinguish expected permission failures from unexpected faults. Never catch Throwable silently.

### F-020 — SpeechRecognizer fallback is a stub despite being advertised as a fallback path

**Severity:** Medium  
**Confidence:** High  
**Primary evidence:** `app/src/main/kotlin/com/jarvis/voice/SpeechRecognizerBridge.kt:40-42`  
**File SHA-256:** `d5d65ded658d6972365f08d6810edb97854c26588413e716853c2d90227245c7`  
**Byte offset:** `1333`  
**Category:** Functionality gap  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    private fun fallback(pcm: ByteArray): String {
        logger.w(TAG, "STT fallback invoked for ${pcm.size} bytes"); return ""
    }
```

**Impact:** The source path documented as an Android SpeechRecognizer fallback currently returns an empty string after logging; it does not instantiate or invoke the Android SpeechRecognizer API. Offline/alternate STT recovery therefore is not implemented as described.  
**Likelihood:** Medium  
**Remediation:** Implement the fallback with explicit lifecycle/permission handling or remove the fallback claim and surface the unavailable capability to the user.

### F-021 — Python bridge initialization errors are swallowed

**Severity:** Low  
**Confidence:** Medium  
**Primary evidence:** `app/src/main/python/brain.py:42-54`  
**File SHA-256:** `c0aab7e84567b589211258d9f8487c8dc3b29a59137b91a07ae8d43605cc8381`  
**Byte offset:** `878`  
**Category:** Error handling  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
def install_tool_dispatcher(bridge: Any) -> None:
    """Kotlin passes ToolBridgeKotlin instance; has .invoke(name, args_json)."""
    global _tool_bridge
    _tool_bridge = bridge
    try:
        import tools_bridge as tb

        def _dispatch(name: str, args_json: str) -> str:
            return str(bridge.invoke(name, args_json))

        tb.set_dispatcher(_dispatch)
    except Exception:
        pass
```

**Impact:** install_tool_dispatcher() catches all exceptions and executes pass. A failed Kotlin/Python dispatcher binding can leave the AI brain without tools while reporting no explicit initialization failure from this function.  
**Likelihood:** Medium  
**Remediation:** Log a redacted structured failure and expose an explicit degraded-health state to Kotlin/UI. Do not silently convert security-critical initialization errors into a nominal running state.

### F-022 — Release lint is configured not to block release builds

**Severity:** Low  
**Confidence:** High  
**Primary evidence:** `app/build.gradle.kts:56-56`  
**File SHA-256:** `6cc6284ac2ba0ed25630c8343cec313d0a5a0b48870fc91c2f46a585fc099c8a`  
**Byte offset:** `1922`  
**Category:** QA / secure build  
**MITRE ATT&CK:** No unambiguous ATT&CK mapping asserted.

**Raw evidence:**

```text
    lint { abortOnError = false; checkReleaseBuilds = false }
```

**Impact:** abortOnError=false and checkReleaseBuilds=false allow release builds to proceed despite lint findings. Security-relevant Android lint diagnostics can therefore be informational only.  
**Likelihood:** High  
**Remediation:** Enable release lint enforcement and baseline only known/accepted findings with review ownership and expiry.

## 7. Security Assessment

### Malware / malicious payload assessment

**Result: no direct malicious payload indicators found; confidence MEDIUM.** The archive contains no PE/ELF/Mach-O/.NET binaries, no native shared libraries, no nested archives, no encrypted containers, no high-entropy executable blobs, and no obvious obfuscation. Static scans found no process-execution APIs (`Runtime.exec`, `ProcessBuilder`), WebView JavaScript bridges, obvious TLS validation bypasses, weak crypto primitives, or base64-heavy payload obfuscation. Boot receiver and foreground-service behavior is present but aligns with the declared assistant functionality, so it is not classified as malicious persistence by itself. YARA/ClamAV/capa/DIE/floss/binwalk were unavailable, and no external hash-reputation service was used because third-party upload/reputation lookup was not authorized.

### Injection / command execution

No direct shell/process-execution code was found in the 148 file payloads. Database queries observed in Contacts/Calendar use selection arguments/placeholders rather than string-concatenating user input into SQL execution APIs.

### Network

HTTP GET is explicitly allowlisted and redirects are disabled in `HttpTool`. `SecurityPolicy` is intended to be default-deny, but `open_url` is intentionally returned as allowed in the cloud-disabled branch and then only receives scheme/local-host checks. This is the principal network-policy exception.

### Secrets

No real private keys, cloud access keys, obvious JWT secrets, API credentials, database passwords, or hardcoded tokens were identified. `scripts/sign_release.sh` uses environment variables (`JARVIS_KEYSTORE`, `JARVIS_KEY_ALIAS`, `JARVIS_STORE_PASS`, `JARVIS_KEY_PASS`) rather than embedding credential values. No secret was therefore redacted in the evidence table.

### Archive anomaly assessment

|Check|Result|
|---|---|
|Path traversal / Zip Slip|PASS: none detected|
|Absolute / drive / UNC / NUL paths|PASS: none detected|
|Unicode RLO / zero-width filenames|PASS: none detected|
|Duplicate / case-collision names|PASS: none detected|
|Symlink/hardlink attacks|PASS: no symlinks; no hardlink metadata observed|
|Overlapping archive entries|PASS|
|CRC/header mismatch|PASS|
|Compression bomb|PASS; overall 2.47:1, max per-file 4.70:1|
|Encryption|None|
|Nested archives|None|
|Future/pre-1980 timestamps|None detected|
|Suspicious extra fields|Only EXTENDED_TIMESTAMP (0x5455) and 0x7875|
|Executable binaries|None present|

## 8. IOC List

### Human-readable IOC / network artifact inventory

|Type|Value|Assessment|
|---|---|---|
|URL|https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,weather_code|Declared weather API; expected|
|URL|https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf|Model source; mutable path|
|URL|https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF/resolve/main/qwen2.5-7b-instruct-q4_k_m.gguf|Model source; mutable path|
|URL|https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin|Model source; mutable path|
|URL|https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin|Model source; mutable path|
|URL|https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/hey_jarvis_v0.1.onnx|Wake-word model release asset|
|URL|https://github.com/ggerganov/llama.cpp.git|Native source repository; mutable HEAD|
|URL|https://github.com/ggerganov/whisper.cpp.git|Native source repository; mutable HEAD|
|URL|https://chaquo.com/maven|Build repository|
|URL|https://jitpack.io|Build repository|
|IP|127.0.0.1|Loopback literal; blocked by UrlTool/SecurityPolicy string checks|
|Email|None found|No email IOC|
|Wallet|None found|No crypto wallet IOC|
|C2|None identified|No suspicious C2 infrastructure identified statically|

### Machine-readable IOC hash set

The JSON companion contains the archive MD5/SHA-1/SHA-256 plus SHA-256 values for every extracted file under `iocs.hashes`; the full per-file inventory remains the authoritative mapping from path to hash.

## 9. MITRE ATT&CK Mapping

No high-confidence malicious ATT&CK technique was identified. Benign Android capabilities such as boot receivers, notification access, microphone capture, package visibility, and model downloading are part of the stated product functionality and are not automatically classified as ATT&CK techniques without evidence of adversarial use. Findings involving authorization, path policy, or native memory safety therefore have no direct ATT&CK ID asserted.

## 10. Risk Matrix

### High (10)
|ID|Confidence|Finding|
|---|---|---|
|F-001|High|Voice confirmation input has no production call site|
|F-002|High|"PIN required" policy is implemented as voice affirmation, not an authentication factor|
|F-004|Medium|Sensitive data read tools are not confirmation-gated|
|F-005|High|Kill switch is not wired to a production control path|
|F-006|High|BOOT_COMPLETED path attempts to start a microphone foreground service|
|F-007|High|Target/compile SDK is below the current Google Play submission requirement|
|F-008|High|Permission surface is substantially broader than the implemented source requires|
|F-010|High|Odd-length PCM input can overrun native vector storage|
|F-012|High|Shipped CI workflow depends on a missing Gradle wrapper|
|F-013|High|Default model manifest contains placeholder SHA-256 values|

### Medium (10)
|ID|Confidence|Finding|
|---|---|---|
|F-003|High|open_url bypasses the centralized network host allowlist|
|F-009|High|Exact-alarm scheduling does not check special access before using setExactAndAllowWhileIdle|
|F-011|High|Raw speech transcripts are written to Logcat and persistent log files|
|F-014|High|Model fetch script downloads mutable artifacts without checksum verification|
|F-015|High|Native dependency builds clone mutable repository HEADs|
|F-016|High|Third-party license notice conflicts with upstream openWakeWord model licensing|
|F-017|High|Goal creation reports success before asynchronous persistence is confirmed|
|F-018|High|Goal resume path ignores maxActiveGoals|
|F-019|High|Alarm receiver silently discards all exceptions|
|F-020|High|SpeechRecognizer fallback is a stub despite being advertised as a fallback path|

### Low (2)
|ID|Confidence|Finding|
|---|---|---|
|F-021|Medium|Python bridge initialization errors are swallowed|
|F-022|High|Release lint is configured not to block release builds|

### Informational (1)
|ID|Confidence|Finding|
|---|---|---|
|I-001|Medium|No direct malicious payload indicators found in supplied archive|

## 11. Recommendations

### Immediate

1. Make authorization truly end-to-end: wire confirmation speech, implement real PIN/biometric authentication for high-risk actions, and persist/wire an emergency kill switch.
2. Change the data-access model so clipboard, notifications, contacts, location and calendar reads are explicitly classified and authorized instead of being implicitly trusted by the LLM.
3. Fix the native PCM boundary and the `open_url` allowlist bypass before any real-world autonomous operation.
4. Remove unnecessary restricted permissions and stop requesting broad sensitive permissions in one onboarding transaction.

### Short-term

1. Raise target/compile SDK to API 36 and redesign the microphone foreground-service boot/lifecycle strategy for current Android restrictions.
2. Commit the complete Gradle wrapper, pin all native repositories to exact commits, pin model sources and publish verified digests/signatures.
3. Make CI security-aware: release lint gates, dependency/SCA scan, secret scanning, SBOM generation, license checking, and clean-checkout reproducibility.
4. Remove raw transcript persistence or apply production redaction/minimization and materially shorter retention.

### Long-term

1. Establish an explicit capability-security model: every tool gets a data-sensitivity class, mutation class, required user presence, authentication level and network scope.
2. Add end-to-end tests covering voice → STT → brain → structured tool call → policy → confirmation/PIN → tool → audit → response.
3. Replace “source distribution considered production complete” with a release gate based on reproducible builds, verified native/model artifacts, Android compatibility tests and Play-policy evidence.

## 12. Appendix A — Commands & Raw Outputs

### Core commands

```bash
cp -p /mnt/data/jarvis_forensic_work_final2/evidence.zip /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
chmod 0444 /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
sha256sum /mnt/data/jarvis_forensic_work_final2/evidence.zip /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
sha1sum /mnt/data/jarvis_forensic_work_final2/evidence.zip /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
md5sum /mnt/data/jarvis_forensic_work_final2/evidence.zip /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
unzip -t /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
zipinfo -v /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
file -b /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
find /mnt/data/jarvis_forensic_work_final2/work/extracted -type f -print
strings --version
file --version
for c in 7z binwalk exiftool clamscan yara diec capa floss trid; do command -v "$c"; done
```

### Raw command log

The exact tool-version/hash/archive-validation output captured during this run is preserved verbatim in the companion `commands_final.txt` artifact and is included in the report bundle.

```text
$ date -u +%Y-%m-%dT%H:%M:%S.%NZ
2026-09-29T00:19:53.916042954Z
$ pwd
/
$ stat
path=/mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
size=140404
mode=444
mtime=2026-09-29 00:10:31.603943023 +0000
$ sha256sum
2bb1b9f01527d4fd78c10591a7fd6470dab6977fe524d156c8862a9f85013aae  /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
$ sha1sum
42c292c2d84aefd6b28dd97f46bb8f84fc6a1b36  /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
$ md5sum
b45f88631d3e9aa6101940133713f09e  /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip
$ python3 CRC32
e8b029ee
$ file
Zip archive data, made by v3.0 UNIX, extract using at least v1.0, last modified Sep 28 2026 21:14:20, uncompressed size 0, method=store
$ unzip -t
    testing: jarvis-android/scripts/sign_release.sh   OK
    testing: jarvis-android/settings.gradle.kts   OK
No errors detected in compressed data of /mnt/data/jarvis_forensic_work_final2/work/evidence.original.zip.
$ unzip -v | head
UnZip 6.00 of 20 April 2009, by Debian. Original by Info-ZIP.

Latest sources and executables are at ftp://ftp.info-zip.org/pub/infozip/ ;
see ftp://ftp.info-zip.org/pub/infozip/UnZip.html for other sites.

Compiled with gcc 14.2.0 for Unix (Linux ELF).

UnZip special compilation options:
$ zipinfo -h
ZipInfo 3.00 of 20 April 2009, by Greg Roelofs and the Info-ZIP group.

List name, date/time, attribute, size, compression method, etc., about files
in list (excluding those in xlist) contained in the specified .zip archive(s).
"file[.zip]" may be a wildcard name containing *, ?, [] (e.g., "[a-j]*.zip").

   usage:  zipinfo [-12smlvChMtTz] file[.zip] [list...] [-x xlist...]
      or:  unzip -Z [-12smlvChMtTz] file[.zip] [list...] [-x xlist...]

main listing-format options:             -s  short Unix "ls -l" format (def.)
  -1  filenames ONLY, one per line       -m  medium Unix "ls -l" format
  -2  just filenames but allow -h/-t/-z  -l  long Unix "ls -l" format
$ file --version
file-5.46
magic file from /etc/magic:/usr/share/misc/magic
$ strings --version
GNU strings (GNU Binutils for Debian) 2.44
Copyright (C) 2025 Free Software Foundation, Inc.
$ tool availability
7z         NOT_FOUND
binwalk    NOT_FOUND
exiftool   NOT_FOUND
clamscan   NOT_FOUND
yara       NOT_FOUND
diec       NOT_FOUND
capa       NOT_FOUND
floss      NOT_FOUND
trid       NOT_FOUND

```

## 13. Appendix B — Skipped / Unprocessed Items

|Item|Status|Reason|
|---|---|---|
|Phase 11 sandbox execution|SKIPPED|Execution was not explicitly authorized.|
|PE/ELF/Mach-O/.NET deep executable analysis|NOT APPLICABLE|No executable binaries present in the archive.|
|Office/PDF/RTF macro analysis|NOT APPLICABLE|No such documents present.|
|Image/audio/video EXIF/stego analysis|NOT APPLICABLE|No image/audio/video payloads present.|
|Nested archive recursion|NOT APPLICABLE|No nested archives present.|
|YARA|UNAVAILABLE|Binary not installed and Python YARA module absent.|
|ClamAV|UNAVAILABLE|clamscan not installed.|
|capa / DIE / FLOSS / binwalk / exiftool / trid|UNAVAILABLE|Tools not installed.|
|Full Gradle compile / Android dynamic tests|NOT EXECUTED|Would execute/compile project dependencies; wrapper and native third-party sources are also absent.|
|External reputation / threat-intel lookup|NOT PERFORMED|No third-party upload/lookup was authorized.|
|Transitive dependency CVE enumeration|PARTIAL|Direct versions were parsed from Gradle manifests, but no complete resolved dependency graph exists in the archive.|

## 14. Appendix C — Completion Checklist

|Checklist item|Status|
|---|---|
|Chain of custody recorded|PASS|
|SHA-256/SHA-1/MD5/CRC32 computed|PASS|
|ZIP integrity / EOCD checked|PASS|
|Archive structure / metadata analyzed|PASS|
|Encryption handled|PASS: none present|
|Safe extraction completed|PASS|
|All file payloads inventoried and hashed|PASS|
|True type/MIME/entropy checked|PASS|
|Deep static code/config review|PASS|
|Nested archive recursion|PASS: none present|
|IOCs extracted|PASS|
|Risk assessment|PASS|
|MITRE mapping|PASS: no unambiguous malicious mappings|
|Markdown report generated|PASS|
|JSON summary generated|PASS|
|Sandbox execution|SKIPPED: not authorized|
|YARA/ClamAV/capa/DIE/etc.|SKIPPED/UNAVAILABLE|

## 15. Evidence References

- Archive SHA-256: `2bb1b9f01527d4fd78c10591a7fd6470dab6977fe524d156c8862a9f85013aae`
- Analysis copy SHA-256: `2bb1b9f01527d4fd78c10591a7fd6470dab6977fe524d156c8862a9f85013aae` (identical)
- Full per-file inventory: `file_inventory.csv`
- Raw tree: `directory_tree.txt`
- Command log: `commands_final.txt`

### External technical sources consulted (current as of 2026-09-29)

- Google Play Target API policy: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
- Android foreground-service timeouts: https://developer.android.com/develop/background-work/services/fgs/timeout
- Android 15 behavior changes / BOOT_COMPLETED FGS restrictions: https://developer.android.com/about/versions/15/behavior-changes-15
- Android exact alarms: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- Android Play All Files policy: https://support.google.com/googleplay/android-developer/answer/10467955?hl=en
- Android Play broad package visibility: https://support.google.com/googleplay/android-developer/answer/10158779?hl=en
- Android Play SMS / Call Log policy: https://support.google.com/googleplay/android-developer/answer/10208820?hl=en
- openWakeWord repository/license statement: https://github.com/dscripka/openWakeWord
