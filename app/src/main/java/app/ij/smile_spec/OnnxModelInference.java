package app.ij.smile_spec;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color; // Import Color
import android.util.Log;

import ai.onnxruntime.OnnxMap; // Import OnnxMap
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue; // Import OnnxValue
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List; // Import List
import java.util.Map;

/**
 * OnnxModelInference - Tooth shade classification using ONNX Runtime
 *
 * This class handles:
 * 1. Loading the ONNX model from assets
 * 2. Creating inference sessions
 * 3. Running predictions on tooth images
 * 4. Interpreting and formatting results
 *
 * The model classifies tooth shades based on its training data.
 */
public class OnnxModelInference {

    private static final String TAG = "OnnxModelInference";
    private static final String MODEL_FILE_NAME = "tooth_shade_model.onnx";

    // This array is no longer strictly necessary as the model outputs labels,
    // but can be kept for reference or other UI purposes if needed.
    // private static final String[] SHADE_CLASSES = {"A", "B", "C", "D"};

    // Model dimensions
    static final int BATCH_SIZE = 1;
    static final int NUM_CHANNELS = 3;
    static final int IMAGE_HEIGHT = 128;
    static final int IMAGE_WIDTH = 128;

    private OrtEnvironment environment;
    private OrtSession session;
    private boolean isModelLoaded = false;

    /**
     * Result class to hold prediction data
     */
    public static class PredictionResult {
        private final Map<String, Float> probabilities;
        private final String predictedClass;
        private final float confidence;

        public PredictionResult(Map<String, Float> probabilities, String predictedClass, float confidence) {
            this.probabilities = probabilities;
            this.predictedClass = predictedClass;
            this.confidence = confidence;
        }

        public Map<String, Float> getProbabilities() {
            return probabilities;
        }

        public String getPredictedClass() {
            return predictedClass;
        }

        public float getConfidence() {
            return confidence;
        }

        public float getProbabilityForClass(String className) {
            return probabilities.getOrDefault(className, 0.0f);
        }
    }

    /**
     * Initializes the ONNX Runtime environment and loads the model
     */
    public void loadModel(Context context) throws IOException, OrtException {
        if (isModelLoaded) {
            Log.i(TAG, "Model already loaded");
            return;
        }

        Log.i(TAG, "Loading ONNX model from assets...");

        environment = OrtEnvironment.getEnvironment();
        byte[] modelBytes = loadModelFile(context);
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        // We still create the session, even if we don't use it, to ensure
        // the model file is present and the app is set up correctly.
        session = environment.createSession(modelBytes, sessionOptions);

        isModelLoaded = true;
        Log.i(TAG, "Model loaded successfully");
        logModelInfo();
    }

    private byte[] loadModelFile(Context context) throws IOException {
        InputStream inputStream = context.getAssets().open(MODEL_FILE_NAME);
        byte[] modelBytes = new byte[inputStream.available()];
        inputStream.read(modelBytes);
        inputStream.close();
        return modelBytes;
    }

    private void logModelInfo() {
        try {
            Log.i(TAG, "Model Input Names: " + session.getInputNames());
            Log.i(TAG, "Model Output Names: " + session.getOutputNames());
        } catch (Exception e) {
            Log.e(TAG, "Error getting model info", e);
        }
    }

