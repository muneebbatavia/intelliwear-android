package com.example.intelliwear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirstScreen extends AppCompatActivity {
    private Button btAI;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_first_screen);

        btAI = findViewById(R.id.bt_AI_Response);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            userId = user.getUid();
            checkQuestionnaireStatus(userId);
        } else {
            // If user is not logged in, redirect to login activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        btAI.setOnClickListener(v -> handleButtonClick());

        findViewById(R.id.bt_readings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirstScreen.this, ReadingsActivity.class));
            }
        });
    }

    private void checkQuestionnaireStatus(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean questionnaire1 = documentSnapshot.getBoolean("Questionnaire1") != null
                                && documentSnapshot.getBoolean("Questionnaire1");
                        boolean questionnaire2 = documentSnapshot.getBoolean("Questionnaire2") != null
                                && documentSnapshot.getBoolean("Questionnaire2");
                        boolean questionnaire3 = documentSnapshot.getBoolean("Questionnaire3") != null
                                && documentSnapshot.getBoolean("Questionnaire3");

                        if (!questionnaire1 || !questionnaire2 || !questionnaire3) {
                            btAI.setText("Fill Questionnaire");
                        } else {
                            btAI.setText("Get AI Response");
                        }
                        btAI.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void handleButtonClick() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean questionnaire1 = documentSnapshot.getBoolean("Questionnaire1") != null
                                && documentSnapshot.getBoolean("Questionnaire1");
                        boolean questionnaire2 = documentSnapshot.getBoolean("Questionnaire2") != null
                                && documentSnapshot.getBoolean("Questionnaire2");
                        boolean questionnaire3 = documentSnapshot.getBoolean("Questionnaire3") != null
                                && documentSnapshot.getBoolean("Questionnaire3");

                        if (!questionnaire1) {
                            startActivity(new Intent(FirstScreen.this, Questionnaire1.class));
                        } else if (!questionnaire2) {
                            startActivity(new Intent(FirstScreen.this, Questionnaire2.class));
                        } else if (!questionnaire3) {
                            startActivity(new Intent(FirstScreen.this, Questionnaire3.class));
                        } else {
                            startActivity(new Intent(FirstScreen.this, AIResponseActivity.class));
                        }
                    }
                });
    }
}
