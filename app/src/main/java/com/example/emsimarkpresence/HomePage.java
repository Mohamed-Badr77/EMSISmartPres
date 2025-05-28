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

    // These are MaterialCardViews in your XML
    private MaterialCardView btnMap, btnAI;
    private MaterialCardView cardDocuments;
    private MaterialCardView btnEmploi;
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
        btnViewClasses = findViewById(R.id.btnViewClasses);
        btnGroupManagement = findViewById(R.id.btnGroupManagement);
        btnStudentManagement = findViewById(R.id.btnStudentManagement);

        // MaterialCardViews
        btnAI = findViewById(R.id.buttonAI);
        btnMap = findViewById(R.id.buttonMap);
        cardDocuments = findViewById(R.id.documents);
        btnEmploi = findViewById(R.id.emploi);

        // Set up click listeners
        btnEmploi.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, TimetableActivity.class);
            startActivity(intent);
        });
    }
}
