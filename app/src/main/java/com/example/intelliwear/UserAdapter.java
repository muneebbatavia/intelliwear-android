package com.example.intelliwear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private ArrayList<AppUser> users;
    private Context context;
    private AdminPanelActivity adminPanel;
    private Boolean isApprovedList;

    public UserAdapter(ArrayList<AppUser> users, Context context, boolean isApprovedList) {
        this.users = users;
        this.context = context;
        this.adminPanel = (AdminPanelActivity) context;
        this.isApprovedList = isApprovedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppUser user = users.get(position);
        holder.userName.setText(user.getName());
        holder.userEmail.setText(user.getEmail());

        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            // If user is an admin, hide all buttons
            holder.approveBtn.setVisibility(View.GONE);
            holder.disapproveBtn.setVisibility(View.GONE);
            holder.deleteBtn.setVisibility(View.GONE);
        } else {
            // Normal users
            if (isApprovedList) {
                holder.approveBtn.setVisibility(View.GONE);
                holder.disapproveBtn.setVisibility(View.VISIBLE);
            } else {
                holder.approveBtn.setVisibility(View.VISIBLE);
                holder.disapproveBtn.setVisibility(View.GONE);
            }
            holder.deleteBtn.setVisibility(View.VISIBLE);
        }

        // Click Listeners
        holder.approveBtn.setOnClickListener(v -> adminPanel.approveUser(user.getId()));
        holder.disapproveBtn.setOnClickListener(v -> adminPanel.disapproveUser(user.getId()));
        holder.deleteBtn.setOnClickListener(v -> adminPanel.deleteUser(user.getId()));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userEmail;
        ImageView approveBtn, disapproveBtn, deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            approveBtn = itemView.findViewById(R.id.approveBtn);
            disapproveBtn = itemView.findViewById(R.id.disapproveBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}
