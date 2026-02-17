package com.hunter.system.core.classification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * "Bed made" classifier using Gemini Nano on-device via ML Kit GenAI Prompt API.
 *
 * Sends the captured bed photo + a strict TRUE/FALSE prompt to Gemini Nano running entirely
 * on the device. No internet, no API key, no cloud calls. Requires a device that supports
 * AICore (Pixel 8+, Galaxy S24+, etc.) and Android 14+.
 */
@Singleton
class BedMadeChecker @Inject constructor(@ApplicationContext private val context: Context) {

  companion object {
    private const val TAG = "BedMadeChecker"

    private const val PROMPT_TEXT =
      "Does this photo show a bed that has been made? " +
      "A made bed means the blanket or cover is pulled up over the mattress " +
      "and is not bunched up or thrown aside. It does not need to look perfect. " +
      "Output FALSE if there is no bed in the photo, or if the bed is clearly unmade " +
      "with sheets tangled or mattress exposed. " +
      "Output ONLY the single word TRUE or FALSE."
  }

  private val model: GenerativeModel by lazy { Generation.getClient() }
  private val mutex = Mutex()
  private var modelReady = false

  /**
   * Analyze the photo at [imageUri] and determine if the bed is made.
   * @return true if Gemini Nano judges the bed as made, false otherwise.
   */
  suspend fun isBedMade(imageUri: Uri): Boolean = withContext(Dispatchers.Default) {
    try {
      ensureModelReady()

      val bitmap = loadBitmap(imageUri)

      val request = generateContentRequest(ImagePart(bitmap), TextPart(PROMPT_TEXT)) {
        temperature = 0.0f
        topK = 1
        maxOutputTokens = 5
      }

      val response = model.generateContent(request)
      val answer = response.candidates.firstOrNull()?.text?.trim()?.lowercase() ?: ""

      Log.i(TAG, "Gemini Nano response: \"$answer\" → bedMade=${answer.contains("true")}")
      answer.contains("true")
    } catch (e: Exception) {
      Log.e(TAG, "Gemini Nano classification failed", e)
      false
    }
  }

  private suspend fun ensureModelReady() = mutex.withLock {
    if (modelReady) return@withLock

    when (val status = model.checkStatus()) {
      FeatureStatus.AVAILABLE -> {
        Log.i(TAG, "Gemini Nano is available")
        modelReady = true
      }
      FeatureStatus.DOWNLOADABLE -> {
        Log.i(TAG, "Gemini Nano downloadable — triggering download")
        model.download().collect { dlStatus ->
          Log.d(TAG, "Download status: $dlStatus")
        }
        modelReady = true
      }
      FeatureStatus.DOWNLOADING -> {
        Log.i(TAG, "Gemini Nano is currently downloading — waiting")
        model.download().collect { dlStatus ->
          Log.d(TAG, "Download status: $dlStatus")
        }
        modelReady = true
      }
      else -> {
        throw UnsupportedOperationException(
          "Gemini Nano is unavailable on this device (status=$status)"
        )
      }
    }
  }

  private fun loadBitmap(uri: Uri): Bitmap {
    val input = context.contentResolver.openInputStream(uri)
      ?: throw IllegalArgumentException("Cannot open URI: $uri")
    val original = BitmapFactory.decodeStream(input)
    input.close()

    val maxDim = 512
    if (original.width <= maxDim && original.height <= maxDim) return original

    val scale = maxDim.toFloat() / maxOf(original.width, original.height)
    val scaled = Bitmap.createScaledBitmap(
      original,
      (original.width * scale).toInt(),
      (original.height * scale).toInt(),
      true
    )
    if (scaled !== original) original.recycle()
    return scaled
  }
}
