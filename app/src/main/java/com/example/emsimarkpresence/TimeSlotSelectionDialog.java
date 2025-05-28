package com.example.emsimarkpresence;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class TimeSlotSelectionDialog extends Dialog {
    private final ClassModel classModel;
    private OnSlotSelectedListener listener;
    private String selectedSlotId;

    public interface OnSlotSelectedListener {
        void onSlotSelected(String slotId, List<String> selectedGroups);
    }

    public TimeSlotSelectionDialog(@NonNull Context context, ClassModel classModel) {
        super(context);
        this.classModel = classModel;
    }

    public void setOnSlotSelectedListener(OnSlotSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_time_slot_selection);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Schedule " + classModel.getClassName());

        setupTimeSlots();
        setupGroups();
        setupButtons();
    }

    private void setupTimeSlots() {
        RadioGroup rgTimeSlots = findViewById(R.id.rgTimeSlots);
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        String[] times = {"8:30-10:30", "10:45-12:45", "14:00-16:00", "16:15-18:15"};

        for (String day : days) {
            for (int slot = 1; slot <= 4; slot++) {
                RadioButton rb = new RadioButton(getContext());
                rb.setText(String.format("%s %s", day, times[slot - 1]));
                String slotId = day.toLowerCase() + "_" + slot;
                rb.setTag(slotId);
                rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedSlotId = slotId;
                    }
                });
                rgTimeSlots.addView(rb);
            }
        }
    }

    private void setupGroups() {
        LinearLayout groupContainer = findViewById(R.id.groupContainer);
        List<String> groupNames = classModel.getSelectedGroupNames();

        for (String groupName : groupNames) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(groupName);
            cb.setTag(groupName);
            groupContainer.addView(cb);
        }
    }

    private void setupButtons() {
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSchedule = findViewById(R.id.btnSchedule);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSchedule.setOnClickListener(v -> {
            if (selectedSlotId != null && !getSelectedGroups().isEmpty()) {
                if (listener != null) {
                    listener.onSlotSelected(selectedSlotId, getSelectedGroups());
                }
                dismiss();
            }
        });
    }

    private List<String> getSelectedGroups() {
        List<String> selectedGroups = new ArrayList<>();
        LinearLayout groupContainer = findViewById(R.id.groupContainer);

        for (int i = 0; i < groupContainer.getChildCount(); i++) {
            if (groupContainer.getChildAt(i) instanceof CheckBox) {
                CheckBox cb = (CheckBox) groupContainer.getChildAt(i);
                if (cb.isChecked()) {
                    selectedGroups.add(cb.getText().toString());
                }
            }
        }
        return selectedGroups;
    }
}
