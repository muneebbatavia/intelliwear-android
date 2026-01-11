package com.example.intelliwear;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class AIResponseActivity extends AppCompatActivity {

    private LinearLayout linearLayoutAIResponse;
    private ProgressBar progressBar;
    private LinearLayout progressBarContainer;
    private TextView progressText;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_airesponse);

        // Initialize Views
        linearLayoutAIResponse = findViewById(R.id.linearLayoutAIResponse);
        progressBar = findViewById(R.id.progressBar);
        progressBarContainer = findViewById(R.id.progress_bar_container);
        progressText = findViewById(R.id.progress_text);
        db = FirebaseFirestore.getInstance();

        // Get Current User ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Button click listener
        findViewById(R.id.reFetch_btn).setOnClickListener(view -> fetchAndDisplayAIResponse(true));

        linearLayoutAIResponse.removeAllViews();
        fetchAndDisplayAIResponse(false);
    }

    private void fetchAndDisplayAIResponse(boolean forceFetchNew) {
        if (forceFetchNew) {
            fallbackToNewFetch("Fetching Response from AI");
            return;
        }

        DocumentReference docRef = db.collection("users").document(userId)
                .collection("ai_responses").document("response");

        docRef.get().addOnCompleteListener(task -> {
            String aiResponse = null;

            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                aiResponse = task.getResult().getString("response");
            }

            if (aiResponse != null && !aiResponse.isEmpty()) {
                progressText.setText("Fetching Your Previous Response");
                progressBarContainer.setVisibility(View.GONE);
                displayResponseInFormattedWay(aiResponse);
            } else {
                fallbackToNewFetch("Fetching Response from AI");
            }
        });
    }

    private void fallbackToNewFetch(String message) {
        progressText.setText(message);
        linearLayoutAIResponse.removeAllViews();
        progressBarContainer.setVisibility(View.VISIBLE);
        fetchAIResponseFromService();
    }


    private void fetchAIResponseFromService() {
        AIHelper.sendQuestionnaireToAI(new AIHelper.ResultCallback() {
            @Override
            public void onSuccess(String aiResponse) {
                runOnUiThread(() -> {
                    progressBarContainer.setVisibility(View.GONE);
                    if (aiResponse != null && !aiResponse.isEmpty()) {
                        saveAIResponseToFirestore(aiResponse);
                        displayResponseInFormattedWay(aiResponse);
                    } else {
                        Toast.makeText(AIResponseActivity.this, "Received empty AI response.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    progressBarContainer.setVisibility(View.GONE);
                    Toast.makeText(AIResponseActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveAIResponseToFirestore(String aiResponse) {
        DocumentReference docRef = db.collection("users").document(userId)
                .collection("ai_responses").document("response");

        docRef.set(new AIResponse(aiResponse))
                .addOnSuccessListener(aVoid -> Log.d("AI RESPONSE", "Response saved to Firestore"))
                .addOnFailureListener(e -> Log.e("AI RESPONSE", "Error saving response to Firestore", e));
    }

    private void displayResponseInFormattedWay(String aiResponse) {
        List<Section> sections = parseDynamicResponse(aiResponse);

        for (Section section : sections) {
            LinearLayout sectionLayout = new LinearLayout(this);
            sectionLayout.setOrientation(LinearLayout.VERTICAL);
            sectionLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            sectionLayout.setPadding(32, 32, 32, 32);

            LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            sectionParams.setMargins(0, 0, 0, 32);
            sectionLayout.setLayoutParams(sectionParams);

            TextView headingTextView = new TextView(this);
            headingTextView.setText(Html.fromHtml("<b>" + section.getTitle() + "</b>"));
            headingTextView.setTextSize(20);
            headingTextView.setTextColor(getResources().getColor(R.color.app_color));


            sectionLayout.addView(headingTextView);

            for (String item : section.getItems()) {
                TextView bulletTextView = new TextView(this);
                bulletTextView.setText(Html.fromHtml("&#8226; " + item.trim()));
                bulletTextView.setTextSize(16);
                bulletTextView.setTextColor(getResources().getColor(android.R.color.black));

                LinearLayout.LayoutParams bulletParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                bulletParams.setMargins(16, 8, 0, 8);
                bulletTextView.setLayoutParams(bulletParams);

                sectionLayout.addView(bulletTextView);
            }

            linearLayoutAIResponse.addView(sectionLayout);
        }
    }

    private List<Section> parseDynamicResponse(String response) {
        List<Section> sections = new ArrayList<>();
        Pattern sectionPattern = Pattern.compile("^(\\*\\*)?(Breast\\s+Cancer\\s+Risk|Lifestyle\\s+Precautions|Future\\s+Suggestions\\s+or\\s+Recommendations)(\\*\\*)?$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Pattern bulletPointPattern = Pattern.compile("^\\s*(\\*)\\s+(.*)$", Pattern.MULTILINE);

        String currentSectionTitle = null;
        List<String> currentItems = new ArrayList<>();

        for (String line : response.split("\n")) {
            Matcher sectionMatcher = sectionPattern.matcher(line.trim());
            Matcher bulletMatcher = bulletPointPattern.matcher(line);

            if (sectionMatcher.find()) {
                if (currentSectionTitle != null) {
                    sections.add(new Section(currentSectionTitle, new ArrayList<>(currentItems)));
                    currentItems.clear();
                }
                currentSectionTitle = sectionMatcher.group(2).trim();
            } else if (bulletMatcher.find() && currentSectionTitle != null) {
                currentItems.add(bulletMatcher.group(2).trim());
            }
        }

        if (currentSectionTitle != null) {
            sections.add(new Section(currentSectionTitle, currentItems));
        }

        return sections;
    }
}
