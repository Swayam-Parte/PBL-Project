package app.ij.smile_spec

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import app.ij.smile_spec.databinding.ActivityResultBinding

/**
 * ResultActivity - Displays prediction results with elegant design
 * Shows predicted shade, confidence, and probability bars
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    companion object {
        const val EXTRA_PREDICTED_CLASS = "extra_predicted_class"
        const val EXTRA_CONFIDENCE = "extra_confidence"
        const val EXTRA_PROB_A = "extra_prob_a"
        const val EXTRA_PROB_B = "extra_prob_b"
        const val EXTRA_PROB_C = "extra_prob_c"
        const val EXTRA_PROB_D = "extra_prob_d"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimations()
        displayResults()
        setupClickListeners()
    }

    /**
     * Setup entrance animations with cascade effect
     */
    private fun setupAnimations() {
        // Scale and fade in card
        binding.cardResult.alpha = 0f
        binding.cardResult.scaleX = 0.8f
        binding.cardResult.scaleY = 0.8f
        binding.cardResult.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(100)
            .start()
    }

    /**
     * Display prediction results
     */
    private fun displayResults() {
        val predictedClass = intent.getStringExtra(EXTRA_PREDICTED_CLASS) ?: "Unknown"
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)
        
        val probA = intent.getFloatExtra(EXTRA_PROB_A, 0f)
        val probB = intent.getFloatExtra(EXTRA_PROB_B, 0f)
        val probC = intent.getFloatExtra(EXTRA_PROB_C, 0f)
        val probD = intent.getFloatExtra(EXTRA_PROB_D, 0f)

        // Display main result
        binding.tvShadeResult.text = "Shade $predictedClass"
        binding.tvConfidence.text = String.format("Confidence: %.1f%%", confidence)
        binding.tvDescription.text = getShadeDescription(predictedClass)

        // Animate result text
        binding.tvShadeResult.alpha = 0f
        binding.tvShadeResult.scaleX = 0.7f
        binding.tvShadeResult.scaleY = 0.7f
        binding.tvShadeResult.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(400)
            .start()

        // Display probability bars
        displayProbabilityBars(
            mapOf(
                "A" to probA,
                "B" to probB,
                "C" to probC,
                "D" to probD
            ),
            predictedClass
        )
    }

    /**
     * Get description for shade class
     */
    private fun getShadeDescription(shadeClass: String): String {
        return when (shadeClass) {
            "A" -> getString(R.string.shade_a_desc)
            "B" -> getString(R.string.shade_b_desc)
            "C" -> getString(R.string.shade_c_desc)
            "D" -> getString(R.string.shade_d_desc)
            else -> "Unknown shade"
        }
    }

    /**
     * Display probability bars for all classes
     */
    private fun displayProbabilityBars(
        probabilities: Map<String, Float>,
        predictedClass: String
    ) {
        val sortedProbs = probabilities.entries.sortedByDescending { it.value }
        
        sortedProbs.forEachIndexed { index, entry ->
            val shadeClass = entry.key
            val probability = entry.value
            val isHighest = shadeClass == predictedClass

            val probabilityView = createProbabilityBar(
                shadeClass,
                probability,
                isHighest
            )

            binding.layoutProbabilities.addView(probabilityView)

            // Animate probability bars with cascade effect
            probabilityView.alpha = 0f
            probabilityView.translationX = -50f
            probabilityView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(400)
                .setStartDelay(600L + (index * 100L))
                .start()
        }
    }

    /**
     * Create probability bar view
     */
    private fun createProbabilityBar(
        shadeClass: String,
        probability: Float,
        isHighest: Boolean
    ): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
        }

        // Label row
        val labelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Shade label
        val labelText = TextView(this).apply {
            text = "Shade $shadeClass"
            textSize = 14f
            typeface = if (isHighest) {
                android.graphics.Typeface.DEFAULT_BOLD
            } else {
                android.graphics.Typeface.DEFAULT
            }
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isHighest) R.color.primary else R.color.text_secondary
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Percentage text
        val percentageText = TextView(this).apply {
            text = String.format("%.1f%%", probability)
            textSize = 14f
            typeface = if (isHighest) {
                android.graphics.Typeface.DEFAULT_BOLD
            } else {
                android.graphics.Typeface.DEFAULT
            }
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isHighest) R.color.primary else R.color.text_secondary
                )
            )
        }

        labelRow.addView(labelText)
        labelRow.addView(percentageText)

        // Progress bar
        val progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(10)
            ).apply {
                topMargin = dpToPx(4)
            }
            max = 100
            progress = probability.toInt()
            progressDrawable = ContextCompat.getDrawable(
                context,
                if (isHighest) {
                    android.R.drawable.progress_horizontal
                } else {
                    android.R.drawable.progress_horizontal
                }
            )
            
            // Set color
            progressTintList = ContextCompat.getColorStateList(
                context,
                if (isHighest) R.color.primary else R.color.text_secondary
            )
        }

        container.addView(labelRow)
        container.addView(progressBar)

        return container
    }

    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /**
     * Setup button click listeners
     */
    private fun setupClickListeners() {
        // Analyze another button
        binding.btnAnalyzeAnother.setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }

        // Share button
        binding.btnShare.setOnClickListener {
            animateButtonClick(it) {
                shareResult()
            }
        }
    }

    /**
     * Animate button click
     */
    private fun animateButtonClick(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }

    /**
     * Share result via intent
     */
    private fun shareResult() {
        val predictedClass = intent.getStringExtra(EXTRA_PREDICTED_CLASS) ?: "Unknown"
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)

        val shareText = """
            🦷 Tooth Shade Analysis Result
            
            Shade: $predictedClass
            Confidence: ${String.format("%.1f%%", confidence)}
            
            Analyzed with Tooth Shade Analyzer
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share Result"))
    }
}



