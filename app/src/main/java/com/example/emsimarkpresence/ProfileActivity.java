package com.example.emsimarkpresence;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import android.Manifest;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;


import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;


public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private EditText etFirstName, etLastName;
    private TextView etEmail, etPhone;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private StorageReference storageRef;
    private Uri imageUri;

    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_READ_STORAGE_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        // Initialize views
        profileImage = findViewById(R.id.profile_image);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);

        // Add at the start of onCreate()
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (FirebaseApp.getApps(this).isEmpty()) {
            Toast.makeText(this, "Firebase not initialized!", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Firebase App not initialized");
            return;
        }

        // Load user data
        loadUserData();

        // Profile picture change
        findViewById(R.id.btn_change_photo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        // Phone change (launches new activity)
//        etPhone.setOnClickListener(v -> {
//            startActivity(new Intent(this, ChangePhoneActivity.class));
//        });
//
//        // Password change
//        findViewById(R.id.btn_change_password).setOnClickListener(v -> {
//            startActivity(new Intent(this, ChangePasswordActivity.class));
//        });

        // Save changes
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            saveUserData();
        });

        findViewById(R.id.btn_change_photo).setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openImagePicker();
            }
        });
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_READ_STORAGE_PERMISSION);
                return false;
            }
        } else {
            // For older versions use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_STORAGE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private void loadUserData() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Set fields with null checks
                            etFirstName.setText(document.getString("firstName") != null ?
                                    document.getString("firstName") : "");
                            etLastName.setText(document.getString("lastName") != null ?
                                    document.getString("lastName") : "");
                            etEmail.setText(currentUser.getEmail() != null ?
                                    currentUser.getEmail() : "");

                            String phone = document.getString("phone");
                            etPhone.setText(phone != null ? phone : "");
                            etPhone.setHint(phone != null ? "" : "Add phone number");

                            // Load profile image
                            String imageUrl = document.getString("profileImageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_logo)
                                        .into(profileImage);
                            }
                        } else {
                            Toast.makeText(this, "User document doesn't exist", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load data: " +
                                        (task.getException() != null ?
                                                task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                        Log.e("ProfileActivity", "Error loading data", task.getException());
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void saveUserData() {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", etFirstName.getText().toString());
        user.put("lastName", etLastName.getText().toString());

        db.collection("users").document(currentUser.getUid())
                .update(user)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
            uploadImage();
        }
    }



    private void uploadImage() {
        if (imageUri != null) {
            StorageReference fileRef = storageRef.child(currentUser.getUid() + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Update Firestore with new image URL
                            db.collection("users").document(currentUser.getUid())
                                    .update("profileImageUrl", uri.toString());
                        });
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied - cannot access images", Toast.LENGTH_SHORT).show();
            }
        }
    }
}