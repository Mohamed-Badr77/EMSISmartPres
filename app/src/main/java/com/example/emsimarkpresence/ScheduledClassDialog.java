package com.example.emsimarkpresence;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class ScheduledClassDialog extends Dialog {
    private final ScheduledClass scheduledClass;
    private final OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(ScheduledClass scheduledClass);
    }

    public ScheduledClassDialog(@NonNull Context context,
                              ScheduledClass scheduledClass,
                              OnDeleteListener deleteListener) {
        super(context);
        this.scheduledClass = scheduledClass;
        this.deleteListener = deleteListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_scheduled_class);

        TextView tvClassName = findViewById(R.id.tvClassName);
        TextView tvGroups = findViewById(R.id.tvGroups);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnClose = findViewById(R.id.btnClose);

        tvClassName.setText(scheduledClass.getClassName());
        tvGroups.setText("Groups: " + scheduledClass.getFormattedGroupNames());

        btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(scheduledClass);
            }
            dismiss();
        });

        btnClose.setOnClickListener(v -> dismiss());
    }
}
