package com.dianerverotect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameEditText, familyNameEditText, addressEditText, ageEditText, durationEditText, typeEditText, weightEditText;
    private RadioGroup genderRadioGroup;
    private Button saveProfileButton, selectImageButton;
    private CircleImageView profileImageView;

    private DatabaseReference usersRef;
    private String userId;
    private Uri selectedImageUri = null;
    private String encodedImage = null;
    
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Display the selected image
                        Glide.with(this)
                                .load(selectedImageUri)
                                .centerCrop()
                                .into(profileImageView);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        nameEditText = findViewById(R.id.name_edit_text);
        familyNameEditText = findViewById(R.id.family_name_edit_text);
        addressEditText = findViewById(R.id.address_edit_text);
        ageEditText = findViewById(R.id.age_edit_text);
        durationEditText = findViewById(R.id.duration_edit_text);
        typeEditText = findViewById(R.id.type_edit_text);
        weightEditText = findViewById(R.id.weight_edit_text);
        genderRadioGroup = findViewById(R.id.gender_radio_group);
        saveProfileButton = findViewById(R.id.save_profile_button);
        selectImageButton = findViewById(R.id.select_image_button);
        profileImageView = findViewById(R.id.profile_image_view);

        saveProfileButton.setOnClickListener(v -> saveProfile());
        selectImageButton.setOnClickListener(v -> openImagePicker());

        loadProfileData();
    }

    private void loadProfileData() {
        usersRef.child(userId).child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    UserProfile profile = snapshot.getValue(UserProfile.class);
                    if (profile != null) {
                        nameEditText.setText(profile.name);
                        familyNameEditText.setText(profile.familyName);
                        addressEditText.setText(profile.address);
                        ageEditText.setText(profile.age);
                        durationEditText.setText(profile.duration);
                        typeEditText.setText(profile.type);
                        weightEditText.setText(profile.weight);

                        if ("Male".equals(profile.gender)) {
                            genderRadioGroup.check(R.id.gender_male);
                        } else if ("Female".equals(profile.gender)) {
                            genderRadioGroup.check(R.id.gender_female);
                        }
                        
                        // Load profile image if it exists
                        if (profile.profileImageUrl != null && !profile.profileImageUrl.isEmpty()) {
                            try {
                                // Decode Base64 string to bitmap
                                byte[] decodedString = Base64.getDecoder().decode(profile.profileImageUrl);
                                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                
                                // Load bitmap into ImageView
                                profileImageView.setImageBitmap(decodedBitmap);
                            } catch (Exception e) {
                                Log.e("ProfileActivity", "Error decoding image: " + e.getMessage());
                                // If there's an error, try loading with Glide as fallback
                                Glide.with(ProfileActivity.this)
                                        .load(profile.profileImageUrl)
                                        .centerCrop()
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .into(profileImageView);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void saveProfile() {
        String name = nameEditText.getText().toString().trim();
        String familyName = familyNameEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String age = ageEditText.getText().toString().trim();
        String duration = durationEditText.getText().toString().trim();
        String type = typeEditText.getText().toString().trim();
        String weight = weightEditText.getText().toString().trim();

        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        String gender = "";
        if (selectedGenderId == R.id.gender_male) {
            gender = "Male";
        } else if (selectedGenderId == R.id.gender_female) {
            gender = "Female";
        }

        // Create a temporary profile without the image URL
        UserProfile profile = new UserProfile(name, familyName, address, gender, age, duration, type, weight);

        if (selectedImageUri != null) {
            // Upload image first, then save profile with image URL
            uploadImageAndSaveProfile(profile);
        } else {
            // Check if there's an existing profile image URL to preserve
            usersRef.child(userId).child("profile").child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String existingImageUrl = snapshot.exists() ? snapshot.getValue(String.class) : null;
                    profile.profileImageUrl = existingImageUrl;
                    saveProfileToDatabase(profile);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // If we can't get the existing URL, just save without it
                    saveProfileToDatabase(profile);
                }
            });
        }
    }

    private void uploadImageAndSaveProfile(UserProfile profile) {
        try {
            if (selectedImageUri != null) {
                // Convert the selected image to a Base64 string
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                
                // Resize the bitmap to reduce storage size
                Bitmap resizedBitmap = getResizedBitmap(bitmap, 500); // Max 500px width/height
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageData = baos.toByteArray();
                
                // Encode the image data as Base64
                encodedImage = Base64.getEncoder().encodeToString(imageData);
                
                // Create a new profile with the encoded image
                UserProfile updatedProfile = new UserProfile(
                    profile.name, profile.familyName, profile.address, 
                    profile.gender, profile.age, profile.duration, 
                    profile.type, profile.weight, encodedImage);
                
                // Save to database
                saveProfileToDatabase(updatedProfile);
                
                Log.d("ProfileActivity", "Image encoded successfully");
            } else {
                saveProfileToDatabase(profile); // Save profile without image
            }
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error encoding image: " + e.getMessage());
            Toast.makeText(ProfileActivity.this, "Failed to process image", Toast.LENGTH_SHORT).show();
            saveProfileToDatabase(profile); // Save profile without image
        }
    }
    
    private Bitmap getResizedBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            // Width is greater than height
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            // Height is greater than width
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void saveProfileToDatabase(UserProfile profile) {
        usersRef.child(userId).child("profile").setValue(profile)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class UserProfile {
        public String name, familyName, address, gender, age, duration, type, weight, profileImageUrl;

        public UserProfile() {}

        public UserProfile(String name, String familyName, String address, String gender, String age, String duration, String type, String weight) {
            this.name = name;
            this.familyName = familyName;
            this.address = address;
            this.gender = gender;
            this.age = age;
            this.duration = duration;
            this.type = type;
            this.weight = weight;
            this.profileImageUrl = null; // Initialize as null
        }
        
        public UserProfile(String name, String familyName, String address, String gender, String age, String duration, String type, String weight, String profileImageUrl) {
            this.name = name;
            this.familyName = familyName;
            this.address = address;
            this.gender = gender;
            this.age = age;
            this.duration = duration;
            this.type = type;
            this.weight = weight;
            this.profileImageUrl = profileImageUrl;
        }
    }
}
