package com.example.emsimarkpresence;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewClassesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ClassAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_classes);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClassAdapter();
        recyclerView.setAdapter(adapter);

        // Load classes from Firestore
        loadClasses();
    }

    private void loadClasses() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ClassModel> classes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classes.add(classModel);
                    }
                    adapter.setClasses(classes);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load classes", Toast.LENGTH_SHORT).show();
                });
    }
}