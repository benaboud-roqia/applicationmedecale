package com.dianerverotect;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dianerverotect.model.NeuropathyPredictor;
import com.dianerverotect.model.RecommendationAdapter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView greetingNameText;
    private EditText glucoseValueInput;
    private Button getStartedButton;
    private NestedScrollView nestedScrollView;
    
    // Test section views
    private TextView testSectionTitle;
    private CardView emgChartCard, question1Card, question2Card;
    private LineChart emgChart;
    private Button yesButton1, noButton1, yesButton2, noButton2, analyzeResultsButton;
    
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;
    
    // Constants for the test
    private static final int COUNTDOWN_SECONDS = 20;
    private static final int COUNTDOWN_INTERVAL = 1000; // 1 second
    
    // Test results
    private boolean temperatureResponse = false;
    private boolean pressureResponse = false;
    private AlertDialog dialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        greetingNameText = view.findViewById(R.id.text_greeting_name);
        glucoseValueInput = view.findViewById(R.id.text_glucose_value);
        getStartedButton = view.findViewById(R.id.button_get_started);
        nestedScrollView = view.findViewById(R.id.nested_scroll_view);
        
        // Initialize test section views
        testSectionTitle = view.findViewById(R.id.text_emg_section_title);
        emgChartCard = view.findViewById(R.id.card_emg_chart);
        question1Card = view.findViewById(R.id.card_question1);
        question2Card = view.findViewById(R.id.card_question2);
        emgChart = view.findViewById(R.id.emg_chart);
        
        yesButton1 = view.findViewById(R.id.yes_button1);
        noButton1 = view.findViewById(R.id.no_button1);
        yesButton2 = view.findViewById(R.id.yes_button2);
        noButton2 = view.findViewById(R.id.no_button2);
        analyzeResultsButton = view.findViewById(R.id.analyze_results_button);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Load user data
        loadUsername();
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Set up EMG chart
        setupEmgChart();

        return view;
    }

    private void loadUsername() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("fullName").exists()) {
                    String username = snapshot.child("fullName").getValue(String.class);
                    greetingNameText.setText(username);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }
    
    private boolean validateGlucoseInput() {
        String glucoseValue = glucoseValueInput.getText().toString().trim();
        if (TextUtils.isEmpty(glucoseValue)) {
            Toast.makeText(getContext(), "Please enter your glucose value", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        try {
            float value = Float.parseFloat(glucoseValue);
            if (value <= 0 || value > 500) { // Reasonable glucose range check
                Toast.makeText(getContext(), "Please enter a valid glucose value", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }
    
    private void saveGlucoseValue() {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        String glucoseValue = glucoseValueInput.getText().toString().trim();
        
        // Save glucose value with timestamp
        long timestamp = System.currentTimeMillis();
        GlucoseReading reading = new GlucoseReading(Float.parseFloat(glucoseValue), timestamp);
        
        usersRef.child(userId).child("glucoseReadings").child(String.valueOf(timestamp))
                .setValue(reading)
                .addOnSuccessListener(aVoid -> {
                    // Successfully saved
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save glucose reading", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showCountdownDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_countdown, null);
        TextView countdownText = dialogView.findViewById(R.id.text_countdown);
        ImageView gifImageView = dialogView.findViewById(R.id.gif_animation);
        Glide.with(this)
                .load(R.drawable.handcrush) // if GIF is in res/raw
                .into(gifImageView);
        
        builder.setView(dialogView);
        builder.setCancelable(false);
        
        dialog = builder.create();
        dialog.show();
        
        new CountDownTimer(COUNTDOWN_SECONDS * 1000, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                countdownText.setText(String.valueOf(secondsRemaining));
            }
            
            @Override
            public void onFinish() {
                dialog.dismiss();
                showTestSection();
            }
        }.start();
    }
    
    private void showTestSection() {
        // Make test section visible
        testSectionTitle.setVisibility(View.VISIBLE);
        emgChartCard.setVisibility(View.VISIBLE);
        question1Card.setVisibility(View.VISIBLE);
        question2Card.setVisibility(View.VISIBLE);
        analyzeResultsButton.setVisibility(View.VISIBLE);
        
        // Generate sample EMG data
        generateSampleEmgData();
        
        // Scroll to the test section
        new Handler().postDelayed(() -> {
            nestedScrollView.smoothScrollTo(0, testSectionTitle.getTop());
        }, 500);
    }
    
    private void setupButtonListeners() {
        // Get Started button
        getStartedButton.setOnClickListener(v -> {
            if (validateGlucoseInput()) {
                saveGlucoseValue();
                showCountdownDialog();
            }
        });
        
        // Question 1 (Temperature) buttons
        yesButton1.setOnClickListener(v -> {
            temperatureResponse = true;
            highlightSelectedButton(yesButton1, noButton1);
            checkEnableAnalyzeButton();
        });
        
        noButton1.setOnClickListener(v -> {
            temperatureResponse = false;
            highlightSelectedButton(noButton1, yesButton1);
            checkEnableAnalyzeButton();
        });
        
        // Question 2 (Pressure) buttons
        yesButton2.setOnClickListener(v -> {
            pressureResponse = true;
            highlightSelectedButton(yesButton2, noButton2);
            checkEnableAnalyzeButton();
        });
        
        noButton2.setOnClickListener(v -> {
            pressureResponse = false;
            highlightSelectedButton(noButton2, yesButton2);
            checkEnableAnalyzeButton();
        });
        
        // Analyze Results button
        analyzeResultsButton.setOnClickListener(v -> {
            saveTestResults();
            showResultsSummary();
        });
        
        // Initially disable the analyze button until all questions are answered
        analyzeResultsButton.setEnabled(false);
    }
    
    private void highlightSelectedButton(Button selectedButton, Button otherButton) {
        selectedButton.setAlpha(1.0f);
        otherButton.setAlpha(0.5f);
    }
    
    private void checkEnableAnalyzeButton() {
        // Enable the analyze button only when both questions have been answered
        boolean question1Answered = yesButton1.getAlpha() == 1.0f || noButton1.getAlpha() == 1.0f;
        boolean question2Answered = yesButton2.getAlpha() == 1.0f || noButton2.getAlpha() == 1.0f;
        
        analyzeResultsButton.setEnabled(question1Answered && question2Answered);
    }
    
    private void setupEmgChart() {
        // Configure the chart appearance
        emgChart.getDescription().setEnabled(false);
        emgChart.setTouchEnabled(true);
        emgChart.setDragEnabled(true);
        emgChart.setScaleEnabled(true);
        emgChart.setPinchZoom(true);
        emgChart.setDrawGridBackground(false);
        
        // Configure X axis
        XAxis xAxis = emgChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(100f);
        xAxis.setLabelCount(5);
        
        // Configure Y axis
        YAxis leftAxis = emgChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(-40f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setLabelCount(7);
        
        YAxis rightAxis = emgChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Configure legend
        Legend legend = emgChart.getLegend();
        legend.setEnabled(false);
    }
    
    private void generateSampleEmgData() {
        List<Entry> entries = new ArrayList<>();
        
        // Generate simulated EMG data with varying patterns
        float baseValue = 20f;
        float amplitude = 15f;
        float noiseLevel = 5f;
        
        // Create a realistic EMG pattern with bursts of activity
        for (int i = 0; i < 100; i++) {
            float value;
            
            // Create baseline with noise
            value = baseValue + (float) (Math.random() * noiseLevel - noiseLevel/2);
            
            // Add periodic muscle contractions
            if (i % 20 < 5) {
                // Burst of activity (simulating muscle contraction)
                value += amplitude * Math.sin(i * Math.PI / 5) + (Math.random() * amplitude/2);
            } else if (i > 50 && i < 60) {
                // Sustained contraction in the middle
                value += amplitude + (Math.random() * noiseLevel);
            } else if (i > 80) {
                // Increasing fatigue pattern toward the end
                value += (amplitude/2) * Math.sin(i * Math.PI / 4) * (100-i)/20;
            }
            
            entries.add(new Entry(i, value));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "EMG Signal");
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#80D6F9FF"));
        dataSet.setFillAlpha(100);
        
        LineData lineData = new LineData(dataSet);
        emgChart.setData(lineData);
        emgChart.invalidate(); // Refresh the chart
        
        // Animate the chart for better visual effect
        emgChart.animateX(1000);
    }
    
    private void saveTestResults() {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();
        
        // Create test result object
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("timestamp", timestamp);
        testResult.put("temperatureSensation", temperatureResponse);
        testResult.put("pressureSensation", pressureResponse);
        
        // Save to Firebase
        usersRef.child(userId).child("testResults").child(String.valueOf(timestamp))
                .setValue(testResult)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Test results saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save test results", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showResultsSummary() {
        try {
            Log.d("HomeFragment", "Starting analysis process...");
            
            // Get the glucose value that was entered
            String glucoseValueStr = glucoseValueInput.getText().toString().trim();
            Log.d("HomeFragment", "Glucose value string: " + glucoseValueStr);
            
            float glucoseValue = Float.parseFloat(glucoseValueStr);
            Log.d("HomeFragment", "Parsed glucose value: " + glucoseValue);
            
            // Extract EMG data features from the chart
            Log.d("HomeFragment", "Extracting EMG features...");
            float[] emgFeatures = extractEmgFeatures();
            Log.d("HomeFragment", "EMG features extracted: " + 
                    "max=" + emgFeatures[0] + 
                    ", range=" + emgFeatures[1] + 
                    ", mean=" + emgFeatures[2] + 
                    ", stdDev=" + emgFeatures[3] + 
                    ", crossings=" + emgFeatures[4]);
            
            // Log sensory responses
            Log.d("HomeFragment", "Temperature response: " + temperatureResponse);
            Log.d("HomeFragment", "Pressure response: " + pressureResponse);
            
            // Create and show the analysis results dialog
            Log.d("HomeFragment", "Showing analysis results dialog...");
            showAnalysisResultsDialog(glucoseValue, emgFeatures, temperatureResponse, pressureResponse);
        } catch (Exception e) {
            Log.e("HomeFragment", "Error in showResultsSummary: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Analysis error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Extracts features from the EMG chart data for use in the prediction model.
     */
    private float[] extractEmgFeatures() {
        // Get the EMG data from the chart
        LineData lineData = emgChart.getData();
        if (lineData == null || lineData.getDataSetCount() == 0) {
            return new float[]{0, 0, 0, 0, 0}; // Default values if no data
        }
        
        LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(0);
        List<Entry> entries = dataSet.getValues();
        
        // Calculate basic features from the EMG data
        float maxAmplitude = 0;
        float minAmplitude = Float.MAX_VALUE;
        float sumAmplitude = 0;
        float sumSquaredDiff = 0;
        int crossings = 0;
        float prevValue = entries.get(0).getY();
        float baseline = 20f; // The baseline value used in generateSampleEmgData
        
        for (Entry entry : entries) {
            float value = entry.getY();
            
            // Update max and min
            maxAmplitude = Math.max(maxAmplitude, value);
            minAmplitude = Math.min(minAmplitude, value);
            
            // Sum for mean calculation
            sumAmplitude += value;
            
            // Count baseline crossings
            if ((prevValue < baseline && value >= baseline) || 
                (prevValue >= baseline && value < baseline)) {
                crossings++;
            }
            
            prevValue = value;
        }
        
        // Calculate mean
        float meanAmplitude = sumAmplitude / entries.size();
        
        // Calculate standard deviation
        for (Entry entry : entries) {
            float diff = entry.getY() - meanAmplitude;
            sumSquaredDiff += diff * diff;
        }
        float stdDeviation = (float) Math.sqrt(sumSquaredDiff / entries.size());
        
        // Return extracted features
        return new float[]{
            maxAmplitude,
            maxAmplitude - minAmplitude, // Range
            meanAmplitude,
            stdDeviation,
            crossings
        };
    }

    /**
     * Shows a dialog with the analysis results and recommendations.
     */
    private void showAnalysisResultsDialog(float glucoseValue, float[] emgFeatures, 
                                          boolean hasTemperatureSensation, boolean hasPressureSensation) {
        try {
            Log.d("HomeFragment", "Starting analysis dialog creation...");
            
            // Create the predictor
            Log.d("HomeFragment", "Creating NeuropathyPredictor...");
            NeuropathyPredictor predictor = new NeuropathyPredictor(requireContext());
            Log.d("HomeFragment", "NeuropathyPredictor created successfully");
            
            // Create input features for the model
            Log.d("HomeFragment", "Creating model features...");
            float[] modelFeatures = createModelFeatures(glucoseValue, emgFeatures, 
                                                      hasTemperatureSensation, hasPressureSensation);
            Log.d("HomeFragment", "Model features created successfully");
            
            // Get prediction
            Log.d("HomeFragment", "Running prediction...");
            // Log the features we're sending to the model
            StringBuilder featureLog = new StringBuilder("Model features: ");
            for (int i = 0; i < modelFeatures.length; i++) {
                featureLog.append(modelFeatures[i]);
                if (i < modelFeatures.length - 1) featureLog.append(", ");
            }
            Log.d("HomeFragment", featureLog.toString());
            
            float prediction = predictor.predict(modelFeatures);
            Log.d("HomeFragment", "Prediction result: " + prediction + 
                  ", Used real model: " + predictor.usedRealModel());
            
            // Evaluate risk
            Log.d("HomeFragment", "Evaluating risk...");
            NeuropathyPredictor.RiskAssessment assessment = 
                    predictor.evaluateRisk(prediction, glucoseValue, 
                                         hasTemperatureSensation, hasPressureSensation);
            Log.d("HomeFragment", "Risk level: " + assessment.getRiskLevel());
            Log.d("HomeFragment", "Risk score: " + assessment.getPredictionScore());
            
            // Create dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_analysis_results, null);
            
            // Set up dialog views
            TextView riskLevelText = dialogView.findViewById(R.id.text_risk_level);
            TextView riskScoreText = dialogView.findViewById(R.id.text_risk_score);
            TextView riskDescriptionText = dialogView.findViewById(R.id.text_risk_description);
            TextView glucoseValueText = dialogView.findViewById(R.id.text_glucose_value_summary);
            TextView temperatureSensationText = dialogView.findViewById(R.id.text_temperature_sensation);
            TextView pressureSensationText = dialogView.findViewById(R.id.text_pressure_sensation);
            TextView emgSummaryText = dialogView.findViewById(R.id.text_emg_summary);
            RecyclerView recommendationsRecycler = dialogView.findViewById(R.id.recycler_recommendations);
            Button closeButton = dialogView.findViewById(R.id.button_close_analysis);
            
            // Set up risk level information
            NeuropathyPredictor.RiskLevel riskLevel = assessment.getRiskLevel();
            String riskLevelStr = riskLevel.toString();
            int riskColor = Color.GREEN;
            
            switch (riskLevel) {
                case HIGH:
                    riskColor = Color.RED;
                    riskDescriptionText.setText("Based on your test results, your risk of diabetic neuropathy is high. " +
                            "Please consult with your healthcare provider as soon as possible.");
                    break;
                case MODERATE:
                    riskColor = Color.parseColor("#FFA500"); // Orange
                    riskDescriptionText.setText("Based on your test results, your risk of diabetic neuropathy is moderate. " +
                            "Discuss these results with your healthcare provider.");
                    break;
                case LOW:
                    riskDescriptionText.setText("Based on your test results, your risk of diabetic neuropathy is low. " +
                            "Continue with your regular diabetes management plan.");
                    break;
            }
            
            // Add indicator showing whether real model or fallback algorithm was used
            TextView modelSourceText = dialogView.findViewById(R.id.text_model_source);
            if (modelSourceText != null) {
                boolean usedRealModel = assessment.usedRealModel();
                modelSourceText.setVisibility(View.VISIBLE);
                if (usedRealModel) {
                    modelSourceText.setText("✓ Analysis by ML Model");
                    modelSourceText.setTextColor(Color.rgb(0, 128, 0)); // Dark Green
                    Log.d("HomeFragment", "UI showing: Analysis by ML Model");
                } else {
                    modelSourceText.setText("⚠ Using Fallback Algorithm");
                    modelSourceText.setTextColor(Color.rgb(255, 140, 0)); // Dark Orange
                    Log.d("HomeFragment", "UI showing: Using Fallback Algorithm");
                }
                Log.d("HomeFragment", "Using real ML model: " + usedRealModel + ", Risk score: " + assessment.getPredictionScore());
                Log.d("HomeFragment", "Using real ML model: " + usedRealModel);
            }
            
            riskLevelText.setText("Risk Level: " + riskLevelStr);
            riskLevelText.setTextColor(riskColor);
            riskScoreText.setText("Risk Score: " + String.format("%.2f", assessment.getPredictionScore()));
            
            // Set up test data summary
            glucoseValueText.setText("Glucose Value: " + glucoseValue + " mg/dL");
            temperatureSensationText.setText("Temperature Sensation: " + (hasTemperatureSensation ? "Yes" : "No"));
            pressureSensationText.setText("Pressure Sensation: " + (hasPressureSensation ? "Yes" : "No"));
            
            // EMG summary based on features
            String emgSummary = "EMG Analysis: ";
            if (emgFeatures[1] > 30) { // Range
                emgSummary += "High variability pattern";
            } else if (emgFeatures[4] > 10) { // Crossings
                emgSummary += "Frequent oscillation pattern";
            } else {
                emgSummary += "Normal pattern";
            }
            emgSummaryText.setText(emgSummary);
            
            // Set up recommendations recycler
            recommendationsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            RecommendationAdapter adapter = new RecommendationAdapter(assessment.getRecommendations());
            recommendationsRecycler.setAdapter(adapter);
            
            // Set up close button
            closeButton.setOnClickListener(v -> {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
            
            // Show dialog
            builder.setView(dialogView);
            dialog = builder.create();
            dialog.show();
            
            // Close the predictor when done
            predictor.close();
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error analyzing results: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error analyzing results: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private float[] createModelFeatures(float glucoseValue, float[] emgFeatures, 
                                       boolean hasTemperatureSensation, boolean hasPressureSensation) {
        // In a real implementation, we would need to match the exact feature set expected by the model
        // This is a simplified version that uses the available data
        
        // Use default values for age and diabetes duration
        // We're not trying to get actual values from Firebase here to avoid lambda issues
        // In a real implementation, you would want to fetch these values earlier and store them
        final float age = 50;
        final float diabetesDuration = 5;
        
        // Create features array
        // Order: age, diabetes_duration, fasting_sugar, emg_features (5), sensory_features (2)
        return new float[]{
            age,
            diabetesDuration,
            glucoseValue,
            emgFeatures[0], // Max amplitude
            emgFeatures[1], // Range
            emgFeatures[2], // Mean
            emgFeatures[3], // Standard deviation
            emgFeatures[4], // Crossings
            hasTemperatureSensation ? 1f : 0f,
            hasPressureSensation ? 1f : 0f
        };
    }
    
    // Model class for glucose readings
    public static class GlucoseReading {
        public float value;
        public long timestamp;
        
        public GlucoseReading() {
            // Required empty constructor for Firebase
        }
        
        public GlucoseReading(float value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
    
    // Model class for test results
    public static class TestResult {
        public long timestamp;
        public boolean temperatureSensation;
        public boolean pressureSensation;
        
        public TestResult() {
            // Required empty constructor for Firebase
        }
        
        public TestResult(long timestamp, boolean temperatureSensation, boolean pressureSensation) {
            this.timestamp = timestamp;
            this.temperatureSensation = temperatureSensation;
            this.pressureSensation = pressureSensation;
        }
    }
}
