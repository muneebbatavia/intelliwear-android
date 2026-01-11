package com.example.intelliwear;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText edEmail, edPassword;
    private ImageView imgEye;

    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final boolean[] isPasswordVisible = {false};

        imgEye = findViewById(R.id.img_eye1);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edEmail = findViewById(R.id.ed_email);
        edPassword = findViewById(R.id.ed_password);

        tvForgotPassword = findViewById(R.id.forgot_pass);

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });

        findViewById(R.id.bt_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        findViewById(R.id.signup_create2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });

        imgEye.setOnClickListener(v -> {
            if (isPasswordVisible[0]) {
                // Hide Password
                edPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                isPasswordVisible[0] = false;
            } else {
                // Show Password
                edPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                isPasswordVisible[0] = true;
            }
            // Move cursor to the end
            edPassword.setSelection(edPassword.length());
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        builder.setView(emailInput);

        builder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = emailInput.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Enter your registered email", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Password reset email sent!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loginUser() {
        String email = edEmail.getText().toString().trim();
        String password = edPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkApprovalStatus(user.getUid());
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkApprovalStatus(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean isApproved = documentSnapshot.getBoolean("isApproved") != null
                                && documentSnapshot.getBoolean("isApproved");

                        boolean isAdmin = documentSnapshot.getBoolean("isAdmin") != null
                                && documentSnapshot.getBoolean("isAdmin");

                        Intent splashIntent = new Intent(LoginActivity.this, SplashActivity.class);
                        String nextActivity;

                        if (isAdmin) {
                            nextActivity = "com.example.intelliwear.AdminPanelActivity";
                        } else if (isApproved) {
                            nextActivity = "com.example.intelliwear.FirstScreen";
                        } else {
                            Toast.makeText(LoginActivity.this, "Your account is not approved yet.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            return;
                        }

                        splashIntent.putExtra("nextActivity", nextActivity);
                        startActivity(splashIntent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

}
