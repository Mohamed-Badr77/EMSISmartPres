package com.example.emsimarkpresence;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView unscheduledClassesRecyclerView;
    private UnscheduledClassAdapter adapter;
    private Map<String, MaterialCardView> timeSlots = new HashMap<>();
    private Map<String, ScheduledClass> scheduledClasses = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize RecyclerView for unscheduled classes
        unscheduledClassesRecyclerView = findViewById(R.id.unscheduledClassesRecyclerView);
        unscheduledClassesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UnscheduledClassAdapter(new ArrayList<>(), this::onClassSelected);
        unscheduledClassesRecyclerView.setAdapter(adapter);

        // Initialize time slots
        initializeTimeSlots();

        // Load data
        loadUnscheduledClasses();
        loadScheduledClasses();
    }

    private void initializeTimeSlots() {
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday"};
        for (String day : days) {
            View dayRow = findViewById(getResources().getIdentifier(day, "id", getPackageName()));
            if (dayRow != null) {
                // Set day name
                TextView dayName = dayRow.findViewById(R.id.dayName);
                dayName.setText(day.substring(0, 1).toUpperCase() + day.substring(1));

                // Initialize slots for this day
                for (int slot = 1; slot <= 4; slot++) {
                    MaterialCardView timeSlot = dayRow.findViewById(
                        getResources().getIdentifier("slot" + slot, "id", getPackageName())
                    );
                    String slotId = day + "_" + slot;
                    timeSlots.put(slotId, timeSlot);
                    timeSlot.setOnClickListener(v -> onTimeSlotClick(slotId));
                }
            }
        }
    }

    private void loadUnscheduledClasses() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("classes")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<ClassModel> classes = new ArrayList<>();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    ClassModel classModel = document.toObject(ClassModel.class);
                    classModel.setId(document.getId());
                    // Only add classes that haven't completed their total hours
                    if (!isClassComplete(classModel)) {
                        classes.add(classModel);
                    }
                }
                adapter.updateClasses(classes);
            });
    }

    private void loadScheduledClasses() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("schedule")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                scheduledClasses.clear();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    ScheduledClass scheduledClass = document.toObject(ScheduledClass.class);
                    scheduledClass.setId(document.getId());
                    scheduledClasses.put(scheduledClass.getSlotId(), scheduledClass);
                    updateTimeSlotView(scheduledClass);
                }
            });
    }

    private boolean isClassComplete(ClassModel classModel) {
        double scheduledHours = 0;
        for (ScheduledClass scheduled : scheduledClasses.values()) {
            if (scheduled.getClassId().equals(classModel.getId())) {
                scheduledHours += 2; // Each slot is 2 hours
            }
        }
        return scheduledHours >= classModel.getTotalHours();
    }

    private void updateTimeSlotView(ScheduledClass scheduledClass) {
        MaterialCardView slot = timeSlots.get(scheduledClass.getSlotId());
        if (slot != null) {
            TextView contentView = new TextView(this);
            contentView.setPadding(8, 8, 8, 8);
            contentView.setText(String.format("%s\n%s",
                scheduledClass.getClassName(),
                scheduledClass.getGroupNames()));
            slot.removeAllViews();
            slot.addView(contentView);
            slot.setCardBackgroundColor(getResources().getColor(R.color.green, null));
        }
    }

    private void onTimeSlotClick(String slotId) {
        if (scheduledClasses.containsKey(slotId)) {
            ScheduledClass scheduled = scheduledClasses.get(slotId);
            ScheduledClassDialog dialog = new ScheduledClassDialog(this, scheduled,
                scheduledClass -> deleteScheduledClass(scheduledClass));
            dialog.show();
        }
    }

    private void onClassSelected(ClassModel classModel) {
        // Show dialog to select time slot and groups
        // Implementation needed for TimeSlotSelectionDialog
        TimeSlotSelectionDialog dialog = new TimeSlotSelectionDialog(this, classModel);
        dialog.setOnSlotSelectedListener((slotId, selectedGroups) -> {
            scheduleClass(classModel, slotId, selectedGroups);
        });
        dialog.show();
    }

    private void deleteScheduledClass(ScheduledClass scheduledClass) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
            .collection("schedule")
            .document(scheduledClass.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                scheduledClasses.remove(scheduledClass.getSlotId());
                MaterialCardView slot = timeSlots.get(scheduledClass.getSlotId());
                if (slot != null) {
                    slot.removeAllViews();
                    slot.setBackgroundColor(getResources().getColor(android.R.color.white, null));
                }
                loadUnscheduledClasses();
                Toast.makeText(this, "Class removed from schedule", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this,
                "Failed to remove class: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void scheduleClass(ClassModel classModel, String slotId, List<String> selectedGroups) {
        // Check if slot is already occupied
        if (scheduledClasses.containsKey(slotId)) {
            Toast.makeText(this, "This time slot is already occupied", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        ScheduledClass scheduledClass = new ScheduledClass(
            classModel.getId(),
            classModel.getClassName(),
            slotId,
            selectedGroups
        );

        // Check if scheduling this would exceed total hours
        double currentHours = calculateScheduledHours(classModel.getId());
        if (currentHours + 2.0 > classModel.getTotalHours()) {
            Toast.makeText(this, "Cannot schedule more hours than the total hours required", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId)
            .collection("schedule")
            .add(scheduledClass)
            .addOnSuccessListener(documentReference -> {
                scheduledClass.setId(documentReference.getId());
                scheduledClasses.put(slotId, scheduledClass);
                updateTimeSlotView(scheduledClass);

                // Check if class is complete after scheduling
                if (calculateScheduledHours(classModel.getId()) >= classModel.getTotalHours()) {
                    updateClassStatus(classModel.getId(), "Completed");
                }
                loadUnscheduledClasses();
                Toast.makeText(this, "Class scheduled successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this,
                "Failed to schedule class: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private double calculateScheduledHours(String classId) {
        return scheduledClasses.values().stream()
            .filter(scheduled -> scheduled.getClassId().equals(classId))
            .count() * 2.0; // Each slot is 2 hours
    }

    private void updateClassStatus(String classId, String newStatus) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
            .collection("classes")
            .document(classId)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Class status updated to " + newStatus, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this,
                "Failed to update class status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUnscheduledClasses();
        loadScheduledClasses();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
