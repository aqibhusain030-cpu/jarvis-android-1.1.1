package com.jarvis.models

import kotlinx.serialization.Serializable

/**
 * Portable vision deployment contract.
 *
 * Inspired by explicit Core ML input/output typing, but intentionally independent
 * of Apple's runtime so the same exported model can target Android ONNX Runtime.
 */
@Serializable
data class VisionModelContract(
    val id: String,
    val task: VisionTask,
    val backend: String = "onnxruntime",
    val inputName: String,
    val outputName: String,
    val inputLayout: TensorLayout,
    val inputDtype: TensorDtype,
    val inputShape: List<Long>,
    val outputDtype: TensorDtype,
    val preprocessing: PreprocessingContract,
    val postprocessing: PostprocessingContract = PostprocessingContract.IDENTITY,
    val precision: String = "fp16",
    val compression: String = "none",
    val sourceRevision: String,
    val sha256: String,
    val enabled: Boolean = false
)

@Serializable
enum class VisionTask {
    DEPTH_ESTIMATION,
    IMAGE_CLASSIFICATION,
    OBJECT_DETECTION,
    EMBEDDING
}

@Serializable
enum class TensorLayout {
    NCHW,
    NHWC
}

@Serializable
enum class TensorDtype {
    FLOAT32,
    FLOAT16,
    INT8
}

@Serializable
data class PreprocessingContract(
    val resizeWidth: Int,
    val resizeHeight: Int,
    val mean: List<Float>,
    val std: List<Float>,
    val colorOrder: String = "RGB",
    val scale: Float = 1f / 255f
)

@Serializable
data class PostprocessingContract(
    val outputRange: String? = null,
    val resizeToInput: Boolean = true
) {
    companion object {
        val IDENTITY = PostprocessingContract()
    }
}

/**
 * Depth Anything V2 deployment profile. The actual checkpoint/ONNX SHA must be
 * populated after exporting and benchmarking the selected model on the target.
 *
 * The paper provides model scales from 25M to 1.3B parameters; JARVIS keeps the
 * artifact choice separate from the runtime contract so a verified mobile-sized
 * export can be selected later without changing the application architecture.
 */
object DepthAnythingV2Contract {
    const val DEFAULT_ID = "depth-anything-v2-small"
    const val INPUT_WIDTH = 518
    const val INPUT_HEIGHT = 518

    fun unverified(): VisionModelContract = VisionModelContract(
        id = DEFAULT_ID,
        task = VisionTask.DEPTH_ESTIMATION,
        inputName = "pixel_values",
        outputName = "predicted_depth",
        inputLayout = TensorLayout.NCHW,
        inputDtype = TensorDtype.FLOAT32,
        inputShape = listOf(1, 3, INPUT_HEIGHT.toLong(), INPUT_WIDTH.toLong()),
        outputDtype = TensorDtype.FLOAT32,
        preprocessing = PreprocessingContract(
            resizeWidth = INPUT_WIDTH,
            resizeHeight = INPUT_HEIGHT,
            mean = listOf(0.485f, 0.456f, 0.406f),
            std = listOf(0.229f, 0.224f, 0.225f)
        ),
        precision = "fp16",
        compression = "none",
        sourceRevision = "DEPTH_ANYTHING_V2_EXPORT_PENDING",
        sha256 = "",
        enabled = false
    )
}
