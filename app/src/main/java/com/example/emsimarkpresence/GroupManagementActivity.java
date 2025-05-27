package com.example.emsimarkpresence;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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

        groupsRecyclerView = findViewById(R.id.groupsRecyclerView);
        groupsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GroupAdapter(groups, group -> {
            Intent intent = new Intent(this, GroupDetailsActivity.class);
            intent.putExtra("groupId", group.getId());
            startActivity(intent);
        });

        groupsRecyclerView.setAdapter(adapter);
        loadGroups();

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
        Spinner spinnerCampus = view.findViewById(R.id.spinnerCampus);

        // Setup campus spinner
        ArrayAdapter<Campus> campusAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, Campus.values());
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCampus.setAdapter(campusAdapter);

        builder.setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    Campus campus = (Campus) spinnerCampus.getSelectedItem();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GroupManager.createGroup(name, campus)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Group created!", Toast.LENGTH_SHORT).show();
                                loadGroups();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}