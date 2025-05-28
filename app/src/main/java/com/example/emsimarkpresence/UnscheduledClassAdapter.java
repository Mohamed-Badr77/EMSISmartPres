package com.example.emsimarkpresence;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class UnscheduledClassAdapter extends RecyclerView.Adapter<UnscheduledClassAdapter.ViewHolder> {
    private List<ClassModel> classes;
    private final OnClassClickListener listener;

    public interface OnClassClickListener {
        void onClassClick(ClassModel classModel);
    }

    public UnscheduledClassAdapter(List<ClassModel> classes, OnClassClickListener listener) {
        this.classes = classes;
        this.listener = listener;
    }

    public void updateClasses(List<ClassModel> newClasses) {
        this.classes = newClasses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unscheduled_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassModel classModel = classes.get(position);
        holder.bind(classModel, listener);
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvClassName;
        private final TextView tvGroupsAndHours;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvGroupsAndHours = itemView.findViewById(R.id.tvGroupsAndHours);
        }

        void bind(ClassModel classModel, OnClassClickListener listener) {
            tvClassName.setText(classModel.getClassName());
            String groupInfo = String.format(Locale.getDefault(),
                    "Groups: %s • Total Hours: %.1f",
                    String.join(", ", classModel.getSelectedGroupNames()),
                    classModel.getTotalHours());
            tvGroupsAndHours.setText(groupInfo);

            itemView.setOnClickListener(v -> listener.onClassClick(classModel));
        }
    }
}
