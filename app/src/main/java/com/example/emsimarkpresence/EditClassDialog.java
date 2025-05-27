package com.example.emsimarkpresence;

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditClassDialog extends Dialog {
    private EditText etNumberOfWeeks, etHoursPerSession;
    private TextView tvTotalHours;
    private LinearLayout groupSelectionContainer;
    private RadioGroup radioStatus;
    private Button btnSave;
    private ClassModel classModel;
    private FirebaseFirestore db;
    private String userId;
    private String documentId;
    private List<Group> availableGroups = new ArrayList<>();
    private Map<String, CheckBox> groupCheckBoxes = new HashMap<>();

    public interface OnClassUpdatedListener{
        void onClassUpdated();
    }

    private OnClassUpdatedListener listener;

    public void setOnClassUpdatedListener(OnClassUpdatedListener listener){
        this.listener = listener;
    }

    public EditClassDialog(@NonNull Context context, ClassModel classModel, String documentId) {
        super(context);
        this.classModel = classModel;
        this.documentId = documentId;
        setContentView(R.layout.dialog_edit_class);

        //Dialog sizing
        Window window = getWindow();
        if(window != null){
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels*0.8);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        etNumberOfWeeks = findViewById(R.id.etNumberOfWeeks);
        etHoursPerSession = findViewById(R.id.etHoursPerSession);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        groupSelectionContainer = findViewById(R.id.groupSelectionContainer);
        radioStatus = findViewById(R.id.radioStatus);
        btnSave = findViewById(R.id.btnSave);

        // Set current values
        etNumberOfWeeks.setText(String.valueOf(classModel.getNumberOfWeeks()));
        etHoursPerSession.setText(String.valueOf(classModel.getHoursPerSession()));

        // Set current status
        switch (classModel.getStatus()) {
            case "Active":
                radioStatus.check(R.id.radioActive);
                break;
            case "Paused":
                radioStatus.check(R.id.radioPaused);
                break;
            case "Completed":
                radioStatus.check(R.id.radioCompleted);
                break;
        }

        // Add listeners for automatic total hours calculation
        etNumberOfWeeks.addTextChangedListener(new SimpleTextWatcher(this::updateTotalHours));
        etHoursPerSession.addTextChangedListener(new SimpleTextWatcher(this::updateTotalHours));

        // Load available groups and populate form
        loadAvailableGroups();

        btnSave.setOnClickListener(v -> updateClass());

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
                        CheckBox checkBox = new CheckBox(getContext());
                        checkBox.setText(group.getName() + " (" + group.getCampus() + ")");
                        checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));

                        groupSelectionContainer.addView(checkBox);
                        groupCheckBoxes.put(group.getId(), checkBox);
                    }

                    // Select the groups that are currently assigned to this class
                    if (classModel.getGroupMap() != null) {
                        for (Map.Entry<String, Boolean> entry : classModel.getGroupMap().entrySet()) {
                            CheckBox checkBox = groupCheckBoxes.get(entry.getKey());
                            if (checkBox != null && entry.getValue()) {
                                checkBox.setChecked(true);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading groups: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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

    private void updateClass() {
        String weeksStr = etNumberOfWeeks.getText().toString().trim();
        String hoursStr = etHoursPerSession.getText().toString().trim();

        // Validate input
        if (weeksStr.isEmpty() || hoursStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfWeeks;
        double hoursPerSession;
        try {
            numberOfWeeks = Integer.parseInt(weeksStr);
            hoursPerSession = Double.parseDouble(hoursStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Please enter valid numbers!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "Please select at least one group!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get updated status
        String status;
        int selectedId = radioStatus.getCheckedRadioButtonId();
        if (selectedId == R.id.radioActive) status = "Active";
        else if (selectedId == R.id.radioPaused) status = "Paused";
        else status = "Completed";

        // Update the class model
        classModel.setGroupMap(groupMap);
        classModel.setNumberOfWeeks(numberOfWeeks);
        classModel.setHoursPerSession(hoursPerSession);
        classModel.setStatus(status);

        // Update Firestore
        db.collection("users").document(userId).collection("classes")
                .document(documentId)
                .set(classModel)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Class updated!", Toast.LENGTH_SHORT).show();
                    if(listener != null){
                        listener.onClassUpdated();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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