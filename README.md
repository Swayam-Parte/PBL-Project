# Smile Spec - Tooth Shade Analyzer

An Android application that uses machine learning to classify tooth shades into categories (A, B, C, D) using an ONNX model. The app runs **fully offline** with no network dependencies.

## 📁 Project Structure

```
Smile_Spec/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── assets/
│   │       │   └── tooth_shade_model.onnx      # ONNX ML model file
│   │       ├── java/app/ij/smile_spec/
│   │       │   ├── MainActivity.kt              # Main UI and app logic
│   │       │   ├── OnnxModelInference.java     # Model loading and inference
│   │       │   └── AndroidStandardScaler.java  # Image preprocessing
│   │       ├── res/
│   │       │   └── xml/
│   │       │       └── file_paths.xml          # FileProvider configuration
│   │       └── AndroidManifest.xml             # App permissions and configuration
│   └── build.gradle.kts                        # Dependencies including ONNX Runtime
└── README.md                                    # This file
```

## 🧠 Model Information

### Model Location
The trained ONNX model is located at:
```
app/src/main/assets/tooth_shade_model.onnx
```

### Model Details
- **Type**: Random Forest Classifier (converted to ONNX format)
- **Input**: 224x224 RGB image tensor (NCHW format)
- **Output**: Probabilities for 4 shade classes (A, B, C, D)
- **Runtime**: Microsoft ONNX Runtime 1.17.0 for Android

## 🔧 How It Works

### 1. Image Preprocessing (`AndroidStandardScaler.java`)

The preprocessing pipeline ensures images match the format expected by the trained model:

1. **Resize**: Images are resized to 224x224 pixels
2. **Normalize**: Pixel values are scaled from [0, 255] to [0, 1]
3. **Standardize**: Apply ImageNet normalization:
   - Mean: [0.485, 0.456, 0.406] for RGB channels
   - Std: [0.229, 0.224, 0.225] for RGB channels
4. **Format Conversion**: Convert to NCHW format (Channels, Height, Width)

**Key Methods:**
- `preprocessImage(Bitmap)` - Standard preprocessing with ImageNet normalization
- `preprocessImage(Bitmap, mean[], std[])` - Custom normalization parameters
- `preprocessImageSimple(Bitmap)` - Simple [0, 1] normalization without standardization

### 2. Model Inference (`OnnxModelInference.java`)

Handles the complete inference pipeline:

1. **Model Loading**: Loads ONNX model from assets folder at app startup
2. **Session Creation**: Creates ONNX Runtime inference session
3. **Tensor Creation**: Converts preprocessed image to ONNX tensor
4. **Prediction**: Runs forward pass through the model
5. **Post-processing**: Applies softmax to get probability distribution
6. **Result Formatting**: Returns structured prediction results

**Key Methods:**
- `loadModel(Context)` - Initialize and load the ONNX model
- `predictShade(Bitmap)` - Run inference on an image
- `close()` - Clean up model resources

**PredictionResult Class:**
```java
public class PredictionResult {
    Map<String, Float> probabilities;  // Probability for each class (%)
    String predictedClass;             // Highest probability class (A/B/C/D)
    float confidence;                  // Confidence score (%)
}
```

### 3. User Interface (`MainActivity.kt`)

Modern Jetpack Compose UI with the following features:

**Image Input:**
- 📁 **Gallery**: Select image from device storage
- 📷 **Camera**: Capture new photo (with runtime permission handling)

**Analysis:**
- **Analyze Shade** button triggers inference
- Progress indicator during processing
- Async processing on IO thread

**Results Display:**
- Primary prediction with confidence score
- Probability bars for all 4 shade classes (A, B, C, D)
- Visual highlighting of the highest probability class
- "Analyze Another Image" button to reset

## 🚀 Setup and Installation

### Prerequisites
- Android Studio Hedgehog or newer
- Android SDK 24 or higher (Android 7.0+)
- Java 11

### Steps

1. **Clone or Open Project**
   ```bash
   cd /path/to/Smile_Spec
   ```

2. **Ensure Model File Exists**
   - Place your `tooth_shade_model.onnx` file in:
     ```
     app/src/main/assets/tooth_shade_model.onnx
     ```
   - The file is already in place if you've copied it from PyCharm

3. **Sync Gradle**
   - Open project in Android Studio
   - Click "Sync Now" when prompted
   - Gradle will download ONNX Runtime and other dependencies

4. **Build and Run**
   - Connect Android device or start emulator
   - Click Run (Shift + F10)
   - Grant camera permission when prompted

## 📱 How to Use the App

1. **Launch App**: Open "Tooth Shade Analyzer"

