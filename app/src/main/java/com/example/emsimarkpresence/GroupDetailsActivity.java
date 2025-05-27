package com.example.emsimarkpresence;

import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupDetailsActivity extends AppCompatActivity implements StudentAdapter.OnStudentRemoveListener, ClassRemoveListener {

    private String groupId;
    private Group currentGroup;
    private FirebaseFirestore db;
    private String currentUserId;

    private TextView tvGroupName, tvCampus, tvNoStudents, tvNoClasses;
    private RecyclerView studentsRecyclerView, classesRecyclerView;
    private StudentAdapter studentAdapter;
    private ClassSimpleAdapter classAdapter;
    private List<Student> students = new ArrayList<>();
    private List<ClassModel> classes = new ArrayList<>();

    private Button btnAddStudent, btnAddClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Get group ID from intent
        groupId = getIntent().getStringExtra("groupId");

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (groupId == null) {
            Toast.makeText(this, "Group ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tvGroupName = findViewById(R.id.tvGroupName);
        tvCampus = findViewById(R.id.tvCampus);
        tvNoStudents = findViewById(R.id.tvNoStudents);
        tvNoClasses = findViewById(R.id.tvNoClasses);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        btnAddClass = findViewById(R.id.btnAddClass);
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        classesRecyclerView = findViewById(R.id.classesRecyclerView);

        // Setup students RecyclerView
        studentAdapter = new StudentAdapter(this, students, this);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentsRecyclerView.setAdapter(studentAdapter);

        // Setup classes RecyclerView
        classAdapter = new ClassSimpleAdapter(classes, this);
        classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        classesRecyclerView.setAdapter(classAdapter);

        // Set click listeners for buttons
        btnAddStudent.setOnClickListener(v -> showAddStudentDialog());
        btnAddClass.setOnClickListener(v -> showAddClassDialog());
        findViewById(R.id.fabMain).setOnClickListener(v -> {
            // Show options for what to add
            showAddOptionsDialog();
        });

        // Load group details
        loadGroupDetails();
    }

    @Override
    public void onStudentRemoved(Student student) {
        GroupManager.removeStudentFromGroup(groupId, student.getId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Student removed", Toast.LENGTH_SHORT).show();
                    // No need to call loadStudents() - snapshot listener handles it
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onClassRemoved(ClassModel classObj) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Removing class...");
        progress.show();

        // Get the teacher ID who owns this class (stored in the group)
        String teacherId = currentGroup.getClassTeachers().get(classObj.getId());
        if (teacherId == null) {
            teacherId = currentUserId; // Fallback to current user if not found
        }

        GroupManager.unlinkGroupFromClass(groupId, classObj.getId(), teacherId)
                .addOnSuccessListener(aVoid -> {
                    progress.dismiss();
                    Toast.makeText(this, "Class removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to remove class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private void showAddOptionsDialog() {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Add Student", "Add Class"}, (dialog, which) -> {
                    if (which == 0) showAddStudentDialog();
                    else showAddClassDialog();
                })
                .show();
    }




    private void loadGroupDetails() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Loading group...");
        progress.show();
        db.collection("groups").document(groupId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    progress.dismiss();
                    if (error != null) {
                        Toast.makeText(this, "Error listening to group: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        currentGroup = documentSnapshot.toObject(Group.class);
                        if (currentGroup != null) {
                            currentGroup.setId(documentSnapshot.getId());
                            updateUI();
                            loadStudents();
                            loadClasses();
                        }
                    } else {
                        Toast.makeText(this, "Group no longer exists", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void updateUI() {
        if (currentGroup != null) {
            tvGroupName.setText(currentGroup.getName());
            tvCampus.setText(currentGroup.getCampus().toString());

            // Update the empty states immediately
            updateEmptyStates();
        }
    }

    private void loadStudents() {
        if (currentGroup.getStudents() != null && !currentGroup.getStudents().isEmpty()) {
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
                                updateEmptyStates();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error loading student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            updateEmptyStates();
        }
    }

    private void loadClasses() {
        if (currentGroup.getClasses() != null && !currentGroup.getClasses().isEmpty()) {
            classes.clear();
            for (Map.Entry<String, Boolean> entry : currentGroup.getClasses().entrySet()) {
                String classId = entry.getKey();
                String teacherId = currentGroup.getClassTeachers().get(classId);
                if (teacherId == null) {
                    teacherId = currentUserId; // Fallback to current user if not found
                }

                db.collection("users").document(teacherId)
                        .collection("classes").document(classId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            ClassModel classObj = documentSnapshot.toObject(ClassModel.class);
                            if (classObj != null) {
                                classObj.setId(documentSnapshot.getId());
                                classes.add(classObj);
                                classAdapter.notifyDataSetChanged();
                                updateEmptyStates();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error loading class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        } else {
            updateEmptyStates();
        }
    }

    private void updateEmptyStates() {
        // Show/hide empty state messages
        tvNoStudents.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
        tvNoClasses.setVisibility(classes.isEmpty() ? View.VISIBLE : View.GONE);
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
                                    // No need to manually refresh - the snapshot listener will handle it
                                    Toast.makeText(this, "Student added to group", Toast.LENGTH_SHORT).show();
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

        // Load classes from the current user's subcollection
        db.collection("users").document(currentUserId).collection("classes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> classNames = new ArrayList<>();
                    List<String> classIds = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        ClassModel classObj = doc.toObject(ClassModel.class);
                        if (classObj != null) {
                            classNames.add(classObj.getClassName());
                            classIds.add(doc.getId());
                        }
                    }

                    if (classNames.isEmpty()) {
                        Toast.makeText(this, "You don't have any classes yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    builder.setItems(classNames.toArray(new String[0]), (dialog, which) -> {
                        String selectedClassId = classIds.get(which);
                        GroupManager.linkGroupToClass(groupId, selectedClassId, currentUserId)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Class added to group", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to add class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });

                    builder.show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load your classes", Toast.LENGTH_SHORT).show();
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


    // Simple adapter for classes
    private class ClassSimpleAdapter extends RecyclerView.Adapter<ClassSimpleAdapter.ClassViewHolder> {
        private final List<ClassModel> classes;
        private final ClassRemoveListener removeListener;

        public ClassSimpleAdapter(List<ClassModel> classes, ClassRemoveListener removeListener) {
            this.classes = classes;
            this.removeListener = removeListener;
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
            ClassModel classObj = classes.get(position);
            holder.bind(classObj);

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(v.getContext())  // Use holder item's context
                        .setTitle("Remove Class")
                        .setMessage("Remove " + classObj.getClassName() + " from group?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            removeListener.onClassRemoved(classObj);
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


        class ClassViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvClassName;

            public ClassViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClassName = itemView.findViewById(R.id.tvClassName);
            }

            public void bind(ClassModel classObj) {
                tvClassName.setText(classObj.getClassName());
                // Make class name stand out
                tvClassName.setTextSize(16);
                tvClassName.setTypeface(tvClassName.getTypeface(), Typeface.BOLD);
            }
        }
    }
}