import streamlit as st
import numpy as np
import joblib
import base64
from sklearn.preprocessing import StandardScaler

# Load the trained model
model = joblib.load("final.sav")

scaler = joblib.load("scaler.pkl")

# Background Image Setup
def set_bg(image_file):
    with open(image_file, "rb") as f:
        encoded = base64.b64encode(f.read()).decode()
    return f"data:image/jpeg;base64,{encoded}"

bg_image = set_bg("images (1).jpeg")  

st.markdown(
    f"""
    <style>
    .stApp {{
        background-image: url("{bg_image}");
        background-size: cover;
        background-position: center;
    }}
    h1, h2, h3, h4, h5, h6, p, label {{
        color: black !important;
        font-weight: bold !important;
    }}
    </style>
    """,
    unsafe_allow_html=True
)

# App Title
st.title("🦶 Diabetic Neuropathy Prediction App")
st.write("Enter patient details to predict the likelihood of Diabetic Neuropathy.")

# User Inputs
age = st.number_input("Age", min_value=20, max_value=100, value=50)
gender = st.selectbox("Gender", ["Male", "Female"])
diabetes_duration = st.number_input("Diabetes Duration (years)", min_value=0, max_value=50, value=10)
fasting_sugar = st.number_input("Fasting Blood Sugar (mg/dL)", min_value=50, max_value=300, value=100)
insulin_level = st.number_input("Insulin Level (µU/mL)", min_value=2, max_value=50, value=10)

# EMG & Nerve Signals
emg_freq = st.number_input("EMG Signal Frequency (Hz)", min_value=80, max_value=150, value=120)
emg_amp = st.number_input("EMG Amplitude (mV)", min_value=2.0, max_value=5.0, value=3.5)
motor_ncv = st.number_input("Motor Nerve Conduction Velocity (m/s)", min_value=25, max_value=50, value=40)
sensory_ncv = st.number_input("Sensory Nerve Conduction Velocity (m/s)", min_value=20, max_value=45, value=35)
f_wave = st.number_input("F-Wave Latency (ms)", min_value=30, max_value=55, value=40)
emg_duration = st.number_input("EMG Signal Duration (ms)", min_value=5, max_value=20, value=10)
resting_emg = st.number_input("Resting EMG Activity (µV)", min_value=10, max_value=35, value=20)

# Muscle Affected
muscle_affected = st.selectbox("Muscle Affected", ["Tibialis Anterior", "Gastrocnemius", "Quadriceps"])

# ✅ Added Missing Features
polyphasic_potential = st.selectbox("Polyphasic Potential (Yes/No)", ["No", "Yes"])
neuropathy_severity = st.slider("Neuropathy Severity (0 = None, 3 = Severe)", min_value=0, max_value=3, value=1)

# Encoding Categorical Variables
gender_encoded = 1 if gender == "Male" else 0
muscle_encoded = ["Tibialis Anterior", "Gastrocnemius", "Quadriceps"].index(muscle_affected)
polyphasic_potential_encoded = 1 if polyphasic_potential == "Yes" else 0

# Create Input Data (Now with 15 Features)
input_data = np.array([[age, diabetes_duration, fasting_sugar, insulin_level, emg_freq, emg_amp, 
                        motor_ncv, sensory_ncv, f_wave, emg_duration, resting_emg, gender_encoded, 
                        muscle_encoded, polyphasic_potential_encoded, neuropathy_severity]])

# Scale Input Using Pre-Trained Scaler
input_data = scaler.transform(input_data)

# Prediction Button
if st.button("Predict Neuropathy"):
    try:
        prediction = model.predict_proba(input_data)[:, 1]  # Use probability if supported
    except AttributeError:
        prediction = model.predict(input_data)  # Use standard prediction otherwise
    
    st.write(f"🔍 Raw Model Output: {prediction[0]}")  # Debugging

    # Adjust threshold based on risk factors
    threshold = 0.5  # Default

    if fasting_sugar > 140:
        threshold -= 0.1  # More aggressive detection for high sugar

    if insulin_level > 35:
        threshold -= 0.1  # More aggressive detection for insulin resistance

    if neuropathy_severity >= 2:
        threshold -= 0.1  # If neuropathy is moderate to severe, lower threshold

    result = "🟥 High Risk of Neuropathy" if prediction[0] > threshold else "🟩 Low Risk of Neuropathy"
    st.subheader(f"**Prediction: {result}**")