package com.example.emsimarkpresence;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupDetailsActivity extends AppCompatActivity {
    private String groupId;
    private Group currentGroup;
    private FirebaseFirestore db;

    private TextView tvGroupName, tvCampus;
    private RecyclerView studentsRecyclerView, classesRecyclerView;
    private StudentAdapter studentAdapter;
    private ClassSimpleAdapter classAdapter;
    private List<Student> students = new ArrayList<>();
    private List<Class> classes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get group ID from intent
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            Toast.makeText(this, "Group ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tvGroupName = findViewById(R.id.tvGroupName);
        tvCampus = findViewById(R.id.tvCampus);
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        classesRecyclerView = findViewById(R.id.classesRecyclerView);

        // Setup students RecyclerView
        studentAdapter = new StudentAdapter(students);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentsRecyclerView.setAdapter(studentAdapter);

        // Setup classes RecyclerView
        classAdapter = new ClassSimpleAdapter(classes);
        classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        classesRecyclerView.setAdapter(classAdapter);

        // Load group details
        loadGroupDetails();
    }

    private void loadGroupDetails() {
        db.collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentGroup = documentSnapshot.toObject(Group.class);
                        if (currentGroup != null) {
                            currentGroup.setId(documentSnapshot.getId());
                            updateUI();
                            loadStudents();
                            loadClasses();
                        }
                    } else {
                        Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI() {
        if (currentGroup != null) {
            tvGroupName.setText(currentGroup.getName());
            tvCampus.setText(currentGroup.getCampus());
        }
    }

    private void loadStudents() {
        if (currentGroup.getStudents() != null) {
            students.clear();
            for (Map.Entry<String, Boolean> entry : currentGroup.getStudents().entrySet()) {
                String studentId = entry.getKey();
                db.collection("students").document(studentId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Student student = documentSnapshot.toObject(Student.class);
                            if (student != null) {
                                student.setId(documentSnapshot.getId());
                                students.add(student);
                                studentAdapter.notifyDataSetChanged();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error loading student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void loadClasses() {
        if (currentGroup.getClasses() != null) {
            classes.clear();
            for (Map.Entry<String, Boolean> entry : currentGroup.getClasses().entrySet()) {
                String classId = entry.getKey();
                db.collection("classes").document(classId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Class classObj = documentSnapshot.toObject(Class.class);
                            if (classObj != null) {
                                classObj.setId(documentSnapshot.getId());
                                classes.add(classObj);
                                classAdapter.notifyDataSetChanged();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error loading class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_student) {
            showAddStudentDialog();
            return true;
        } else if (item.getItemId() == R.id.action_add_class) {
            showAddClassDialog();
            return true;
        } else if (item.getItemId() == R.id.action_delete_group) {
            deleteGroup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddStudentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Student to Group");

        // Get all available students
        db.collection("students")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> studentNames = new ArrayList<>();
                    List<String> studentIds = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Student student = doc.toObject(Student.class);
                        if (student != null) {
                            studentNames.add(student.getName() + " (" + student.getEmail() + ")");
                            studentIds.add(doc.getId());
                        }
                    }

                    builder.setItems(studentNames.toArray(new String[0]), (dialog, which) -> {
                        String selectedStudentId = studentIds.get(which);
                        GroupManager.addStudentToGroup(groupId, selectedStudentId)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Student added to group", Toast.LENGTH_SHORT).show();
                                    loadStudents(); // Refresh the list
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to add student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });

                    builder.show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Class to Group");

        // Get all available classes
        db.collection("classes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> classNames = new ArrayList<>();
                    List<String> classIds = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Class classObj = doc.toObject(Class.class);
                        if (classObj != null) {
                            classNames.add(classObj.getName());
                            classIds.add(doc.getId());
                        }
                    }

                    builder.setItems(classNames.toArray(new String[0]), (dialog, which) -> {
                        String selectedClassId = classIds.get(which);
                        GroupManager.linkGroupToClass(groupId, selectedClassId)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Class added to group", Toast.LENGTH_SHORT).show();
                                    loadClasses(); // Refresh the list
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to add class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });

                    builder.show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load classes", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteGroup() {
        // Implement group deletion with confirmation
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Group")
                .setMessage("Are you sure you want to delete this group?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("groups").document(groupId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Simple adapter for students
    private static class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
        private final List<Student> students;

        public StudentAdapter(List<Student> students) {
            this.students = students;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_simple, parent, false);
            return new StudentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            Student student = students.get(position);
            holder.bind(student);
        }

        @Override
        public int getItemCount() {
            return students.size();
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
            }
        }
    }

    // Simple adapter for classes
    private static class ClassSimpleAdapter extends RecyclerView.Adapter<ClassSimpleAdapter.ClassViewHolder> {
        private final List<Class> classes;

        public ClassSimpleAdapter(List<Class> classes) {
            this.classes = classes;
        }

        @NonNull
        @Override
        public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_class_simple, parent, false);
            return new ClassViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
            Class classObj = classes.get(position);
            holder.bind(classObj);
        }

        @Override
        public int getItemCount() {
            return classes.size();
        }

        static class ClassViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvClassName;

            public ClassViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClassName = itemView.findViewById(R.id.tvClassName);
            }

            public void bind(Class classObj) {
                tvClassName.setText(classObj.getName());
            }
        }
    }
}