package com.example.emsimarkpresence;

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EditClassDialog extends Dialog {
    private EditText etNewGroup;
    private LinearLayout groupContainer;
    private RadioGroup radioStatus;
    private Button btnAddGroup, btnSave;
    private ClassModel classModel;
    private FirebaseFirestore db;
    private String userId;
    private String documentId; // To identify the Firestore document


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

        //Dialog
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
        etNewGroup = findViewById(R.id.etNewGroup);
        groupContainer = findViewById(R.id.groupContainer);
        radioStatus = findViewById(R.id.radioStatus);
        btnAddGroup = findViewById(R.id.btnAddGroup);
        btnSave = findViewById(R.id.btnSave);

        // Load existing groups
        for (String group : classModel.getGroups()) {
            addGroupView(group);
        }

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

        btnAddGroup.setOnClickListener(v -> {
            String groupName = etNewGroup.getText().toString().trim();
            if (!groupName.isEmpty()) {
                addGroupView(groupName);
                etNewGroup.setText("");
            }
        });

        btnSave.setOnClickListener(v -> updateClass());
    }

    private void addGroupView(String groupName) {
        View groupView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_group_edit, groupContainer, false); // Changed to new layout

        TextView tvGroup = groupView.findViewById(R.id.tvGroup);
        ImageButton btnRemove = groupView.findViewById(R.id.btnRemove);

        tvGroup.setText(groupName);
        btnRemove.setOnClickListener(v -> groupContainer.removeView(groupView));

        groupContainer.addView(groupView);
    }

    private void updateClass() {
        // Get updated groups
        List<String> updatedGroups = new ArrayList<>();
        for (int i = 0; i < groupContainer.getChildCount(); i++) {
            View view = groupContainer.getChildAt(i);
            TextView tvGroup = view.findViewById(R.id.tvGroup);
            updatedGroups.add(tvGroup.getText().toString());
        }

        // Get updated status
        String status;
        int selectedId = radioStatus.getCheckedRadioButtonId();
        if (selectedId == R.id.radioActive) status = "Active";
        else if (selectedId == R.id.radioPaused) status = "Paused";
        else status = "Completed";

        // Update Firestore
        db.collection("users").document(userId).collection("classes")
                .document(documentId)
                .update(
                        "groups", updatedGroups,
                        "status", status
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Class updated!", Toast.LENGTH_SHORT).show();
                    if(listener!=null){
                        listener.onClassUpdated();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}