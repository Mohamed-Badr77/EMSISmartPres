package com.example.emsimarkpresence;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

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
        // New version with proper constructor
        adapter = new StudentAdapter(
                this,               // Context
                students,           // List<Student>
                null,               // No removal listener
                new StudentAdapter.OnStudentClickListener() {
                    @Override
                    public void onStudentClick(Student student) {
                        showEditStudentDialog(student);
                    }

                    @Override
                    public void onStudentLongClick(Student student) {
                        showDeleteConfirmation(student);
                    }
                }
        );

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
                    List<Student> tempList = new ArrayList<>(); // Create temporary list
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Student student = document.toObject(Student.class);
                        if (student != null) {
                            student.setId(document.getId());
                            tempList.add(student);
                        }
                    }
                    // Update both the activity's list and adapter
                    students.clear();
                    students.addAll(tempList);
                    adapter.updateStudents(tempList); // This should trigger notifyDataSetChanged()
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
                .setTitle("Nuclear Option")
                .setMessage("Delete " + student.getName() + " and scrub from ALL groups?")
                .setPositiveButton("NUKE IT", (dialog, which) -> {
                    // Show loading dialog
                    ProgressDialog progress = new ProgressDialog(this);
                    progress.setMessage("Terminating student existence...");
                    progress.setCancelable(false);
                    progress.show();

                    // Step 1: Remove from all groups
                    removeStudentFromAllGroups(student.getId())
                            .addOnCompleteListener(removeTask -> {
                                if (removeTask.isSuccessful()) {
                                    // Step 2: Delete student document
                                    deleteStudentDocument(student, progress);
                                } else {
                                    progress.dismiss();
                                    Toast.makeText(this, "Failed to remove from groups: " +
                                            removeTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("ABORT MISSION", null)
                .show();
    }

    private Task<Void> removeStudentFromAllGroups(String studentId) {
        // This query finds ALL groups containing this student
        return db.collection("groups")
                .whereEqualTo("students." + studentId, true)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Batch delete all references
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot groupDoc : task.getResult()) {
                        batch.update(groupDoc.getReference(),
                                "students." + studentId, FieldValue.delete());
                    }

                    Log.d("FIREBASE", "Removing " + studentId + " from " +
                            task.getResult().size() + " groups");

                    return batch.commit();
                });
    }

    private void deleteStudentDocument(Student student, ProgressDialog progress) {
        db.collection("students").document(student.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progress.dismiss();
                    Toast.makeText(this, "Student obliterated from existence", Toast.LENGTH_SHORT).show();
                    loadStudents(); // Refresh your list
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to delete student doc: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}