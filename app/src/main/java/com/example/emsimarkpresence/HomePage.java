package com.example.emsimarkpresence;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomePage extends AppCompatActivity {
    // View declarations

    private ImageView profileImage;
    private TextView welcomeText;
    private Button btnProfile, btnSettings, btnAI, btnMap, btnLogout, btnNotification, btnClassManagement, btnViewClasses;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_page);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // Initialize views
        welcomeText = findViewById(R.id.welcome_text);
        btnProfile = findViewById(R.id.buttonProfile);
        btnSettings = findViewById(R.id.buttonSettings);
        btnAI = findViewById(R.id.buttonAI);
        btnMap = findViewById(R.id.buttonMap);
        btnLogout = findViewById(R.id.buttonLogout);
        btnNotification = findViewById(R.id.notificationButton);
        profileImage = findViewById(R.id.profileImage);
        btnClassManagement =  findViewById(R.id.btnClassManagement);
        btnViewClasses = findViewById(R.id.btnViewClasses);

        // Load user data
        loadUserData();

        // Load user Image

        loadProfileImage();

        // Set click listeners
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, ProfileActivity.class));
        });

        btnAI.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, Assistant_virtuel.class));
        });

        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, MapsActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(HomePage.this, AuthentifyYourself.class));
            finish();
        });

        btnClassManagement.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, ClassManagementActivity.class));
        });

        btnViewClasses.setOnClickListener(v ->{
            startActivity(new Intent(HomePage.this, ViewClassesActivity.class));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");

                            String welcomeMsg;
                            if (firstName != null && lastName != null) {
                                welcomeMsg = "Welcome, " + firstName + " " + lastName + "!";
                            } else if (firstName != null) {
                                welcomeMsg = "Welcome, " + firstName + "!";
                            } else {
                                welcomeMsg = "Welcome to EMSI Smart Presence!";
                            }

                            welcomeText.setText(welcomeMsg);

                            // Load profile image if available
                            String imageUrl = documentSnapshot.getString("profileImageUrl");
                            // You could add Glide here to load the image if you add an ImageView
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadProfileImage() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("profileImageUrl")) {
                        String imageUrl = documentSnapshot.getString("profileImageUrl");

                        // Load the actual profile image if URL exists
                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.default_logo) // shows while loading
                                .error(R.drawable.default_logo) // shows if loading fails
                                .circleCrop() // makes the image circular
                                .into(profileImage);
                    } else {
                        // No image exists in Firestore, load default from drawable
                        Glide.with(this)
                                .load(R.drawable.default_logo)
                                .circleCrop()
                                .into(profileImage);
                    }
                })
                .addOnFailureListener(e -> {
                    // If there's any error, load default from drawable
                    Glide.with(this)
                            .load(R.drawable.default_logo)
                            .circleCrop()
                            .into(profileImage);
                });
    }
}

