package com.example.emsimarkpresence;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

        FloatingActionButton fab = findViewById(R.id.fabAddClass);
        fab.setOnClickListener(v -> {
            // Launch your “add a class” screen
            Intent intent = new Intent(ViewClassesActivity.this, ClassManagementActivity.class);
            startActivity(intent);
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClassAdapter(this);
        recyclerView.setAdapter(adapter);

        loadClasses();

        adapter.setOnClassActionListener(new ClassAdapter.OnClassActionListener() {
            @Override
            public void onEditClass(ClassModel classModel) {
                // Handle edit logic
                String userId = mAuth.getCurrentUser().getUid();
                db.collection("users").document(userId).collection("classes")
                        .whereEqualTo("className", classModel.getClassName())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                String documentId = querySnapshot.getDocuments().get(0).getId();
                                EditClassDialog dialog = new EditClassDialog(
                                        ViewClassesActivity.this,
                                        classModel,
                                        documentId
                                );
                                dialog.setOnClassUpdatedListener(() -> loadClasses());
                                dialog.show();
                            }
                        });
            }

            @Override
            public void onDeleteClass(ClassModel classModel) {
                showDeleteConfirmationDialog(classModel);
            }
            @Override
            public void onStatusChanged(ClassModel classModel) {
                // Empty implementation if not needed
            }
        });
    }

    private void showDeleteConfirmationDialog(ClassModel classModel) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to delete '" + classModel.getClassName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteClass(classModel))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteClass(ClassModel classModel) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Deleting class...");
        progress.show();

        String userId = mAuth.getCurrentUser().getUid();
        String classId = classModel.getId();

        // First remove from all groups
        GroupManager.unlinkClassFromAllGroups(classId, userId)
                .addOnSuccessListener(aVoid -> {
                    // Then delete the class document
                    db.collection("users").document(userId)
                            .collection("classes").document(classId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                progress.dismiss();
                                Toast.makeText(this, "Class deleted", Toast.LENGTH_SHORT).show();
                                loadClasses();
                            })
                            .addOnFailureListener(e -> {
                                progress.dismiss();
                                Toast.makeText(this, "Failed to delete class", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to remove class from groups", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadClasses() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ClassModel> classes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classModel.setId(document.getId());
                        classes.add(classModel);
                    }
                    adapter.setClasses(classes);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load classes", Toast.LENGTH_SHORT).show();
                });
    }
}