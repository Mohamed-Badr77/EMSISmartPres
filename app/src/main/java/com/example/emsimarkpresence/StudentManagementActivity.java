package com.example.emsimarkpresence;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private List<Student> students = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_management);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up adapter with click listeners
        adapter = new StudentAdapter(students);
        adapter.setOnStudentClickListener(new StudentAdapter.OnStudentClickListener() {
            @Override
            public void onStudentClick(Student student) {
                showEditStudentDialog(student);
            }

            @Override
            public void onStudentLongClick(Student student) {
                showDeleteConfirmation(student);
            }
        });
        recyclerView.setAdapter(adapter);

        // Set up FAB
        findViewById(R.id.fabAddStudent).setOnClickListener(v -> showAddStudentDialog());

        // Set up back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadStudents();
    }

    private void loadStudents() {
        db.collection("students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    students.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Student student = document.toObject(Student.class);
                        if (student != null) {
                            student.setId(document.getId());
                            students.add(student);
                        }
                    }
                    adapter.updateStudents(students);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_student, null);
        builder.setView(dialogView);
        builder.setTitle("Add New Student");

        EditText etName = dialogView.findViewById(R.id.etStudentName);
        EditText etEmail = dialogView.findViewById(R.id.etStudentEmail);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Student newStudent = new Student();
            newStudent.setName(name);
            newStudent.setEmail(email);

            db.collection("students")
                    .add(newStudent)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
                        loadStudents();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showEditStudentDialog(Student student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_student, null);
        builder.setView(dialogView);
        builder.setTitle("Edit Student");

        EditText etName = dialogView.findViewById(R.id.etStudentName);
        EditText etEmail = dialogView.findViewById(R.id.etStudentEmail);

        // Pre-fill with current values
        etName.setText(student.getName());
        etEmail.setText(student.getEmail());

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("email", email);

            db.collection("students").document(student.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Student updated successfully", Toast.LENGTH_SHORT).show();
                        loadStudents();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void showDeleteConfirmation(Student student) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Student")
                .setMessage("Are you sure you want to delete " + student.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("students").document(student.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Student deleted", Toast.LENGTH_SHORT).show();
                                loadStudents();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}