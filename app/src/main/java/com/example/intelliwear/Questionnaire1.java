package com.example.intelliwear;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Questionnaire1 extends AppCompatActivity {

    private EditText edDob;
    private Spinner spinnerFamilyHistory, spinnerDiagnosis, spinnerImplants, spinnerSurgeries;
    private CheckBox cbLump, cbPain, cbDischarge, cbSkinChanges, cbNone;
    private Button btnNext;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire_1);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        edDob = findViewById(R.id.ed_dob);
        spinnerFamilyHistory = findViewById(R.id.spinner_family_history);
        spinnerDiagnosis = findViewById(R.id.spinner_diagnosis);
        spinnerImplants = findViewById(R.id.spinner_implants);
        spinnerSurgeries = findViewById(R.id.spinner_surgeries);
        btnNext = findViewById(R.id.bt_next);

        cbLump = findViewById(R.id.cbLump);
        cbPain = findViewById(R.id.cbPain);
        cbDischarge = findViewById(R.id.cbDischarge);
        cbSkinChanges = findViewById(R.id.cbIrritation);
        cbNone = findViewById(R.id.cbNone);

        // Set DatePicker on EditText (DOB)
        edDob.setOnClickListener(v -> showDatePickerDialog());

        // Set up Spinners with Yes/No options
        ArrayAdapter<CharSequence> adapterYesNo = ArrayAdapter.createFromResource(this,
                R.array.yes_no_options, android.R.layout.simple_spinner_item);
        adapterYesNo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFamilyHistory.setAdapter(adapterYesNo);
        spinnerDiagnosis.setAdapter(adapterYesNo);
        spinnerImplants.setAdapter(adapterYesNo);
        spinnerSurgeries.setAdapter(adapterYesNo);

        // Handle "None" checkbox logic
        cbNone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbLump.setChecked(false);
                cbPain.setChecked(false);
                cbDischarge.setChecked(false);
                cbSkinChanges.setChecked(false);
            }
        });

        View.OnClickListener symptomClickListener = v -> {
            if (cbLump.isChecked() || cbPain.isChecked() || cbDischarge.isChecked() || cbSkinChanges.isChecked()) {
                cbNone.setChecked(false);
            }
        };

        cbLump.setOnClickListener(symptomClickListener);
        cbPain.setOnClickListener(symptomClickListener);
        cbDischarge.setOnClickListener(symptomClickListener);
        cbSkinChanges.setOnClickListener(symptomClickListener);

        // Submit Button Click Listener
        btnNext.setOnClickListener(v -> saveQuestionnaireData());
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) ->
                        edDob.setText(selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear),
                year, month, day);
        datePickerDialog.show();
    }

    private void saveQuestionnaireData() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        // Collect user input data
        String dob = edDob.getText().toString().trim();
        String familyHistory = spinnerFamilyHistory.getSelectedItem().toString();
        String diagnosis = spinnerDiagnosis.getSelectedItem().toString();
        String implants = spinnerImplants.getSelectedItem().toString();
        String surgeries = spinnerSurgeries.getSelectedItem().toString();

        // Validate all fields
        if (dob.isEmpty()) {
            Toast.makeText(this, "Please enter Date of Birth!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (familyHistory.equals("Select Option...") || diagnosis.equals("Select Option...") ||
                implants.equals("Select Option...") || surgeries.equals("Select Option...")) {
            Toast.makeText(this, "Please select all options!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect symptoms
        List<String> symptoms = new ArrayList<>();
        if (cbLump.isChecked()) symptoms.add("Lump");
        if (cbPain.isChecked()) symptoms.add("Pain");
        if (cbDischarge.isChecked()) symptoms.add("Discharge");
        if (cbSkinChanges.isChecked()) symptoms.add("Irritation or Dimpling");
        if (cbNone.isChecked()) symptoms.add("None");

        // Ensure at least one symptom is selected
        if (symptoms.isEmpty()) {
            Toast.makeText(this, "Please select at least one symptom!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a data map
        Map<String, Object> questionnaireData = new HashMap<>();
        questionnaireData.put("dob", dob);
        questionnaireData.put("familyHistory", familyHistory);
        questionnaireData.put("diagnosis", diagnosis);
        questionnaireData.put("implants", implants);
        questionnaireData.put("surgeries", surgeries);
        questionnaireData.put("symptoms", symptoms);

        // Store questionnaire data in Firestore
        db.collection("users").document(userId).collection("questionnaire")
                .document("questionnaire1")
                .set(questionnaireData)
                .addOnSuccessListener(aVoid -> {
                    // Update "Questionnaire1" field as true inside user document
                    db.collection("users").document(userId)
                            .update("Questionnaire1", true)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(Questionnaire1.this, "Data Saved Successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Questionnaire1.this, Questionnaire2.class)); // Proceed to next questionnaire
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(Questionnaire1.this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Questionnaire1.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
