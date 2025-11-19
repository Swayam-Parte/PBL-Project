package app.ij.smile_spec;

import android.graphics.Bitmap;
import android.util.Log;

import java.nio.FloatBuffer;

public class AndroidStandardScaler {
    
    private static final String TAG = "AndroidStandardScaler";
    public static FloatBuffer preprocessImage(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
            throw new IllegalArgumentException("Bitmap cannot be null");
        }
        
        if (bitmap.isRecycled()) {
            Log.e(TAG, "Bitmap is recycled");
            throw new IllegalArgumentException("Bitmap has been recycled");
        }
        
        try {
            return preprocessImageToFloatBuffer(
                bitmap, 
                OnnxModelInference.IMAGE_WIDTH, 
                OnnxModelInference.IMAGE_HEIGHT
            );
        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing image", e);
            throw new IllegalStateException("Failed to preprocess image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Preprocess an image to a FloatBuffer with specific dimensions
     * 
     * @param bitmap Input image bitmap
     * @param targetWidth Target width for resizing
     * @param targetHeight Target height for resizing
     * @return FloatBuffer with shape [3 * height * width] in NCHW format
     */
    public static FloatBuffer preprocessImageToFloatBuffer(Bitmap bitmap, int targetWidth, int targetHeight) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap is null in preprocessImageToFloatBuffer");
        }
        
        Log.d(TAG, String.format("Preprocessing bitmap: %dx%d -> %dx%d", 
            bitmap.getWidth(), bitmap.getHeight(), targetWidth, targetHeight));
        
        // Step 1: Resize bitmap to target dimensions (preserves aspect ratio)
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        
        try {
            int w = resized.getWidth();
            int h = resized.getHeight();
            
            // Step 2: Extract pixel data
            int[] pixels = new int[w * h];
            resized.getPixels(pixels, 0, w, 0, 0, w, h);
            
            // Step 3: Allocate float array for NCHW format
            // NCHW => size = channels * height * width = 3 * h * w
            int numChannels = 3;
            int size = numChannels * h * w;
            float[] floatArray = new float[size];
            
            // Step 4: Convert pixels to float array in NCHW order
            // NCHW layout: [Channel, Height, Width]
            // For RGB: [R..., G..., B...]
            // Index calculation: channel * (h * w) + y * w + x
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int pixel = pixels[y * w + x];
                    
                    // Extract RGB values (ARGB format)
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    
                    // Calculate indices for NCHW format
                    int baseIndex = y * w + x;
                    int idxR = 0 * (h * w) + baseIndex; // Red channel
                    int idxG = 1 * (h * w) + baseIndex; // Green channel
                    int idxB = 2 * (h * w) + baseIndex; // Blue channel
                    
                    // Normalize to [0, 1] range
                    floatArray[idxR] = r / 255.0f;
                    floatArray[idxG] = g / 255.0f;
                    floatArray[idxB] = b / 255.0f;
                }
            }
            
            // Step 5: Wrap into FloatBuffer (native byte order for ONNX)
            FloatBuffer floatBuffer = FloatBuffer.allocate(floatArray.length);
            floatBuffer.put(floatArray);
            floatBuffer.rewind();
            
            Log.d(TAG, String.format("Preprocessing complete. Buffer size: %d", floatBuffer.capacity()));
            
            return floatBuffer;
            
        } finally {
            // Clean up resized bitmap if it's different from original
            if (resized != bitmap && !resized.isRecycled()) {
                resized.recycle();
            }
        }
    }
    
    /**
     * Validate that preprocessing dimensions are correct
     * 
     * @param buffer Buffer to validate
     * @param expectedSize Expected size in elements
     * @return true if valid, false otherwise
     */
    public static boolean validateBuffer(FloatBuffer buffer, int expectedSize) {
        if (buffer == null) {
            Log.e(TAG, "Buffer is null");
            return false;
        }
        
        if (buffer.capacity() != expectedSize) {
            Log.e(TAG, String.format("Buffer size mismatch: expected %d, got %d", 
                expectedSize, buffer.capacity()));
            return false;
        }
        
        return true;
    }
}
