package com.example.intelliwear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Questionnaire2 extends AppCompatActivity {

    private EditText edFirstCycle, edFirstChild;
    private Spinner spinnerMenuOrPost, spinnerPregnant, spinnerMedications;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire_2);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        edFirstCycle = findViewById(R.id.edFirstCycle);
        edFirstChild = findViewById(R.id.edFirstChild);
        spinnerMenuOrPost = findViewById(R.id.spinner_menu_or_post);
        spinnerPregnant = findViewById(R.id.spinner_pregnant);
        spinnerMedications = findViewById(R.id.spinner_medications);

        // Set up spinners with Yes/No options
        ArrayAdapter<CharSequence> adapterYesNo = ArrayAdapter.createFromResource(this,
                R.array.yes_no_options, android.R.layout.simple_spinner_item);
        adapterYesNo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerMenuOrPost.setAdapter(adapterYesNo);
        spinnerPregnant.setAdapter(adapterYesNo);
        spinnerMedications.setAdapter(adapterYesNo);

        // Handle enabling/disabling edFirstChild based on pregnancy status
        spinnerPregnant.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (spinnerPregnant.getSelectedItem().toString().equals("Yes")) {
                    edFirstChild.setEnabled(true);
                    edFirstChild.setBackgroundColor(getResources().getColor(android.R.color.white));
                } else {
                    edFirstChild.setEnabled(false);
                    edFirstChild.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    edFirstChild.setText(""); // Clear the field if it's disabled
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.bt_next2).setOnClickListener(v -> saveQuestionnaireData());
    }

    private void saveQuestionnaireData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        // Collecting Data
        String firstCycle = edFirstCycle.getText().toString().trim();
        String firstChild = edFirstChild.getText().toString().trim();
        String menuOrPost = spinnerMenuOrPost.getSelectedItem().toString();
        String pregnant = spinnerPregnant.getSelectedItem().toString();
        String medications = spinnerMedications.getSelectedItem().toString();

        // Validation: Ensure required fields are not empty
        if (firstCycle.isEmpty() || menuOrPost.equals("Select Option...") || pregnant.equals("Select Option...") || medications.equals("Select Option...")) {
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creating a Map to store data
        Map<String, Object> questionnaireData = new HashMap<>();
        questionnaireData.put("firstCycleAge", firstCycle);
        questionnaireData.put("isMenopausal", menuOrPost);
        questionnaireData.put("hasBeenPregnant", pregnant);
        questionnaireData.put("isOnMedications", medications);

        // Store firstChild only if pregnant is "Yes"
        if (pregnant.equals("Yes")) {
            if (firstChild.isEmpty()) {
                Toast.makeText(this, "Please enter the age at first childbirth!", Toast.LENGTH_SHORT).show();
                return;
            }
            questionnaireData.put("firstChildAge", firstChild);
        }

        // Save Data in Firestore under Users -> userId -> Questionnaire2
        db.collection("users").document(userId).collection("questionnaire")
                .document("questionnaire2")
                .set(questionnaireData)
                .addOnSuccessListener(aVoid -> {
                    // Add Questionnaire2 field to user document
                    db.collection("users").document(userId)
                            .update("Questionnaire2", true);

                    Toast.makeText(Questionnaire2.this, "Data Saved Successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Questionnaire2.this, Questionnaire3.class)); // Proceed to next questionnaire
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Questionnaire2.this, "Failed to Save Data!", Toast.LENGTH_SHORT).show());
    }
}