2. **Select Image**:
   - Tap "📁 Gallery" to choose existing image
   - Tap "📷 Camera" to take new photo

3. **Analyze**:
   - Tap "Analyze Shade" button
   - Wait for processing (typically < 1 second)

4. **View Results**:
   - See predicted shade (A, B, C, or D)
   - View confidence percentage
   - Check probability distribution for all classes

5. **Analyze Another**:
   - Tap "Analyze Another Image" to start over

## 🧪 Testing Predictions

### Basic Testing

1. **Verify Model Loading**
   - Check logcat for: `Model loaded successfully`
   - If error appears, verify model file exists in assets

2. **Test with Sample Images**
   - Use images from your Python training dataset
   - Compare Android predictions with Python predictions
   - Expected: Same class predictions with similar probabilities

3. **Test Different Scenarios**
   - Low light images
   - Different angles
   - Various tooth types
   - Edge cases from your validation set

### Debugging Tips

**Enable Debug Logging:**
```kotlin
// Check logcat for these tags:
- "OnnxModelInference" - Model loading and prediction logs
- "MainActivity" - UI and image processing logs
```

**Common Issues:**

| Issue | Solution |
|-------|----------|
| Model not loading | Verify `tooth_shade_model.onnx` exists in assets folder |
| Wrong predictions | Check if preprocessing matches Python training code |
| App crashes | Check logcat for exceptions; verify ONNX Runtime dependency |
| Camera not working | Grant camera permission in app settings |

### Validation Against Python

To ensure Android predictions match Python:

```python
# Python validation script
import onnxruntime as ort
import numpy as np
from PIL import Image

# Load same model
session = ort.InferenceSession("tooth_shade_model.onnx")

# Preprocess image (must match AndroidStandardScaler)
img = Image.open("test_tooth.jpg").resize((224, 224))
img_array = np.array(img).astype(np.float32) / 255.0

# Apply same normalization
mean = np.array([0.485, 0.456, 0.406])
std = np.array([0.229, 0.224, 0.225])
img_array = (img_array - mean) / std

# Convert to NCHW format
img_array = np.transpose(img_array, (2, 0, 1))
img_array = np.expand_dims(img_array, axis=0)

# Run prediction
outputs = session.run(None, {session.get_inputs()[0].name: img_array})

# Compare with Android output
print("Predictions:", outputs[0])
```

## 🔒 Offline Functionality

The app is designed for **100% offline operation**:

✅ **No Network Required:**
- Model is bundled in assets (no download needed)
- All inference runs locally on device
- No API calls or cloud dependencies

✅ **Privacy:**
- Images never leave the device
- No data collection or telemetry
- Full HIPAA/GDPR compliance potential

✅ **Performance:**
- Fast inference (< 1 second on most devices)
- No network latency
- Works in airplane mode

## 📊 Model Performance

Update this section with your model's metrics:

```
Accuracy: [Your accuracy]%
Precision: [Your precision]
Recall: [Your recall]
F1-Score: [Your F1 score]

Confusion Matrix:
     A    B    C    D
A  [...]
B  [...]
C  [...]
D  [...]
```

## 🛠️ Customization

### Adjust Preprocessing

If your Python model used different normalization:

```java
// In AndroidStandardScaler.java, modify:
private static final float[] MEAN = {your_r, your_g, your_b};
private static final float[] STD = {your_r, your_g, your_b};
```

### Change Input Size

If your model expects different dimensions:

```java
// In AndroidStandardScaler.java:
private static final int IMAGE_WIDTH = your_width;
private static final int IMAGE_HEIGHT = your_height;
```

### Add More Classes

If you have more than 4 shade classes:

```java
// In OnnxModelInference.java:
private static final String[] SHADE_CLASSES = {"A", "B", "C", "D", "E", ...};
```

## 📦 Dependencies

```kotlin
// ONNX Runtime for ML inference
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

// Coil for image loading in Compose
implementation("io.coil-kt:coil-compose:2.5.0")

// Jetpack Compose (Material 3)
implementation(libs.androidx.compose.bom)
implementation(libs.androidx.material3)
```

## 🔄 Updating the Model

To replace the model with a new version:

1. Train new model in Python
2. Export to ONNX format:
   ```python
   from skl2onnx import convert_sklearn
   onx = convert_sklearn(model, initial_types=[...])
   with open("tooth_shade_model.onnx", "wb") as f:
       f.write(onx.SerializeToString())
   ```
3. Replace file in `app/src/main/assets/tooth_shade_model.onnx`
4. Rebuild and test app

## 👥 Authors

Swayam Parte


**Built with:**
- Kotlin & Jetpack Compose
- ONNX Runtime
- Material Design 3

**Last Updated:** November 2025



