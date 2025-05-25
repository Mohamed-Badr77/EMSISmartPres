package com.example.emsimarkpresence;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {
    private List<ClassModel> classes = new ArrayList<>();
    private OnClassEditListener editListener;

    public interface OnClassEditListener {
        void onEditClass(ClassModel classModel);
    }


    public void setOnClassEditListener(OnClassEditListener listener) {
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
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        private TextView tvClassName, tvHours, tvGroups, tvStatus;
        private Button btnEdit;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvHours = itemView.findViewById(R.id.tvHours);
            tvGroups = itemView.findViewById(R.id.tvGroups);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }

        public void bind(ClassModel classModel) {
            tvClassName.setText(classModel.getClassName());
            tvHours.setText("Hours: " + classModel.getTotalHours());
            tvGroups.setText("Groups: " + String.join(", ", classModel.getGroups()));
            tvStatus.setText("Status: " + classModel.getStatus());
        }
    }
}