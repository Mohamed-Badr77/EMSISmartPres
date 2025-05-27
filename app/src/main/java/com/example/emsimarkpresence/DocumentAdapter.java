package com.example.emsimarkpresence;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {
    private List<Document> documents;
    private OnDocumentActionListener listener;
    private Context context;

    public interface OnDocumentActionListener {
        void onDocumentClicked(Document document);
        void onDocumentDeleted(Document document);
    }

    public DocumentAdapter(Context context, List<Document> documents, OnDocumentActionListener listener) {
        this.context = context;
        this.documents = documents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documents.get(position);

        // Set document icon based on type
        int iconRes = getIconForType(document.getType());
        holder.ivDocumentIcon.setImageResource(iconRes);

        holder.tvDocumentName.setText(document.getName());

        // Format info text (size and date)
        String size = formatFileSize(document.getSize());
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(document.getUploadDate());
        holder.tvDocumentInfo.setText(String.format("%s • %s", size, date));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentClicked(document);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDocumentDeleted(document);
            }
        });
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDocumentIcon;
        TextView tvDocumentName, tvDocumentInfo;
        ImageButton btnDelete;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDocumentIcon = itemView.findViewById(R.id.ivDocumentIcon);
            tvDocumentName = itemView.findViewById(R.id.tvDocumentName);
            tvDocumentInfo = itemView.findViewById(R.id.tvDocumentInfo);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private int getIconForType(String type) {
        switch (type.toLowerCase()) {
            case "pdf": return R.drawable.ic_pdf;
            case "doc":
            case "docx": return R.drawable.ic_word;
            case "xls":
            case "xlsx": return R.drawable.ic_excel;
            case "ppt":
            case "pptx": return R.drawable.ic_powerpoint;
            case "jpg":
            case "jpeg":
            case "png": return R.drawable.ic_image;
            default: return R.drawable.ic_file;
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}