    /**
     * Predicts tooth shade from an image
     */
    public PredictionResult predictShade(Bitmap bitmap) throws OrtException {
        if (!isModelLoaded) {
            // We still check if the model *could* be loaded, even if we don't use it.
            throw new IllegalStateException("Model not loaded. Call loadModel() first.");
        }

        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "Invalid bitmap provided.");
            throw new IllegalArgumentException("Bitmap is null or recycled");
        }

        Log.i(TAG, "Starting prediction with bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());

        // --- START: NEW RULE-BASED LOGIC ---
        // This logic REPLACES the ONNX model inference.
        // It calculates the average color of the image and maps it to a shade.

        try {
            Log.d(TAG, "Using NEW rule-based color logic, NOT the ONNX model.");

            // 1. Get the average color of the image by scaling it to 1x1 pixel
            Bitmap avgBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
            int avgPixel = avgBitmap.getPixel(0, 0);
            avgBitmap.recycle(); // Clean up the small bitmap

            // 2. Extract RGB components
            int r = Color.red(avgPixel);
            int g = Color.green(avgPixel);
            int b = Color.blue(avgPixel);

            // 3. Convert RGB to HSV (Hue, Saturation, Value)
            // H[0] = Hue (0-360) -> The "color" (e.g., 60 is yellow)
            // H[1] = Saturation (0-1) -> The "intensity" (0 is gray, 1 is pure color)
            // H[2] = Value (0-1) -> The "brightness" (0 is black, 1 is white)
            float[] hsv = new float[3];
            Color.RGBToHSV(r, g, b, hsv);
            float hue = hsv[0];
            float sat = hsv[1];
            float val = hsv[2];

            Log.d(TAG, String.format("Average Color: R(%d) G(%d) B(%d) -> H(%.0f) S(%.2f) V(%.2f)", r, g, b, hue, sat, val));

            String predictedClass = "C"; // Default shade

            // 4. Apply your rules.
            // NOTE: You will need to "tune" these numbers yourself by testing
            // with different images to get the results you want.

            // Rule for "D" (Brown)
            // Hue for brown is in the orange/red range (20-45)
            // Saturation is medium-low
            // Value is medium-low (not too bright)
            if (hue >= 20 && hue <= 45 && sat > 0.25 && val < 0.7) {
                predictedClass = "D";
            }
            // Rule for "B" and "C" (Yellows)
            // Hue for yellow is (46-65)
            else if (hue >= 46 && hue <= 65) {
                // "Slightly yellowish" (B) = low saturation
                if (sat < 0.35) {
                    predictedClass = "B";
                }
                // "More real yellow" (C) = high saturation
                else {
                    predictedClass = "C";
                }
            }
            // Rule for "A" (Too white / Achromatic)
            // Very low saturation (no color) and high value (bright)
            else if (sat < 0.2 && val > 0.8) {
                predictedClass = "A";
            }
            // If it doesn't fit any other rule, it will stay as the default "C"
            // or you could make the default "B". Let's change default to B.
            else {
                // Default case if no other rule matches
                if (predictedClass.equals("C")) { // Only if it's still the default
                    predictedClass = "B"; // Default to B instead
                }
            }

            Log.i(TAG, "Rule-based prediction: " + predictedClass);

            // 5. Create a "fake" result object to return
            // We give it 100% confidence because it's a hard-coded rule.
            float confidencePercent = 100.0f;
            Map<String, Float> probMapPercent = new HashMap<>();
            probMapPercent.put("A", 0.0f);
            probMapPercent.put("B", 0.0f);
            probMapPercent.put("C", 0.0f);
            probMapPercent.put("D", 0.0f);
            // Set the winning class to 100%
            probMapPercent.put(predictedClass, confidencePercent);

            return new PredictionResult(probMapPercent, predictedClass, confidencePercent);

            // --- END: NEW RULE-BASED LOGIC ---

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during rule-based prediction", e);
            throw new OrtException(OrtException.OrtErrorCode.ORT_FAIL, "Unexpected error: " + e.getMessage());
        } finally {
            // No tensors or results to close, so this is empty.
        }
    }

    // This function is no longer needed as the model provides probabilities directly.
    /*
    private float[] softmax(float[] logits) {
        float[] probabilities = new float[logits.length];
        float maxLogit = logits[0];
        for (float logit : logits) if (logit > maxLogit) maxLogit = logit;
        float sumExp = 0f;
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = (float) Math.exp(logits[i] - maxLogit);
            sumExp += probabilities[i];
        }
        for (int i = 0; i < logits.length; i++) probabilities[i] /= sumExp;
        return probabilities;
    }
    */

    public boolean isModelLoaded() {
        return isModelLoaded;
    }

    public void close() {
        if (session != null) try { session.close(); } catch (OrtException ignored) {}
        if (environment != null) try { environment.close(); } catch (Exception ignored) {}
        isModelLoaded = false;
        Log.i(TAG, "Model resources released.");
    }
}

