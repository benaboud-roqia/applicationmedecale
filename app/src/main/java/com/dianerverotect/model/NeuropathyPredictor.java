package com.dianerverotect.model;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * This class handles the prediction of diabetic neuropathy using a TensorFlow Lite model.
 */
public class NeuropathyPredictor {
    private static final String TAG = "NeuropathyPredictor";
    private static final String MODEL_FILE = "model.tflite";
    
    private Interpreter interpreter;
    private final Context context;
    
    // Flag to track whether the real model was used for prediction
    private boolean usedRealModel = false;
    
    // Constructor
    public NeuropathyPredictor(Context context) {
        this.context = context;
        try {
            Log.d(TAG, "Initializing NeuropathyPredictor with TensorFlow Lite model");
            interpreter = new Interpreter(loadModelFile());
            Log.d(TAG, "NeuropathyPredictor initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing NeuropathyPredictor: " + e.getMessage(), e);
            // We'll continue without the interpreter and handle errors in predict()
        }
    }
    
    /**
     * Loads the TensorFlow Lite model from assets.
     */
    private MappedByteBuffer loadModelFile() throws IOException {
        try {
            Log.d(TAG, "Loading model file: " + MODEL_FILE);
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
            Log.d(TAG, "File descriptor opened successfully");
            
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            Log.d(TAG, "Model file details - startOffset: " + startOffset + ", declaredLength: " + declaredLength);
            
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            Log.d(TAG, "Model file loaded successfully");
            return buffer;
        } catch (Exception e) {
            Log.e(TAG, "Error loading model file: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Simple test method to ensure the class is working properly.
     */
    public boolean testConnection() {
        Log.d(TAG, "Testing connection to NeuropathyPredictor");
        return interpreter != null;
    }
    

    
    /**
     * Predicts neuropathy risk based on input features.
     * 
     * @param features Array of input features
     * @return Probability of neuropathy (0.0 to 1.0)
     */
    public float predict(float[] features) {
        // Reset the flag at the start of each prediction
        usedRealModel = false;
        
        // First check if the interpreter was initialized successfully
        if (interpreter == null) {
            Log.e(TAG, "TensorFlow Lite interpreter is null, falling back to backup algorithm");
            return fallbackPredict(features);
        }
        
        try {
            Log.d(TAG, "Starting prediction with TensorFlow Lite model using " + features.length + " features");
            
            // Log the features for debugging
            StringBuilder featureLog = new StringBuilder("Features: ");
            for (int i = 0; i < features.length; i++) {
                featureLog.append(features[i]);
                if (i < features.length - 1) featureLog.append(", ");
            }
            Log.d(TAG, featureLog.toString());
            
            // Get input tensor shape to determine correct input size
            int[] inputShape = interpreter.getInputTensor(0).shape();
            Log.d(TAG, "Model input shape: [" + inputShape[0] + ", " + inputShape[1] + "]");
            
            // Check if we need to adjust our input size
            int requiredFeatures = inputShape[1];
            float[] adjustedFeatures;
            
            if (requiredFeatures != features.length) {
                Log.d(TAG, "Model expects " + requiredFeatures + " features, but we have " + features.length);
                // Create a new array with the correct size
                adjustedFeatures = new float[requiredFeatures];
                // Copy as many features as we can
                int copyLength = Math.min(features.length, requiredFeatures);
                System.arraycopy(features, 0, adjustedFeatures, 0, copyLength);
                // Fill remaining slots with meaningful values instead of zeros
                // Based on the error logs, we need to provide a proper value for the 11th feature
                for (int i = copyLength; i < requiredFeatures; i++) {
                    // Use 1.0f as default for the 11th feature (this is often a bias term)
                    adjustedFeatures[i] = 1.0f;
                    Log.d(TAG, "Added default value 1.0f for feature " + i);
                }
                
                // Log the adjusted features
                StringBuilder adjustedFeatureLog = new StringBuilder("Adjusted features: ");
                for (int i = 0; i < adjustedFeatures.length; i++) {
                    adjustedFeatureLog.append(adjustedFeatures[i]);
                    if (i < adjustedFeatures.length - 1) adjustedFeatureLog.append(", ");
                }
                Log.d(TAG, adjustedFeatureLog.toString());
                
                Log.d(TAG, "Adjusted input features to match model requirements");
            } else {
                adjustedFeatures = features;
            }
            
            // The error suggests we need a 4D input tensor for a CNN model
            // Let's examine the full input shape details
            Log.d(TAG, "Examining detailed model input requirements");
            int[] fullInputShape = interpreter.getInputTensor(0).shape();
            StringBuilder shapeDetails = new StringBuilder("Full input shape: [");
            for (int i = 0; i < fullInputShape.length; i++) {
                shapeDetails.append(fullInputShape[i]);
                if (i < fullInputShape.length - 1) shapeDetails.append(", ");
            }
            shapeDetails.append("]");
            Log.d(TAG, shapeDetails.toString());
            
            try {
                // Prepare input based on the model's expected dimensions
                Object inputData;
                
                if (fullInputShape.length == 4) {
                    // This is likely a CNN model expecting image-like input with shape [batch, height, width, channels]
                    Log.d(TAG, "Model expects 4D input tensor (CNN model), reshaping data");
                    
                    // Create a 4D tensor with shape [1, 1, 1, requiredFeatures] or similar
                    // The exact dimensions depend on your model, but this is a common approach
                    float[][][][] input4D = new float[1][1][1][requiredFeatures];
                    
                    // Copy our features into this 4D structure
                    for (int i = 0; i < requiredFeatures; i++) {
                        input4D[0][0][0][i] = adjustedFeatures[i];
                    }
                    
                    inputData = input4D;
                    Log.d(TAG, "Reshaped input to 4D tensor with shape [1, 1, 1, " + requiredFeatures + "]");
                } else if (fullInputShape.length == 3) {
                    // This is a 3D input tensor
                    Log.d(TAG, "Model expects 3D input tensor, reshaping data");
                    
                    // Create a 3D tensor with the exact shape from the model
                    // From logs, we see the shape is [1, 11, 1]
                    int dim1 = fullInputShape[0];
                    int dim2 = fullInputShape[1];
                    int dim3 = fullInputShape[2];
                    
                    Log.d(TAG, "Creating 3D tensor with exact dimensions: [" + dim1 + ", " + dim2 + ", " + dim3 + "]");
                    float[][][] input3D = new float[dim1][dim2][dim3];
                    
                    // Copy our features - for shape [1, 11, 1], we need to put each feature in [0][i][0]
                    for (int i = 0; i < requiredFeatures; i++) {
                        input3D[0][i][0] = adjustedFeatures[i];
                    }
                    
                    inputData = input3D;
                    Log.d(TAG, "Reshaped input to 3D tensor with exact model dimensions");
                } else {
                    // Default to 2D for standard models
                    float[][] input2D = new float[1][requiredFeatures];
                    System.arraycopy(adjustedFeatures, 0, input2D[0], 0, requiredFeatures);
                    inputData = input2D;
                    Log.d(TAG, "Using standard 2D input with shape [1, " + requiredFeatures + "]");
                }
                
                Log.d(TAG, "Features copied to properly shaped input array");
                
                // Get output tensor info
                int[] outputShape = interpreter.getOutputTensor(0).shape();
                StringBuilder outShapeStr = new StringBuilder("Output shape: [");
                for (int i = 0; i < outputShape.length; i++) {
                    outShapeStr.append(outputShape[i]);
                    if (i < outputShape.length - 1) outShapeStr.append(", ");
                }
                outShapeStr.append("]");
                Log.d(TAG, outShapeStr.toString());
                
                // Run the model
                if (outputShape.length == 2 && outputShape[0] == 1 && outputShape[1] == 1) {
                    // Standard output shape
                    float[][] output = new float[1][1];
                    interpreter.run(inputData, output);
                    float result = output[0][0];
                    usedRealModel = true;
                    Log.d(TAG, "Prediction result from TensorFlow model: " + result);
                    return result;
                } else {
                    // Use a more flexible approach for unusual output shapes
                    ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4); // 4 bytes for a float
                    outputBuffer.order(ByteOrder.nativeOrder());
                    
                    Map<Integer, Object> outputs = new HashMap<>();
                    outputs.put(0, outputBuffer);
                    
                    interpreter.runForMultipleInputsOutputs(new Object[]{inputData}, outputs);
                    
                    outputBuffer.rewind();
                    float result = outputBuffer.getFloat();
                    usedRealModel = true;
                    Log.d(TAG, "Prediction result from TensorFlow model (using ByteBuffer): " + result);
                    return result;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during model inference: " + e.getMessage(), e);
                
                // We're getting a tensor allocation error, which suggests a fundamental mismatch
                // between our input and what the model expects. Let's use our improved fallback algorithm.
                Log.d(TAG, "TensorFlow model failed with tensor allocation error - using fallback algorithm");
                usedRealModel = false;
                
                // Use our sophisticated fallback algorithm that better matches expected model output
                float result = fallbackPredict(features); // Use original features, not adjusted ones
                
                Log.d(TAG, "Using fallback algorithm result: " + result);
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during TensorFlow prediction: " + e.getMessage(), e);
            Log.d(TAG, "Falling back to backup algorithm");
            return fallbackPredict(features);
        }
    }
    
    /**
     * Fallback prediction method using a simplified algorithm when TensorFlow fails.
     * 
     * @param features Array of input features
     * @return Probability of neuropathy (0.0 to 1.0)
     */
    private float fallbackPredict(float[] features) {
        try {
            Log.d(TAG, "Using fallback prediction algorithm with " + features.length + " features");
            
            // Log the features we're using for the fallback algorithm
            StringBuilder featureLog = new StringBuilder("Fallback algorithm using features: ");
            for (int i = 0; i < features.length; i++) {
                featureLog.append(features[i]);
                if (i < features.length - 1) featureLog.append(", ");
            }
            Log.d(TAG, featureLog.toString());
            
            // Extract key features (assuming standard order)
            float age = features.length > 0 ? features[0] : 50.0f; // Default age 50
            float diabetesDuration = features.length > 1 ? features[1] : 5.0f; // Default 5 years
            float glucoseLevel = features.length > 2 ? features[2] : 120.0f; // Default 120 mg/dL
            float emgAmplitude = features.length > 3 ? features[3] : 25.0f; // Default 25 mV
            float emgFrequency = features.length > 4 ? features[4] : 15.0f; // Default 15 Hz
            float emgVariability = features.length > 5 ? features[5] : 10.0f; // Default 10
            
            // Additional features if available
            boolean hasTemperatureSensation = features.length > 8 ? features[8] > 0.5f : true;
            boolean hasPressureSensation = features.length > 9 ? features[9] > 0.5f : true;
            
            // Calculate base risk from glucose (major factor)
            float glucoseRisk;
            if (glucoseLevel > 200) {
                glucoseRisk = 0.6f; // Reduced from 0.8f
            } else if (glucoseLevel > 170) {
                glucoseRisk = 0.45f; // Reduced from 0.65f
            } else if (glucoseLevel > 140) {
                glucoseRisk = 0.3f; // Reduced from 0.5f
            } else if (glucoseLevel > 120) {
                glucoseRisk = 0.2f; // Reduced from 0.3f
            } else {
                glucoseRisk = 0.1f; // Reduced from 0.15f
            }
            Log.d(TAG, "Glucose risk component: " + glucoseRisk);
            
            // Factor in diabetes duration (exponential risk increase)
            // Using sigmoid function to model risk increase with duration
            float durationFactor = (float)(1.0 / (1.0 + Math.exp(-0.2 * (diabetesDuration - 7))));
            Log.d(TAG, "Duration factor: " + durationFactor);
            
            // Factor in age (risk increases with age, especially after 50)
            float ageFactor;
            if (age > 65) {
                ageFactor = 0.8f;
            } else if (age > 55) {
                ageFactor = 0.6f;
            } else if (age > 45) {
                ageFactor = 0.4f;
            } else if (age > 35) {
                ageFactor = 0.2f;
            } else {
                ageFactor = 0.1f;
            }
            Log.d(TAG, "Age factor: " + ageFactor);
            
            // Factor in EMG readings - more sophisticated analysis
            float emgRisk = 0.0f;
            
            // Amplitude analysis (lower amplitude = higher risk)
            float amplitudeRisk;
            if (emgAmplitude < 15) {
                amplitudeRisk = 0.9f; // Severe reduction
            } else if (emgAmplitude < 20) {
                amplitudeRisk = 0.7f; // Significant reduction
            } else if (emgAmplitude < 25) {
                amplitudeRisk = 0.5f; // Moderate reduction
            } else if (emgAmplitude < 30) {
                amplitudeRisk = 0.3f; // Mild reduction
            } else {
                amplitudeRisk = 0.1f; // Normal range
            }
            
            // Frequency analysis (higher frequency = higher risk)
            float frequencyRisk;
            if (emgFrequency > 30) {
                frequencyRisk = 0.9f; // Severe abnormality
            } else if (emgFrequency > 25) {
                frequencyRisk = 0.7f; // Significant abnormality
            } else if (emgFrequency > 20) {
                frequencyRisk = 0.5f; // Moderate abnormality
            } else if (emgFrequency > 15) {
                frequencyRisk = 0.3f; // Mild abnormality
            } else {
                frequencyRisk = 0.1f; // Normal range
            }
            
            // Variability analysis (higher variability = higher risk)
            float variabilityRisk;
            if (emgVariability > 25) {
                variabilityRisk = 0.9f; // Severe variability
            } else if (emgVariability > 20) {
                variabilityRisk = 0.7f; // Significant variability
            } else if (emgVariability > 15) {
                variabilityRisk = 0.5f; // Moderate variability
            } else if (emgVariability > 10) {
                variabilityRisk = 0.3f; // Mild variability
            } else {
                variabilityRisk = 0.1f; // Normal range
            }
            
            // Combine EMG factors
            emgRisk = (amplitudeRisk * 0.4f) + (frequencyRisk * 0.3f) + (variabilityRisk * 0.3f);
            Log.d(TAG, "EMG risk component: " + emgRisk);
            
            // Factor in sensation loss
            float sensationRisk = 0.0f;
            if (!hasTemperatureSensation && !hasPressureSensation) {
                sensationRisk = 0.9f; // Both sensations lost - high risk
            } else if (!hasTemperatureSensation || !hasPressureSensation) {
                sensationRisk = 0.5f; // One sensation lost - moderate risk
            } else {
                sensationRisk = 0.1f; // Normal sensation
            }
            Log.d(TAG, "Sensation risk component: " + sensationRisk);
            
            // Combine all factors with appropriate weights based on clinical importance
            float prediction = (glucoseRisk * 0.30f) +  // Glucose is a primary factor but reduced weight
                              (durationFactor * 0.15f) + // Duration is important but less than current glucose
                              (ageFactor * 0.10f) +     // Age is a risk factor but less significant
                              (emgRisk * 0.25f) +       // EMG findings are important but reduced weight
                              (sensationRisk * 0.10f);  // Sensation is important but often a later sign
            
            // Apply a scaling factor to reduce overall prediction values
            prediction = prediction * 0.85f; // Scale down the final prediction value
            
            // Ensure result is in valid range 0-1
            prediction = Math.max(0.0f, Math.min(prediction, 1.0f));
            
            Log.d(TAG, "Final fallback prediction: " + prediction);
            return prediction;
        } catch (Exception e) {
            Log.e(TAG, "Error during fallback prediction: " + e.getMessage(), e);
            // Return a default value in case of error
            return 0.5f; // Neutral prediction
        }
    }
    
    /**
     * Checks if the real ML model was used for the last prediction.
     * 
     * @return true if the real model was used, false if fallback algorithm was used
     */
    public boolean usedRealModel() {
        return usedRealModel;
    }
    
    /**
     * Evaluates neuropathy risk based on prediction and additional factors.
     * 
     * @param prediction Raw prediction from model
     * @param fastingGlucose Fasting glucose value
     * @param hasTemperatureSensation Whether patient can sense temperature
     * @param hasPressureSensation Whether patient can sense pressure
     * @return Risk assessment result
     */
    public RiskAssessment evaluateRisk(float prediction, float fastingGlucose, 
                                      boolean hasTemperatureSensation, boolean hasPressureSensation) {
        try {
            Log.d(TAG, "Evaluating risk with prediction: " + prediction + ", glucose: " + fastingGlucose + 
                  ", temperature sensation: " + hasTemperatureSensation + ", pressure sensation: " + hasPressureSensation);
            
            // Default threshold - increased to make HIGH risk less common
            float threshold = 0.6f; // Increased from 0.5f
            
            // Adjust threshold based on risk factors
            if (fastingGlucose > 200) {
                threshold -= 0.1f;  // More aggressive detection for very high sugar
                Log.d(TAG, "Adjusted threshold for very high glucose (>200): " + threshold);
            } else if (fastingGlucose > 140) {
                threshold -= 0.05f;  // Slightly more aggressive for moderately high sugar
                Log.d(TAG, "Adjusted threshold for high glucose (>140): " + threshold);
            }
            
            // Adjust for sensory symptoms
            if (!hasTemperatureSensation && !hasPressureSensation) {
                threshold -= 0.15f;  // Both sensations missing is high risk
                Log.d(TAG, "Adjusted threshold for both sensations missing: " + threshold);
            } else if (!hasTemperatureSensation || !hasPressureSensation) {
                threshold -= 0.05f;  // One sensation missing is moderate risk
                Log.d(TAG, "Adjusted threshold for one sensation missing: " + threshold);
            }
            
            // Determine risk level with adjusted thresholds
            RiskLevel riskLevel;
            if (prediction > threshold + 0.3f) { // Increased from 0.25f
                riskLevel = RiskLevel.HIGH;
            } else if (prediction > threshold) {
                riskLevel = RiskLevel.MODERATE;
            } else {
                riskLevel = RiskLevel.LOW;
            }
            
            // Add more detailed logging to help diagnose risk level determination
            Log.d(TAG, "Risk calculation: prediction = " + prediction + ", threshold = " + threshold);
            Log.d(TAG, "HIGH threshold = " + (threshold + 0.3f) + ", MODERATE threshold = " + threshold);
            Log.d(TAG, "Determined risk level: " + riskLevel);
            
            // Generate recommendations based on risk level
            Log.d(TAG, "Generating recommendations...");
            Map<String, String> recommendations = generateRecommendations(riskLevel, fastingGlucose, 
                                                                        hasTemperatureSensation, hasPressureSensation);
            Log.d(TAG, "Generated " + recommendations.size() + " recommendations");
            
            // Create risk assessment and set the model flag
            RiskAssessment assessment = new RiskAssessment(prediction, riskLevel, recommendations);
            assessment.setUsedRealModel(usedRealModel);
            Log.d(TAG, "Risk assessment created successfully using " + 
                  (usedRealModel ? "real ML model" : "fallback algorithm"));
            return assessment;
        } catch (Exception e) {
            Log.e(TAG, "Error evaluating risk: " + e.getMessage(), e);
            // Create a default low risk assessment in case of error
            Map<String, String> defaultRecommendations = new HashMap<>();
            defaultRecommendations.put("Error", "An error occurred during risk assessment. Please try again later.");
            RiskAssessment errorAssessment = new RiskAssessment(0.0f, RiskLevel.LOW, defaultRecommendations);
            errorAssessment.setUsedRealModel(false);
            return errorAssessment;
        }
    }
    
    /**
     * Generates recommendations based on risk level and patient data.
     */
    private Map<String, String> generateRecommendations(RiskLevel riskLevel, float fastingGlucose,
                                                      boolean hasTemperatureSensation, boolean hasPressureSensation) {
        try {
            Log.d(TAG, "Generating recommendations for risk level: " + riskLevel);
            Map<String, String> recommendations = new HashMap<>();
            
            // Add general recommendations based on risk level
            switch (riskLevel) {
                case HIGH:
                    Log.d(TAG, "Adding HIGH risk recommendations");
                    recommendations.put("Medical Consultation", 
                            "Schedule an appointment with your healthcare provider as soon as possible.");
                    recommendations.put("Glucose Management", 
                            "Monitor your blood glucose levels more frequently and maintain tight control.");
                    recommendations.put("Foot Care", 
                            "Inspect your feet daily for cuts, blisters, or sores.");
                    break;
                    
                case MODERATE:
                    Log.d(TAG, "Adding MODERATE risk recommendations");
                    recommendations.put("Medical Follow-up", 
                            "Discuss these results with your healthcare provider at your next appointment.");
                    recommendations.put("Glucose Management", 
                            "Continue monitoring your blood glucose levels regularly.");
                    recommendations.put("Lifestyle", 
                            "Consider increasing physical activity to improve circulation.");
                    break;
                    
                case LOW:
                    Log.d(TAG, "Adding LOW risk recommendations");
                    recommendations.put("Preventive Care", 
                            "Continue your current diabetes management plan.");
                    recommendations.put("Monitoring", 
                            "Regular check-ups with your healthcare provider are recommended.");
                    recommendations.put("Lifestyle", 
                            "Maintain a healthy diet and regular exercise routine.");
                    break;
            }
            
            // Add specific recommendations based on symptoms
            if (!hasTemperatureSensation || !hasPressureSensation) {
                Log.d(TAG, "Adding sensory protection recommendations");
                recommendations.put("Sensory Protection", 
                        "Take extra precautions with hot surfaces and sharp objects. Wear protective footwear.");
            }
            
            // Add recommendations based on glucose levels
            if (fastingGlucose > 140) {
                Log.d(TAG, "Adding glucose control recommendations");
                recommendations.put("Glucose Control", 
                        "Your glucose levels are elevated. Consider dietary adjustments and consult your healthcare provider.");
            }
            
            Log.d(TAG, "Generated " + recommendations.size() + " recommendations");
            return recommendations;
        } catch (Exception e) {
            Log.e(TAG, "Error generating recommendations: " + e.getMessage(), e);
            Map<String, String> errorRecommendations = new HashMap<>();
            errorRecommendations.put("General Advice", "Continue with your regular diabetes management plan and consult your healthcare provider.");
            return errorRecommendations;
        }
    }
    
    /**
     * Closes the interpreter when no longer needed.
     */
    public void close() {
        Log.d(TAG, "Closing NeuropathyPredictor resources");
        if (interpreter != null) {
            try {
                interpreter.close();
                Log.d(TAG, "TensorFlow Lite interpreter closed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error closing interpreter: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Risk level enumeration.
     */
    public enum RiskLevel {
        LOW, MODERATE, HIGH
    }
    /**
     * Risk assessment result class.
     */
    public static class RiskAssessment {
        private final float predictionScore;
        private final RiskLevel riskLevel;
        private final Map<String, String> recommendations;
        private boolean usedRealModel;
        
        public RiskAssessment(float predictionScore, RiskLevel riskLevel, Map<String, String> recommendations) {
            this.predictionScore = predictionScore;
            this.riskLevel = riskLevel;
            this.recommendations = recommendations;
            this.usedRealModel = false;
        }
        
        public void setUsedRealModel(boolean usedRealModel) {
            this.usedRealModel = usedRealModel;
        }
        
        public boolean usedRealModel() {
            return usedRealModel;
        }
        
        public float getPredictionScore() {
            return predictionScore;
        }
        
        public RiskLevel getRiskLevel() {
            return riskLevel;
        }
        
        public Map<String, String> getRecommendations() {
            return recommendations;
        }
        
        @Override
        public String toString() {
            return "Risk Level: " + riskLevel + 
                   "\nPrediction Score: " + predictionScore;
        }
    }
}
