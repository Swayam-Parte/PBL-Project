package app.ij.smile_spec

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import app.ij.smile_spec.databinding.ActivityMainBinding
import java.io.File

/**
 * MainActivity - Home screen with elegant UI
 * Features: Camera capture and gallery upload with smooth animations
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cameraImageUri: Uri? = null

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navigateToProcessing(it)
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            navigateToProcessing(cameraImageUri!!)
        }
    }

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                this,
                getString(R.string.permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimations()
        setupClickListeners()
    }

    /**
     * Setup fade-in animations for UI elements
     */
    private fun setupAnimations() {
        // Fade in animation for logo
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in).apply {
            duration = 800
        }
        binding.ivLogo.startAnimation(fadeIn)

        // Slide up animation for title
        binding.tvTitle.alpha = 0f
        binding.tvTitle.translationY = 50f
        binding.tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(200)
            .start()

        // Slide up animation for subtitle
        binding.tvSubtitle.alpha = 0f
        binding.tvSubtitle.translationY = 50f
        binding.tvSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(300)
            .start()

        // Slide up animation for button container
        binding.buttonContainer.alpha = 0f
        binding.buttonContainer.translationY = 100f
        binding.buttonContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(400)
            .start()

        // Fade in footer
        binding.tvFooter.alpha = 0f
        binding.tvFooter.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(700)
            .start()
    }

    /**
     * Setup button click listeners with scale animations
     */
    private fun setupClickListeners() {
        // Take Photo button
        binding.btnTakePhoto.setOnClickListener { view ->
            animateButtonClick(view) {
                checkCameraPermissionAndLaunch()
            }
        }

        // Upload from Gallery button
        binding.btnUploadGallery.setOnClickListener { view ->
            animateButtonClick(view) {
                galleryLauncher.launch("image/*")
            }
        }
    }

    /**
     * Animate button click with scale effect
     */
    private fun animateButtonClick(view: android.view.View, action: () -> Unit) {
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
     * Check camera permission and launch camera if granted
     */
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    this,
                    getString(R.string.permission_camera_rationale),
                    Toast.LENGTH_LONG
                ).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Launch camera with temporary file
     */
    private fun launchCamera() {
        val imageFile = createTempImageFile()
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )
        cameraLauncher.launch(cameraImageUri)
    }

    /**
     * Create temporary file for camera image
     */
    private fun createTempImageFile(): File {
        val timestamp = System.currentTimeMillis()
        val storageDir = cacheDir
        return File(storageDir, "tooth_image_$timestamp.jpg")
    }

    /**
     * Navigate to image processing activity with smooth transition
     */
    private fun navigateToProcessing(imageUri: Uri) {
        val intent = Intent(this, ImageProcessingActivity::class.java).apply {
            putExtra(ImageProcessingActivity.EXTRA_IMAGE_URI, imageUri.toString())
        }
        startActivity(intent)
        // Smooth fade transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
