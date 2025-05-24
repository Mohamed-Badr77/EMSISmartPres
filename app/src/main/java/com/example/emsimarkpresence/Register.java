package com.example.emsimarkpresence;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.*;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.activity.EdgeToEdge;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    private EditText RegFirstName, RegLastName, RegEmail, RegPassword, RegConPassword;
    private Button btnRegister;
    private TextView linkToLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    String userId;
    private ImageView registerProfileImage;
    private Button btnUploadPhoto;
    private Uri imageUri;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        RegEmail = findViewById(R.id.RegisterEmail);
        RegFirstName = findViewById(R.id.FirstName);
        RegLastName = findViewById(R.id.LastName);
        RegPassword = findViewById(R.id.RegisterPassword);
        RegConPassword = findViewById(R.id.ConfirmPassword);
        linkToLogin = findViewById(R.id.LoginPage);
        btnRegister =findViewById(R.id.btn_register);
        storageRef = FirebaseStorage.getInstance().getReference("profile_pictures");
        registerProfileImage = findViewById(R.id.registerProfileImage);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);

        btnRegister.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                registerUser();
            }
        });
        linkToLogin.setOnClickListener(v->{
            Intent intent = new Intent(Register.this,AuthentifyYourself.class);
            startActivity(intent);
        });
        btnUploadPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });
    }
    private void registerUser(){
        String nom = RegFirstName.getText().toString().trim();
        String prenom = RegLastName.getText().toString().trim();
        String email = RegEmail.getText().toString().trim();
        String passwordone = RegPassword.getText().toString().trim();
        String passwordtwo = RegConPassword.getText().toString().trim();
        String fullname = nom + " " + prenom;


        if(nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || passwordone.isEmpty() || passwordtwo.isEmpty()){
            Toast.makeText(this,"Veuillez remplir tout les champs",Toast.LENGTH_SHORT).show();
            return;
        }else{
            if(!passwordone.equals(passwordtwo)){
                Toast.makeText(this,"Mot de passes ne sont pas identiques!",Toast.LENGTH_SHORT).show();
                return;
            }else{
                /*Ajouter vérification d email pro*/
                mAuth.createUserWithEmailAndPassword(email,passwordone).addOnCompleteListener(this, task ->{
                    if(task.isSuccessful()) {
                        Toast.makeText(this, "Inscription réussie", Toast.LENGTH_SHORT).show();
                        userId = mAuth.getCurrentUser().getUid();
                        store_user_firestore(userId,email,fullname);
                        Intent i=new Intent(Register.this, AuthentifyYourself.class);
                        i.putExtra("name",fullname);
                        startActivity(i);
                        finish();
                    } else {
                        Toast.makeText(this, "Erreur: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            registerProfileImage.setImageURI(imageUri);
        }
    }

    private void store_user_firestore(String uid, String email, String name){
        if (imageUri != null) {
            StorageReference fileRef = storageRef.child(uid + ".jpg");
            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveUserData(uid, email, name, uri.toString());
                });
            });
        } else {
            saveUserData(uid, email, name, null);
        }
    }

    private void saveUserData(String uid, String email, String name, String imageUrl) {
        Map<String, Object> user = new HashMap<>();
        user.put("user_email", email);
        user.put("date_inscription", new Timestamp(new Date()));
        user.put("name", name);
        user.put("firstName", RegFirstName.getText().toString());
        user.put("lastName", RegLastName.getText().toString());
        user.put("phone", ((EditText)findViewById(R.id.PhoneNumber)).getText().toString());

        // Only store the URL if an image was uploaded
        if (imageUrl != null) {
            user.put("profileImageUrl", imageUrl);
        }
        // Otherwise, no profileImageUrl field will be created

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(Register.this, "Bienvenue " + name, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Register.this, AuthentifyYourself.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Register.this, "Failed to create user", Toast.LENGTH_SHORT).show();
                });
    }
}