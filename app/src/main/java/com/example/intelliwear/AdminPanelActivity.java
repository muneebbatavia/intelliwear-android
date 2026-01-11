package com.example.intelliwear;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import java.util.ArrayList;

public class AdminPanelActivity extends AppCompatActivity {
    private RecyclerView recyclerApproved, recyclerUnapproved;
    private UserAdapter approvedAdapter, unapprovedAdapter;
    private ArrayList<AppUser> approvedUsers = new ArrayList<>();
    private ArrayList<AppUser> unapprovedUsers = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        recyclerApproved = findViewById(R.id.recyclerApproved);
        recyclerUnapproved = findViewById(R.id.recyclerUnapproved);
        db = FirebaseFirestore.getInstance();

        recyclerApproved.setLayoutManager(new LinearLayoutManager(this));
        recyclerUnapproved.setLayoutManager(new LinearLayoutManager(this));

        approvedAdapter = new UserAdapter(approvedUsers, this, true);
        unapprovedAdapter = new UserAdapter(unapprovedUsers, this, false);

        recyclerApproved.setAdapter(approvedAdapter);
        recyclerUnapproved.setAdapter(unapprovedAdapter);

        fetchUsers();
    }

    private void fetchUsers() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore Error", error.getMessage());
                return;
            }

            if (value == null) return;

            approvedUsers.clear();
            unapprovedUsers.clear();

            for (DocumentSnapshot doc : value.getDocuments()) {

                AppUser user = doc.toObject(AppUser.class);
                if (user != null) {
                    user.setId(doc.getId());

                    // 🔹 Manually handle isApproved mapping
                    if (doc.contains("isApproved")) {
                        Boolean isApproved = doc.getBoolean("isApproved");
                        user.setApproved(isApproved != null ? isApproved : false);
                    } else {
                        user.setApproved(false); // Default
                    }

                    Log.d("Firestore Parsed Data", "User: " + user.getName() + ", isApproved: " + user.isApproved());

                    if (user.isApproved()) {
                        approvedUsers.add(user);
                    } else {
                        unapprovedUsers.add(user);
                    }
                }
            }

            runOnUiThread(() -> {
                approvedAdapter.notifyDataSetChanged();
                unapprovedAdapter.notifyDataSetChanged();
            });

            Log.d("Firestore", "Users Loaded: Approved=" + approvedUsers.size() + ", Unapproved=" + unapprovedUsers.size());
        });
    }

    public void approveUser(String userId) {
        db.collection("users").document(userId).update("isApproved", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User Approved: " + userId);
                    moveUserBetweenLists(userId, true);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error approving user", e));
    }

    public void disapproveUser(String userId) {
        db.collection("users").document(userId).update("isApproved", false)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User Disapproved: " + userId);
                    moveUserBetweenLists(userId, false);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error disapproving user", e));
    }

    public void deleteUser(String userId) {
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    approvedUsers.removeIf(user -> user.getId().equals(userId));
                    unapprovedUsers.removeIf(user -> user.getId().equals(userId));

                    runOnUiThread(() -> {
                        approvedAdapter.notifyDataSetChanged();
                        unapprovedAdapter.notifyDataSetChanged();
                    });

                    Log.d("Firestore", "User Deleted: " + userId);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting user", e));
    }

    private void moveUserBetweenLists(String userId, boolean toApproved) {
        AppUser movedUser = null;

        if (toApproved) {
            for (AppUser user : unapprovedUsers) {
                if (user.getId().equals(userId)) {
                    user.setApproved(true);
                    movedUser = user;
                    break;
                }
            }
            if (movedUser != null) {
                unapprovedUsers.remove(movedUser);
                approvedUsers.add(movedUser);
            }
        } else {
            for (AppUser user : approvedUsers) {
                if (user.getId().equals(userId)) {
                    user.setApproved(false);
                    movedUser = user;
                    break;
                }
            }
            if (movedUser != null) {
                approvedUsers.remove(movedUser);
                unapprovedUsers.add(movedUser);
            }
        }

        // Update UI
        runOnUiThread(() -> {
            approvedAdapter.notifyDataSetChanged();
            unapprovedAdapter.notifyDataSetChanged();
        });
    }
}
