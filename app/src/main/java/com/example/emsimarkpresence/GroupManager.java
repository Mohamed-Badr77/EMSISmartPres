package com.example.emsimarkpresence;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

public class GroupManager {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Create new global group
    public static Task<DocumentReference> createGroup(String name, String campus) {
        Group group = new Group();
        group.setName(name);
        group.setCampus(campus);

        return db.collection("groups")
                .add(group)
                .addOnSuccessListener(documentReference -> {
                    // Auto-set the document ID
                    documentReference.update("id", documentReference.getId());
                });
    }

    // Link group to class (bidirectional)
    public static Task<Void> linkGroupToClass(String groupId, String classId) {
        WriteBatch batch = db.batch();

        // Add to class's groups
        DocumentReference classRef = db.collection("classes").document(classId);
        batch.update(classRef, "groups." + groupId, true);

        // Add to group's classes (optional)
        DocumentReference groupRef = db.collection("groups").document(groupId);
        batch.update(groupRef, "classes." + classId, true);

        return batch.commit();
    }

    // Add student to group
    public static Task<Void> addStudentToGroup(String groupId, String studentId) {
        return db.collection("groups").document(groupId)
                .update("students." + studentId, true);
    }

    // Remove student from group
    public static Task<Void> removeStudentFromGroup(String groupId, String studentId) {
        return db.collection("groups").document(groupId)
                .update("students." + studentId, FieldValue.delete());
    }

    // Get all groups
    public static Task<QuerySnapshot> getAllGroups() {
        return db.collection("groups").get();
    }
}
