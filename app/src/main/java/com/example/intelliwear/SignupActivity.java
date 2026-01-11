package com.example.intelliwear;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText edName, edPhone, edEmail, edPassword, edConfirmPassword;

    private CheckBox chkbox;

    private ImageView imgEye, imgEye2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        chkbox =findViewById(R.id.checkBox);

        final boolean[] isPasswordVisible = {false};
        final boolean[] isPasswordVisible2 = {false};
        imgEye = findViewById(R.id.img_eye1);
        imgEye2 = findViewById(R.id.img_eye2);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edName = findViewById(R.id.ed_name);
        edPhone = findViewById(R.id.ed_phone);
        edEmail = findViewById(R.id.ed_email);
        edPassword = findViewById(R.id.ed_password);
        edConfirmPassword = findViewById(R.id.ed_confirm_password);

        findViewById(R.id.bt_signup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        findViewById(R.id.login_create2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
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

        imgEye2.setOnClickListener(v -> {
            if (isPasswordVisible2[0]) {
                // Hide Password
                edConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                isPasswordVisible2[0] = false;
            } else {
                // Show Password
                edConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                isPasswordVisible2[0] = true;
            }
            // Move cursor to the end
            edConfirmPassword.setSelection(edConfirmPassword.length());
        });
    }

    private void registerUser() {
        String name = edName.getText().toString().trim();
        String phone = edPhone.getText().toString().trim();
        String email = edEmail.getText().toString().trim();
        String password = edPassword.getText().toString().trim();
        String password2 = edConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(SignupActivity.this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!chkbox.isChecked()){
            Toast.makeText(SignupActivity.this, "Accept Terms & Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(password2)) {
            Toast.makeText(SignupActivity.this, "Passwords are not same!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user.getUid(), name, phone, email);
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String userId, String name, String phone, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("phone", phone);
        userMap.put("email", email);
        userMap.put("isApproved", false);  // User needs admin approval
        userMap.put("Questionnaire1", false); // Default to false
        userMap.put("Questionnaire2", false); // Default to false
        userMap.put("Questionnaire3", false); // Default to false

        db.collection("users").document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> showApprovalPopup())
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();  // Print to logcat for deeper debug
                });

    }

    private void showApprovalPopup() {
        new AlertDialog.Builder(this)
                .setTitle("Account Pending Approval")
                .setMessage("Your registration has been received. You will be able to log in once it is reviewed and approved, typically within 24 hours.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

}
