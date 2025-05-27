package com.example.emsimarkpresence;

import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassManagementActivity extends AppCompatActivity {
    private EditText etClassName, etNumberOfWeeks, etHoursPerSession;
    private TextView tvTotalHours;
    private LinearLayout groupSelectionContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Group> availableGroups = new ArrayList<>();
    private Map<String, CheckBox> groupCheckBoxes = new HashMap<>();
    private boolean isEditMode = false;
    private ClassModel editingClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_management);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etClassName = findViewById(R.id.etClassName);
        etNumberOfWeeks = findViewById(R.id.etNumberOfWeeks);
        etHoursPerSession = findViewById(R.id.etHoursPerSession);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        groupSelectionContainer = findViewById(R.id.groupSelectionContainer);

        // Check if we're editing an existing class
        String editClassId = getIntent().getStringExtra("editClassId");
        if (editClassId != null) {
            isEditMode = true;
            loadClassForEditing(editClassId);
        }

        // Set default values
        etNumberOfWeeks.setText("7");
        etHoursPerSession.setText("1.5");

        // Load existing groups from Firestore
        loadAvailableGroups();

        // Add listeners for automatic total hours calculation
        etNumberOfWeeks.addTextChangedListener(new SimpleTextWatcher(this::updateTotalHours));
        etHoursPerSession.addTextChangedListener(new SimpleTextWatcher(this::updateTotalHours));

        // Save Class Button
        Button btnSave = findViewById(R.id.btnSaveClass);
        btnSave.setText(isEditMode ? "Update Class" : "Save Class");
        btnSave.setOnClickListener(v -> {
            if (isEditMode) {
                updateClassInFirestore();
            } else {
                saveClassToFirestore();
            }
        });

        // Initial calculation
        updateTotalHours();
    }

    private void loadAvailableGroups() {
        // Load groups from the global groups collection
        db.collection("groups")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    availableGroups.clear();
                    groupSelectionContainer.removeAllViews();
                    groupCheckBoxes.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Group group = doc.toObject(Group.class);
                        group.setId(doc.getId());
                        availableGroups.add(group);

                        // Create checkbox for each group
                        CheckBox checkBox = new CheckBox(this);
                        checkBox.setText(group.getName() + " (" + group.getCampus() + ")");
                        checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));

                        groupSelectionContainer.addView(checkBox);
                        groupCheckBoxes.put(group.getId(), checkBox);
                    }

                    // If in edit mode, populate the form with existing data
                    if (isEditMode && editingClass != null) {
                        populateFormForEditing();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading groups: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadClassForEditing(String classId) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("classes")
                .document(classId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editingClass = documentSnapshot.toObject(ClassModel.class);
                        if (editingClass != null) {
                            // Set the document ID for updating later
                            editingClass.setId(documentSnapshot.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading class: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void populateFormForEditing() {
        if (editingClass == null) return;

        etClassName.setText(editingClass.getClassName());
        etNumberOfWeeks.setText(String.valueOf(editingClass.getNumberOfWeeks()));
        etHoursPerSession.setText(String.valueOf(editingClass.getHoursPerSession()));

        // Select the appropriate groups
        if (editingClass.getGroupMap() != null) {
            for (Map.Entry<String, Boolean> entry : editingClass.getGroupMap().entrySet()) {
                CheckBox checkBox = groupCheckBoxes.get(entry.getKey());
                if (checkBox != null && entry.getValue()) {
                    checkBox.setChecked(true);
                }
            }
        }

        updateTotalHours();
    }

    private void updateClassInFirestore() {
        String className = etClassName.getText().toString().trim();
        String weeksStr = etNumberOfWeeks.getText().toString().trim();
        String hoursStr = etHoursPerSession.getText().toString().trim();

        // Validate input
        if (className.isEmpty() || weeksStr.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfWeeks;
        double hoursPerSession;
        try {
            numberOfWeeks = Integer.parseInt(weeksStr);
            hoursPerSession = Double.parseDouble(hoursStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected groups
        Map<String, Boolean> groupMap = new HashMap<>();
        boolean hasSelectedGroups = false;
        for (Map.Entry<String, CheckBox> entry : groupCheckBoxes.entrySet()) {
            if (entry.getValue().isChecked()) {
                groupMap.put(entry.getKey(), true);
                hasSelectedGroups = true;
            }
        }

        if (!hasSelectedGroups) {
            Toast.makeText(this, "Please select at least one group!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the existing class object
        editingClass.setClassName(className);
        editingClass.setGroupMap(groupMap);
        editingClass.setNumberOfWeeks(numberOfWeeks);
        editingClass.setHoursPerSession(hoursPerSession);
        // Status remains the same when updating basic info

        // Update in Firestore
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("classes")
                .document(editingClass.getId())
                .set(editingClass)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Class updated!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTotalHours() {
        try {
            String weeksStr = etNumberOfWeeks.getText().toString().trim();
            String hoursStr = etHoursPerSession.getText().toString().trim();

            if (!weeksStr.isEmpty() && !hoursStr.isEmpty()) {
                int weeks = Integer.parseInt(weeksStr);
                double hoursPerSession = Double.parseDouble(hoursStr);
                double totalHours = weeks * hoursPerSession;
                tvTotalHours.setText("Total Hours: " + totalHours);
            } else {
                tvTotalHours.setText("Total Hours: 0");
            }
        } catch (NumberFormatException e) {
            tvTotalHours.setText("Total Hours: Invalid input");
        }
    }

    private void saveClassToFirestore() {
        String className = etClassName.getText().toString().trim();
        String weeksStr = etNumberOfWeeks.getText().toString().trim();
        String hoursStr = etHoursPerSession.getText().toString().trim();

        // Validate input
        if (className.isEmpty() || weeksStr.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfWeeks;
        double hoursPerSession;
        try {
            numberOfWeeks = Integer.parseInt(weeksStr);
            hoursPerSession = Double.parseDouble(hoursStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected groups
        Map<String, Boolean> groupMap = new HashMap<>();
        boolean hasSelectedGroups = false;
        for (Map.Entry<String, CheckBox> entry : groupCheckBoxes.entrySet()) {
            if (entry.getValue().isChecked()) {
                groupMap.put(entry.getKey(), true);
                hasSelectedGroups = true;
            }
        }

        if (!hasSelectedGroups) {
            Toast.makeText(this, "Please select at least one group!", Toast.LENGTH_SHORT).show();
            return;
        }

        // New classes are always created as "Active"
        String status = "Active";

        // Create ClassModel
        ClassModel newClass = new ClassModel(className, groupMap, status, numberOfWeeks, hoursPerSession);

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

    // Simple TextWatcher helper class
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;

        public SimpleTextWatcher(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {
            callback.run();
        }
    }
}