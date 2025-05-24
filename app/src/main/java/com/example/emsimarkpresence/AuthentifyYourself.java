package com.example.emsimarkpresence;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import androidx.activity.EdgeToEdge;

public class AuthentifyYourself extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView linkToRegister;
    private FirebaseAuth mAuth;
    private final String validEmail = "user@example.com";
    private final String validPassword = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_authentify_yourself);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        linkToRegister = findViewById(R.id.textView2);
        etEmail = findViewById(R.id.Email);
        etPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                authenticateUser();
            }
        });
        linkToRegister.setOnClickListener(v->{
            Intent intent = new Intent(AuthentifyYourself.this,Register.class);
            startActivity(intent);
        });
    }

    private void authenticateUser(){
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this,"Veuillez remplir tout les champs",Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task->{
            if(task.isSuccessful()){
                Toast.makeText(this, "Authentification réussie!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AuthentifyYourself.this, HomePage.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
            }

        });
    }


}