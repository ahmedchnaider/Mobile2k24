package com.example.mobile2k24.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile2k24.R;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private List<Map<String, Object>> users;
    private static OnUserActionListener listener;

    public interface OnUserActionListener {
        void onEditUser(Map<String, Object> user);
        void onDeleteUser(Map<String, Object> user);
    }

    public UserAdapter(List<Map<String, Object>> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d("UserAdapter", "Creating new ViewHolder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d("UserAdapter", "Binding ViewHolder at position: " + position);
        Map<String, Object> user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<Map<String, Object>> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameText;
        public TextView emailText;
        public TextView roleText;
        public TextView statusText;
        public ImageButton editButton;
        public ImageButton deleteButton;

        public ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.user_name);
            emailText = view.findViewById(R.id.user_email);
            roleText = view.findViewById(R.id.user_role);
            statusText = view.findViewById(R.id.user_status);
            editButton = view.findViewById(R.id.btn_edit);
            deleteButton = view.findViewById(R.id.btn_delete);
        }

        void bind(Map<String, Object> user) {
            String name = (String) user.get("name");
            String email = (String) user.get("email");
            String role = (String) user.get("role");
            String status = (String) user.get("status");

            nameText.setText(name != null ? name : "N/A");
            emailText.setText(email != null ? email : "N/A");
            roleText.setText(role != null ? role : "N/A");

            if (status != null) {
                statusText.setText(status);
                statusText.setVisibility(View.VISIBLE);
                
                if ("active".equals(status)) {
                    statusText.setTextColor(Color.parseColor("#4CAF50")); // Green
                } else {
                    statusText.setTextColor(Color.parseColor("#F44336")); // Red
                }
            } else {
                statusText.setVisibility(View.GONE);
            }

            Log.d("UserAdapter", "Binding user: " + user.toString());

            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditUser(user);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteUser(user);
                }
            });
        }
    }
} 