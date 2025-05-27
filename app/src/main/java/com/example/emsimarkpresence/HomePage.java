package com.example.emsimarkpresence;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

    // These are MaterialButtons in your XML
    private MaterialButton btnLogout, btnNotification;
    private MaterialButton btnClassManagement;

    // These are MaterialCardViews in your XML
    private MaterialCardView btnMap, btnAI;
    private MaterialCardView cardDocuments;
    private LinearLayout btnProfile, btnViewClasses, btnGroupManagement, btnStudentManagement;


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
        welcomeText = findViewById(R.id.tvUserName);

        // MaterialButtons
        btnProfile = findViewById(R.id.buttonProfile);

        btnLogout = findViewById(R.id.buttonLogout);
        btnNotification = findViewById(R.id.notificationButton);
        btnClassManagement = findViewById(R.id.btnClassManagement);
        btnViewClasses = findViewById(R.id.btnViewClasses);
        btnGroupManagement = findViewById(R.id.btnGroupManagement);
        btnStudentManagement = findViewById(R.id.btnStudentManagement);

        // MaterialCardViews
        btnAI = findViewById(R.id.buttonAI);
        btnMap = findViewById(R.id.buttonMap);
        cardDocuments = findViewById(R.id.documents);

        // ImageView
        profileImage = findViewById(R.id.profileImage);

        // Load user data and image
        loadUserData();
        loadProfileImage();

        // Set click listeners for all buttons
        setupButtonListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupButtonListeners() {
        // Profile button
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, ProfileActivity.class));
        });

        // Settings button
//        btnSettings.setOnClickListener(v -> {
//            startActivity(new Intent(HomePage.this, SettingsActivity.class));
//        });

        // AI Assistant button (MaterialCardView)
        btnAI.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, Assistant_virtuel.class));
        });

        // Map button (MaterialCardView)
        btnMap.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, MapsActivity.class));
        });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(HomePage.this, AuthentifyYourself.class));
            finish();
        });

        // Notification button
//        btnNotification.setOnClickListener(v -> {
//            startActivity(new Intent(HomePage.this, NotificationsActivity.class));
//        });

        // Class Management button
        btnClassManagement.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, ClassManagementActivity.class));
        });

        // View Classes button
        btnViewClasses.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, ViewClassesActivity.class));
        });

        // Profile Image click (if you want to make it clickable)
        profileImage.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, ProfileActivity.class));
        });

        btnGroupManagement.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, GroupManagementActivity.class));
        });

        btnStudentManagement.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, StudentManagementActivity.class));
        });

        // Documents card (MaterialCardView)
        cardDocuments.setOnClickListener(v -> {
            startActivity(new Intent(HomePage.this, DocumentsActivity.class));
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
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("profileImageUrl")) {
                            String imageUrl = documentSnapshot.getString("profileImageUrl");
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_logo)
                                    .error(R.drawable.default_logo)
                                    .circleCrop()
                                    .into(profileImage);
                        } else {
                            Glide.with(this)
                                    .load(R.drawable.default_logo)
                                    .circleCrop()
                                    .into(profileImage);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Glide.with(this)
                                .load(R.drawable.default_logo)
                                .circleCrop()
                                .into(profileImage);
                    });
        }
    }
}