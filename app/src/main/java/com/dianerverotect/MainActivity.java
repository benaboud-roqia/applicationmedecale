package com.dianerverotect;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast; // Import Toast

import androidx.activity.EdgeToEdge; // Keep EdgeToEdge if still desired, though AppBarLayout might handle some aspects
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
// Removed ViewCompat and WindowInsetsCompat imports as we'll remove the listener for now
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.Toolbar; // Import Toolbar

import com.google.android.material.appbar.MaterialToolbar; // Import MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Base64;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private MaterialToolbar topAppBar; // Changed to MaterialToolbar
    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FragmentManager fragmentManager;
    private Fragment activeFragment;
    
    // Fragment instances
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment; // Added HistoryFragment instance
    private SettingsFragment settingsFragment;

    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Can be kept or removed depending on desired edge-to-edge behavior with AppBar
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadDrawerHeaderData();

        // --- Setup Toolbar ---
        topAppBar = findViewById(R.id.top_app_bar);
        setSupportActionBar(topAppBar);
        // Enable the navigation icon (hamburger)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Use the custom ic_menu drawable provided by the user
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu); // Changed to use custom drawable
            getSupportActionBar().setTitle("DiaNerveProtect"); // Keep the title
        }
        // ---------------------

        // Initialize fragments
        homeFragment = new HomeFragment();
        historyFragment = new HistoryFragment(); // Initialize HistoryFragment
        settingsFragment = new SettingsFragment();
        
        // Set the initial fragment
        fragmentManager = getSupportFragmentManager();
        activeFragment = homeFragment;
        
        // Add all fragments and hide all except the active one
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, homeFragment, "home");
        transaction.add(R.id.fragment_container, historyFragment, "history").hide(historyFragment); // Add HistoryFragment, hidden
        transaction.add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment);
        transaction.commit();

        // Initialize the bottom navigation view
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);
        
        // Set the default selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // Handle navigation drawer item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new android.content.Intent(MainActivity.this, ProfileActivity.class));
            } else if (id == R.id.nav_settings) {
                Toast.makeText(MainActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    // Removed onCreateOptionsMenu as we are not using the right-side options menu anymore

    private void loadDrawerHeaderData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference profileRef = usersRef.child(userId);

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = "";
                String email = "";
                if (snapshot.child("fullName").exists()) {
                    username = snapshot.child("fullName").getValue(String.class);
                }
                if (snapshot.child("email").exists()) {
                    email = snapshot.child("email").getValue(String.class);
                }

                // Access header views
                android.view.View headerView = navigationView.getHeaderView(0);
                TextView usernameText = headerView.findViewById(R.id.drawer_username);
                TextView emailText = headerView.findViewById(R.id.drawer_email);
                CircleImageView profileImage = headerView.findViewById(R.id.drawer_profile_image);

                usernameText.setText(username);
                emailText.setText(email);
                
                // Load profile image if available
                if (snapshot.child("profile").exists() && 
                    snapshot.child("profile").child("profileImageUrl").exists()) {
                    String imageData = snapshot.child("profile").child("profileImageUrl").getValue(String.class);
                    if (imageData != null && !imageData.isEmpty()) {
                        try {
                            // Try to decode as Base64
                            byte[] decodedString = Base64.getDecoder().decode(imageData);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            
                            if (decodedBitmap != null) {
                                // Successfully decoded as Base64
                                Log.d("MainActivity", "Successfully loaded profile image from Base64");
                                profileImage.setImageBitmap(decodedBitmap);
                            } else {
                                // Fallback to Glide
                                Log.d("MainActivity", "Failed to decode Base64, trying as URL");
                                Glide.with(MainActivity.this)
                                        .load(imageData)
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .error(R.drawable.ic_launcher_foreground)
                                        .centerCrop()
                                        .into(profileImage);
                            }
                        } catch (Exception e) {
                            // If Base64 decoding fails, try as URL
                            Log.e("MainActivity", "Error loading profile image: " + e.getMessage());
                            Glide.with(MainActivity.this)
                                    .load(imageData)
                                    .placeholder(R.drawable.ic_launcher_foreground)
                                    .error(R.drawable.ic_launcher_foreground)
                                    .centerCrop()
                                    .into(profileImage);
                        }
                    } else {
                        Log.d("MainActivity", "Profile image data is null or empty");
                    }
                } else {
                    Log.d("MainActivity", "Profile image URL node doesn't exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore for now
            }
        });
    }

    // --- Handle Toolbar Navigation/Menu Item Clicks ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle the navigation icon (hamburger) click
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(android.view.Gravity.START);
            return true;
        }
        // Handle other menu items if they were added programmatically or via fragments
        return super.onOptionsItemSelected(item);
    }
    // -------------------------------------

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.navigation_home) {
            switchFragment(homeFragment);
            return true;
        } else if (itemId == R.id.navigation_history) { // Added case for History
            switchFragment(historyFragment);
            return true;
        } else if (itemId == R.id.navigation_settings) {
            switchFragment(settingsFragment);
            return true;
        }
        
        return false;
    }
    
    private void switchFragment(Fragment fragment) {
        if (fragment != activeFragment) {
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(fragment)
                    .commit();
            activeFragment = fragment;
        }
    }
}
