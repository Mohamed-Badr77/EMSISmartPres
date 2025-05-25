package com.example.emsimarkpresence;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<Student> students;
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
        void onStudentLongClick(Student student);
    }

    public StudentAdapter(List<Student> students) {
        this.students = students;
    }

    public void setOnStudentClickListener(OnStudentClickListener listener) {
        this.listener = listener;
    }

    public void updateStudents(List<Student> newStudents) {
        this.students = newStudents;
        notifyDataSetChanged();
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
            if (listener != null) {
                listener.onStudentClick(student);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onStudentLongClick(student);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvStudentName;
        private TextView tvStudentEmail;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
        }

        public void bind(Student student) {
            tvStudentName.setText(student.getName());
            tvStudentEmail.setText(student.getEmail());
        }
    }
}