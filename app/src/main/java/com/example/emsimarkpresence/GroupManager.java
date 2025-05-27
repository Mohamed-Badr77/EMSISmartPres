package com.example.emsimarkpresence;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

public class GroupManager {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

    // Create new global group
    public static Task<DocumentReference> createGroup(String name, Campus campus) {
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

    public static Task<Void> deleteClass(String classId, String teacherId) {
        WriteBatch batch = db.batch();

        // First, remove this class from all groups it's associated with
        return db.collection("groups")
                .whereArrayContains("classes", classId)
                .get()
                .continueWithTask(task -> {
                    for (DocumentSnapshot groupDoc : task.getResult()) {
                        String groupId = groupDoc.getId();
                        DocumentReference groupRef = db.collection("groups").document(groupId);
                        batch.update(groupRef, "classes." + classId, FieldValue.delete());
                        batch.update(groupRef, "classTeachers." + classId, FieldValue.delete());
                    }

                    // Then delete the class document itself
                    DocumentReference classRef = db.collection("users")
                            .document(teacherId)
                            .collection("classes")
                            .document(classId);
                    batch.delete(classRef);

                    return batch.commit();
                });
    }

    // Link group to class (bidirectional)
    public static Task<Void> linkGroupToClass(String groupId, String classId, String teacherId) {
        WriteBatch batch = db.batch();

        // Add to class's groups (in user's subcollection)
        DocumentReference classRef = db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId);
        batch.update(classRef, "groupMap." + groupId, true);

        // Add to group's classes
        DocumentReference groupRef = db.collection("groups").document(groupId);
        batch.update(groupRef, "classes." + classId, true);
        batch.update(groupRef, "classTeachers." + classId, teacherId);

        return batch.commit();
    }

    public static Task<Void> unlinkClassFromAllGroups(String classId, String teacherId) {
        WriteBatch batch = db.batch();

        return db.collection("groups")
                .whereEqualTo("classes." + classId, true)
                .get()
                .continueWithTask(task -> {
                    for (DocumentSnapshot groupDoc : task.getResult()) {
                        String groupId = groupDoc.getId();
                        DocumentReference groupRef = db.collection("groups").document(groupId);
                        batch.update(groupRef, "classes." + classId, FieldValue.delete());
                        batch.update(groupRef, "classTeachers." + classId, FieldValue.delete());
                    }
                    return batch.commit();
                });
    }

    public static Task<Void> unlinkGroupFromClass(String groupId, String classId, String teacherId) {
        WriteBatch batch = db.batch();

        // Remove from group's classes
        DocumentReference groupRef = db.collection("groups").document(groupId);
        batch.update(groupRef, "classes." + classId, FieldValue.delete());
        batch.update(groupRef, "classTeachers." + classId, FieldValue.delete());

        // Remove from class's groups (in user's subcollection)
        DocumentReference classRef = db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId);
        batch.update(classRef, "groupMap." + groupId, FieldValue.delete());

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
