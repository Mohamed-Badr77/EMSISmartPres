package com.example.emsimarkpresence;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ClassStatusEditDialog {

    public interface OnStatusUpdateListener {
        void onStatusUpdated(ClassModel updatedClass);
    }

    public static void show(Context context, ClassModel classModel, OnStatusUpdateListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_class_status, null);

        RadioGroup radioStatus = view.findViewById(R.id.radioStatusEdit);
        RadioButton radioActive = view.findViewById(R.id.radioActiveEdit);
        RadioButton radioPaused = view.findViewById(R.id.radioPausedEdit);
        RadioButton radioCompleted = view.findViewById(R.id.radioCompletedEdit);

        // Set current status
        switch (classModel.getStatus()) {
            case "Active":
                radioActive.setChecked(true);
                break;
            case "Paused":
                radioPaused.setChecked(true);
                break;
            case "Completed":
                radioCompleted.setChecked(true);
                break;
        }

        builder.setView(view)
                .setTitle("Change Class Status: " + classModel.getClassName())
                .setPositiveButton("Update", (dialog, which) -> {
                    String newStatus;
                    int selectedId = radioStatus.getCheckedRadioButtonId();
                    if (selectedId == R.id.radioActiveEdit) newStatus = "Active";
                    else if (selectedId == R.id.radioPausedEdit) newStatus = "Paused";
                    else newStatus = "Completed";

                    updateClassStatus(context, classModel, newStatus, listener);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void updateClassStatus(Context context, ClassModel classModel,
                                          String newStatus, OnStatusUpdateListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();

        classModel.setStatus(newStatus);

        db.collection("users").document(userId).collection("classes")
                .document(classModel.getId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onStatusUpdated(classModel);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error updating status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}