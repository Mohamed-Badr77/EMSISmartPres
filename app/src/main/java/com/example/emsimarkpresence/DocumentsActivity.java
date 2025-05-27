package com.example.emsimarkpresence;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DocumentsActivity extends AppCompatActivity implements DocumentAdapter.OnDocumentActionListener {
    private static final int PICK_FILE_REQUEST = 1;

    private RecyclerView rvDocuments;
    private View emptyState;
    private FloatingActionButton fabAddDocument;
    private DocumentAdapter adapter;
    private List<Document> documents = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        rvDocuments = findViewById(R.id.rvDocuments);
        emptyState = findViewById(R.id.emptyState);
        fabAddDocument = findViewById(R.id.fabAddDocument);

        // Setup RecyclerView
        adapter = new DocumentAdapter(this, documents, this);
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        rvDocuments.setAdapter(adapter);

        // Set click listeners
        fabAddDocument.setOnClickListener(v -> pickDocument());
        findViewById(R.id.btnAddFirstDocument).setOnClickListener(v -> pickDocument());

        // Load documents
        loadDocuments();
    }

    private void loadDocuments() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Loading documents...");
        progress.show();

        db.collection("documents")
                .orderBy("uploadDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progress.dismiss();
                    documents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Document document = doc.toObject(Document.class);
                        document.setId(doc.getId());
                        documents.add(document);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to load documents", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (documents.isEmpty()) {
            rvDocuments.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvDocuments.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void pickDocument() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            uploadDocument(fileUri);
        }
    }

    private void uploadDocument(Uri fileUri) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Uploading document...");
        progress.show();

        // Get file info
        String fileName = getFileName(fileUri);
        String fileType = getFileType(fileName);
        String userId = auth.getCurrentUser().getUid();

        // Create storage reference
        StorageReference storageRef = storage.getReference()
                .child("documents")
                .child(userId)
                .child(fileName);

        // Upload file to storage
        UploadTask uploadTask = storageRef.putFile(fileUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Create document in Firestore
                Document document = new Document();
                document.setName(fileName);
                document.setType(fileType);
                document.setDownloadUrl(uri.toString());
                document.setSize(taskSnapshot.getMetadata().getSizeBytes());
                document.setUploadDate(new Date());
                document.setUploaderId(userId);

                db.collection("documents")
                        .add(document)
                        .addOnSuccessListener(documentReference -> {
                            progress.dismiss();
                            document.setId(documentReference.getId());
                            documents.add(0, document);
                            adapter.notifyItemInserted(0);
                            rvDocuments.smoothScrollToPosition(0);
                            updateEmptyState();
                        });
            });
        }).addOnFailureListener(e -> {
            progress.dismiss();
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    @Override
    public void onDocumentClicked(Document document) {
        // Open document
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(document.getDownloadUrl()), getMimeType(document.getType()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onDocumentDeleted(Document document) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete " + document.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument(document))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDocument(Document document) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Deleting document...");
        progress.show();

        // Delete from storage
        StorageReference storageRef = storage.getReferenceFromUrl(document.getDownloadUrl());
        storageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Delete from Firestore
                    db.collection("documents").document(document.getId())
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                progress.dismiss();
                                int position = documents.indexOf(document);
                                documents.remove(position);
                                adapter.notifyItemRemoved(position);
                                updateEmptyState();
                            });
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    Toast.makeText(this, "Failed to delete document", Toast.LENGTH_SHORT).show();
                });
    }

    private String getMimeType(String type) {
        switch (type.toLowerCase()) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            default: return "*/*";
        }
    }
}