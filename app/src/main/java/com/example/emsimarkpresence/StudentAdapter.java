package com.example.emsimarkpresence;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private final List<Student> students;
    private final Context context;
    private OnStudentRemoveListener removeListener;
    private OnStudentClickListener clickListener;

    // Interface for click events
    public interface OnStudentClickListener {
        void onStudentClick(Student student);
        void onStudentLongClick(Student student);
    }

    // Interface for removal events
    public interface OnStudentRemoveListener {
        void onStudentRemoved(Student student);
    }

    // Constructor with all parameters
    public StudentAdapter(Context context, List<Student> students,
                          OnStudentRemoveListener removeListener,
                          OnStudentClickListener clickListener) {
        this.context = context;
        this.students = students;
        this.removeListener = removeListener;
        this.clickListener = clickListener;
    }

    // Constructor for removal only
    public StudentAdapter(Context context, List<Student> students,
                          OnStudentRemoveListener removeListener) {
        this(context, students, removeListener, null);
    }

    // Constructor for click only
    public StudentAdapter(Context context, List<Student> students,
                          OnStudentClickListener clickListener) {
        this(context, students, null, clickListener);
    }

    // Minimal constructor - requires at least Context
    public StudentAdapter(Context context, List<Student> students) {
        this(context, students, null, null);
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = students.get(position);
        holder.bind(student);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStudentClick(student);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStudentLongClick(student);
            }

            if (removeListener != null) {
                showRemoveDialog(student);
            }
            return true;
        });
    }

    private void showRemoveDialog(Student student) {
        new AlertDialog.Builder(context)
                .setTitle("Remove Student")
                .setMessage("Remove " + student.getName() + " from group?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    removeListener.onStudentRemoved(student);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public void updateStudents(List<Student> newStudents) {
        this.students.clear();
        this.students.addAll(newStudents);
        notifyDataSetChanged(); // This is crucial
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStudentName, tvStudentEmail;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
        }

        public void bind(Student student) {
            tvStudentName.setText(student.getName());
            tvStudentEmail.setText(student.getEmail());

            // Custom styling
            tvStudentName.setTextSize(16);
            tvStudentName.setTypeface(tvStudentName.getTypeface(), Typeface.BOLD);
            tvStudentEmail.setTextSize(12);
            tvStudentEmail.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.gray_600));
        }
    }
}