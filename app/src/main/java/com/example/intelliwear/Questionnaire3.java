package com.example.intelliwear;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Questionnaire3 extends AppCompatActivity {

    private Spinner spinnerSmoke, spinnerAlcohol, spinnerExercise, spinnerDiet, spinnerAllergies, spinnerSelfExam;
    private EditText edDietOther, edAllergies;
    private Button btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire_3);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        spinnerSmoke = findViewById(R.id.spinner_smoke);
        spinnerAlcohol = findViewById(R.id.spinner_alcohol);
        spinnerExercise = findViewById(R.id.spinner_exercise);
        spinnerDiet = findViewById(R.id.spinner_diet);
        spinnerAllergies = findViewById(R.id.spinner_allergies);
        spinnerSelfExam = findViewById(R.id.spinner_self_exam);
        edDietOther = findViewById(R.id.edDietOther);
        edAllergies = findViewById(R.id.edAllergies);
        btnSave = findViewById(R.id.bt_next3);

        ArrayAdapter<CharSequence> adapterYesNoOnly = ArrayAdapter.createFromResource(this,
                R.array.yes_no_only, android.R.layout.simple_spinner_item);
        adapterYesNoOnly.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSmoke.setAdapter(adapterYesNoOnly);
        spinnerAlcohol.setAdapter(adapterYesNoOnly);
        setSpinnerAdapter(spinnerExercise, new String[]{"Select Option...", "Daily", "Weekly", "Occasionally", "Rarely or never"});
        setSpinnerAdapter(spinnerDiet, new String[]{"Select Option...", "Balanced", "High in processed foods", "Other (Specify)"});
        setSpinnerAdapter(spinnerAllergies, new String[]{"Select Option...", "Yes (Specify)", "No"});
        setSpinnerAdapter(spinnerSelfExam, new String[]{"Select Option...", "Regularly", "Occasionally", "Rarely or Never"});

        // Disable fields initially
        edDietOther.setEnabled(false);
        edAllergies.setEnabled(false);

        // Handle enabling/disabling edDietOther based on spinnerDiet selection
        spinnerDiet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                edDietOther.setEnabled(spinnerDiet.getSelectedItem().toString().equals("Other (Specify)"));
                edDietOther.setBackgroundColor(getResources().getColor(android.R.color.white));
                if (!edDietOther.isEnabled()) {
                    edDietOther.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));// Clear if disabled
                    edDietOther.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Handle enabling/disabling edAllergies based on spinnerAllergies selection
        spinnerAllergies.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                edAllergies.setEnabled(spinnerAllergies.getSelectedItem().toString().equals("Yes (Specify)"));
                edAllergies.setBackgroundColor(getResources().getColor(android.R.color.white));
                if (!edAllergies.isEnabled()) {
                    edAllergies.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    edAllergies.setText(""); // Clear if disabled
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Save data when button is clicked
        btnSave.setOnClickListener(v -> saveQuestionnaireData());
    }

    private void setSpinnerAdapter(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        spinner.setAdapter(adapter);
    }

    private void saveQuestionnaireData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        // Collecting Data
        String smoke = spinnerSmoke.getSelectedItem().toString();
        String alcohol = spinnerAlcohol.getSelectedItem().toString();
        String exercise = spinnerExercise.getSelectedItem().toString();
        String diet = spinnerDiet.getSelectedItem().toString();
        String allergies = spinnerAllergies.getSelectedItem().toString();
        String selfExam = spinnerSelfExam.getSelectedItem().toString();

        // Validate required fields
        if (smoke.equals("Select Option...") || alcohol.equals("Select Option...") || exercise.equals("Select Option...") || diet.equals("Select Option...") || allergies.equals("Select Option...") || selfExam.equals("Select Option...")) {
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store edDietOther only if diet is "Other (Specify)"
        String dietOther = edDietOther.isEnabled() ? edDietOther.getText().toString().trim() : null;

        // Store edAllergies only if allergies is "Yes (Specify)"
        String allergyDetails = edAllergies.isEnabled() ? edAllergies.getText().toString().trim() : null;

        // Validate conditional fields
        if (edDietOther.isEnabled() && dietOther.isEmpty()) {
            Toast.makeText(this, "Please specify your diet!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (edAllergies.isEnabled() && allergyDetails.isEmpty()) {
            Toast.makeText(this, "Please specify your allergies!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creating a Map to store data
        Map<String, Object> questionnaireData = new HashMap<>();
        questionnaireData.put("smoke", smoke);
        questionnaireData.put("alcohol", alcohol);
        questionnaireData.put("exercise", exercise);
        questionnaireData.put("diet", diet);
        if (dietOther != null) questionnaireData.put("dietOther", dietOther);
        questionnaireData.put("allergies", allergies);
        if (allergyDetails != null) questionnaireData.put("allergyDetails", allergyDetails);
        questionnaireData.put("selfExam", selfExam);

        // Save Data in Firestore under Users -> userId -> Questionnaire3
        db.collection("users").document(userId).collection("questionnaire")
                .document("questionnaire3")
                .set(questionnaireData)
                .addOnSuccessListener(aVoid -> {
                    // Mark Questionnaire3 as completed
                    db.collection("users").document(userId)
                            .update("Questionnaire3", true)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(Questionnaire3.this, "Data Saved Successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Questionnaire3.this, FirstScreen.class)); // Move to next activity
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(Questionnaire3.this, "Failed to update Questionnaire3 status!", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(Questionnaire3.this, "Failed to Save Data!", Toast.LENGTH_SHORT).show());
    }
}
