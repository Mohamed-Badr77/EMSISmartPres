package com.example.emsimarkpresence;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ClassManagementActivity extends AppCompatActivity {
    private EditText etClassName, etTotalHours;
    private LinearLayout groupContainer;
    private RadioGroup radioStatus;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_management);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etClassName = findViewById(R.id.etClassName);
        etTotalHours = findViewById(R.id.etTotalHours);
        groupContainer = findViewById(R.id.groupContainer);
        radioStatus = findViewById(R.id.radioStatus);

        // Add Group Button
        findViewById(R.id.btnAddGroup).setOnClickListener(v -> addGroupField());

        // Save Class Button
        findViewById(R.id.btnSaveClass).setOnClickListener(v -> saveClassToFirestore());
    }

    private void addGroupField() {
        EditText etGroup = new EditText(this);
        etGroup.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        etGroup.setHint("Group Name (e.g., Group A)");
        groupContainer.addView(etGroup);
    }

    private void saveClassToFirestore() {
        String className = etClassName.getText().toString().trim();
        String hoursStr = etTotalHours.getText().toString().trim();
        int totalHours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);

        // Get groups
        List<String> groups = new ArrayList<>();
        for (int i = 0; i < groupContainer.getChildCount(); i++) {
            View view = groupContainer.getChildAt(i);
            if (view instanceof EditText) {
                String groupName = ((EditText) view).getText().toString().trim();
                if (!groupName.isEmpty()) {
                    groups.add(groupName);
                }
            }
        }

        // Get status
        String status;
        int selectedId = radioStatus.getCheckedRadioButtonId();
        if (selectedId == R.id.radioActive) status = "Active";
        else if (selectedId == R.id.radioPaused) status = "Paused";
        else status = "Completed";

        // Validate input
        if (className.isEmpty() || totalHours <= 0 || groups.isEmpty()) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create ClassModel
        ClassModel newClass = new ClassModel(className, totalHours, groups, status);

        // Save to Firestore
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("classes")
                .add(newClass)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Class saved!", Toast.LENGTH_SHORT).show();
                    finish();  // Close activity after saving
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}