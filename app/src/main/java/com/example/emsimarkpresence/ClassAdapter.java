package com.example.emsimarkpresence;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {
    private List<ClassModel> classes = new ArrayList<>();
    private OnClassActionListener editListener;
    private Context context;

    public interface OnClassActionListener {
        void onEditClass(ClassModel classModel);
        void onDeleteClass(ClassModel classModel);
        void onStatusChanged(ClassModel classModel);
    }

    public ClassAdapter(Context context) {
        this.context = context;
    }

    public void setOnClassActionListener(OnClassActionListener listener) {
        this.editListener = listener;
    }

    public void setClasses(List<ClassModel> classes) {
        this.classes = classes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassModel classModel = classes.get(position);
        holder.bind(classModel);

        holder.btnEdit.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditClass(classModel);
            }
        });

        holder.btnChangeStatus.setOnClickListener(v -> {
            ClassStatusEditDialog.show(context, classModel, updatedClass -> {
                if (editListener != null) {
                    editListener.onStatusChanged(updatedClass);
                }
            });
        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Class")
                    .setMessage("Are you sure you want to delete this class?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (editListener != null) {
                            editListener.onDeleteClass(classModel);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        private TextView tvClassName, tvWeeksAndHours, tvTotalHours, tvGroups, tvStatus;
        private Button btnEdit, btnChangeStatus;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvWeeksAndHours = itemView.findViewById(R.id.tvWeeksAndHours);
            tvTotalHours = itemView.findViewById(R.id.tvTotalHours);
            tvGroups = itemView.findViewById(R.id.tvGroups);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnChangeStatus = itemView.findViewById(R.id.btnChangeStatus);
        }

        public void bind(ClassModel classModel) {
            tvClassName.setText(classModel.getClassName());

            // Display weeks and hours per session
            tvWeeksAndHours.setText(String.format(Locale.getDefault(),
                    "%d weeks × %.1fh/session",
                    classModel.getNumberOfWeeks(),
                    classModel.getHoursPerSession()));

            // Display total hours
            tvTotalHours.setText(String.format(Locale.getDefault(),
                    "Total: %.1f hours",
                    classModel.getTotalHours()));

            // Display selected groups count
            if (classModel.getGroupMap() != null && !classModel.getGroupMap().isEmpty()) {
                long selectedGroups = classModel.getGroupMap().values().stream()
                        .mapToLong(selected -> selected ? 1 : 0).sum();
                tvGroups.setText("Groups: " + selectedGroups + " selected");
            } else {
                tvGroups.setText("Groups: None");
            }

            tvStatus.setText("Status: " + classModel.getStatus());
        }
    }
}