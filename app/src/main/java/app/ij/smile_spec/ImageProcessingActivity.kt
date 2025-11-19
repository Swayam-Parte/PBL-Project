package app.ij.smile_spec

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.ij.smile_spec.databinding.ActivityImageProcessingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.onnxruntime.OrtException
import java.io.IOException

/**
 * ImageProcessingActivity - Shows image preview and loading animation
 * Processes image with ONNX model and navigates to result screen
 */
class ImageProcessingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageProcessingBinding
    private lateinit var modelInference: OnnxModelInference
    private var selectedBitmap: Bitmap? = null

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val TAG = "ImageProcessing"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize model
        modelInference = OnnxModelInference()

        // Setup animations
        setupAnimations()

        // Load image and start processing
        loadImageAndProcess()
    }

    /**
     * Setup entrance animations
     */
    private fun setupAnimations() {
        // Fade in image card
        binding.cardImagePreview.alpha = 0f
        binding.cardImagePreview.scaleX = 0.9f
        binding.cardImagePreview.scaleY = 0.9f
        binding.cardImagePreview.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .start()

        // Fade in processing card
        binding.cardProcessing.alpha = 0f
        binding.cardProcessing.translationY = 50f
        binding.cardProcessing.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(200)
            .start()
    }

    /**
     * Load image from URI and start processing
     */
    private fun loadImageAndProcess() {
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString == null) {
            showError(getString(R.string.error_no_image))
            return
        }

        val imageUri = Uri.parse(imageUriString)
        
        lifecycleScope.launch {
            try {
                // Load bitmap
                selectedBitmap = loadBitmapFromUri(imageUri)
                
                if (selectedBitmap == null) {
                    Log.e(TAG, "Failed to load bitmap from URI: $imageUriString")
                    showError(getString(R.string.error_processing_image))
                    return@launch
                }

                Log.d(TAG, "Bitmap loaded successfully: ${selectedBitmap!!.width}x${selectedBitmap!!.height}")

                // Display image preview
                binding.ivPreview.setImageBitmap(selectedBitmap)

                // Load model in background
                try {
                    withContext(Dispatchers.IO) {
                        modelInference.loadModel(this@ImageProcessingActivity)
                    }
                    Log.d(TAG, "Model loaded successfully")
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to load model: file I/O error", e)
                    showError(getString(R.string.error_loading_model))
                    return@launch
                } catch (e: OrtException) {
                    Log.e(TAG, "Failed to load model: ONNX Runtime error", e)
                    showError(getString(R.string.error_loading_model))
                    return@launch
                }

                // Add small delay for better UX (show loading animation)
                delay(1500)

                // Process image
                val result = withContext(Dispatchers.IO) {
                    modelInference.predictShade(selectedBitmap!!)
                }

                Log.d(TAG, "Prediction successful: ${result.predictedClass} (${result.confidence}%)")

                // Navigate to result screen
                navigateToResult(result)

            } catch (e: OrtException) {
                Log.e(TAG, "ONNX inference error during prediction", e)
                showError(getString(R.string.error_processing_image))
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid bitmap or argument", e)
                showError(getString(R.string.error_processing_image))
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Illegal state during processing", e)
                showError(getString(R.string.error_processing_image))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error processing image", e)
                showError(getString(R.string.error_processing_image))
            }
        }
    }

    /**
     * Load bitmap from URI
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading bitmap", e)
            null
        }
    }

    /**
     * Navigate to result activity with smooth transition
     */
    private fun navigateToResult(result: OnnxModelInference.PredictionResult) {
        // Fade out animation before navigation
        binding.cardImagePreview.animate()
            .alpha(0f)
            .setDuration(300)
            .start()

        binding.cardProcessing.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_PREDICTED_CLASS, result.predictedClass)
                    putExtra(ResultActivity.EXTRA_CONFIDENCE, result.confidence)
                    
                    // Pass all probabilities
                    val probA = result.getProbabilityForClass("A")
                    val probB = result.getProbabilityForClass("B")
                    val probC = result.getProbabilityForClass("C")
                    val probD = result.getProbabilityForClass("D")
                    
                    putExtra(ResultActivity.EXTRA_PROB_A, probA)
                    putExtra(ResultActivity.EXTRA_PROB_B, probB)
                    putExtra(ResultActivity.EXTRA_PROB_C, probC)
                    putExtra(ResultActivity.EXTRA_PROB_D, probD)
                }
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .start()
    }

    /**
     * Show error message and return to main activity
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            delay(2000)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedBitmap?.recycle()
        modelInference.close()
    }
}


