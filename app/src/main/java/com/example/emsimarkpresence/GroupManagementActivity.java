package com.example.emsimarkpresence;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupManagementActivity extends AppCompatActivity {
    private RecyclerView groupsRecyclerView;
    private GroupAdapter adapter;
    private List<Group> groups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_management);

        // Initialize RecyclerView
        groupsRecyclerView = findViewById(R.id.groupsRecyclerView);
        groupsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter with click listener
        adapter = new GroupAdapter(groups, group -> {
            // Handle group click (e.g., open GroupDetailsActivity)
            Intent intent = new Intent(this, GroupDetailsActivity.class);
            intent.putExtra("groupId", group.getId());
            startActivity(intent);
        });

        groupsRecyclerView.setAdapter(adapter);

        // Load groups
        loadGroups();

        // Setup FAB
        findViewById(R.id.fabAddGroup).setOnClickListener(v -> showCreateGroupDialog());
    }

    private void loadGroups() {
        GroupManager.getAllGroups().addOnSuccessListener(querySnapshot -> {
            groups.clear();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                Group group = doc.toObject(Group.class);
                group.setId(doc.getId());
                groups.add(group);
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading groups", Toast.LENGTH_SHORT).show();
        });
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_group, null);

        EditText etName = view.findViewById(R.id.etGroupName);
        EditText etCampus = view.findViewById(R.id.etCampus);

        builder.setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String campus = etCampus.getText().toString().trim();

                    if (name.isEmpty() || campus.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GroupManager.createGroup(name, campus)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Group created!", Toast.LENGTH_SHORT).show();
                                loadGroups(); // Refresh list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}