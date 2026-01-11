package com.example.intelliwear;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Map;

public class AIHelper {

    private static final String TAG = "AIHelper";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String GEMINI_API_KEY = "AIzaSyBoGVyPmNvpyTOD_5f0bH8RwgH5msaleJI"; // <-- Put your Gemini API key here

    public interface ResultCallback {
        void onSuccess(String aiResponse);
        void onFailure(String errorMessage);
    }

    public static void sendQuestionnaireToAI(ResultCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("questionnaire")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        StringBuilder combinedData = new StringBuilder();

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Map<String, Object> map = doc.getData();
                            if (map != null) {
                                for (Map.Entry<String, Object> entry : map.entrySet()) {
                                    combinedData.append(entry.getKey())
                                            .append(": ")
                                            .append(entry.getValue())
                                            .append("\n");
                                }
                            }
                        }

                        callAIAPI(combinedData.toString(), userId, callback);

                    } else {
                        callback.onFailure("No questionnaire data found.");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                });
    }

    private static void callAIAPI(String userInfo, String userId, ResultCallback callback) {
        OkHttpClient client = new OkHttpClient();

        JSONObject prompt = new JSONObject();
        try {
            JSONObject content = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", "Based on the following health information, provide a structured summary covering three key areas. Ensure each area is clearly labeled with a distinct heading, followed by the relevant information in bullet points.\n\n**Health Information:**\n" + userInfo + "\n\n**Summary Requirements:**\n\n1.  **Breast Cancer Risk:** Present a concise summary of the individual's breast cancer risk factors based on the provided information. Use bullet points to list each identified risk or mitigating factor along with a brief explanation.\n\n2.  **Lifestyle Precautions:** Outline any lifestyle factors mentioned in the health information that the individual should be aware of or consider modifying for better health. Use bullet points to list each precaution and a brief note.\n\n3.  **Future Suggestions or Recommendations:** Provide actionable suggestions or recommendations based on the health information. These could include lifestyle changes, screenings, or consultations. Use bullet points for each recommendation.\n\n**Formatting Guidelines:**\n\nThe response MUST adhere to the following format:\n\n**Breast Cancer Risk**\n* [Bullet point 1: Risk factor/mitigating factor and explanation]\n* [Bullet point 2: ...]\n* ...\n\n**Lifestyle Precautions**\n* [Bullet point 1: Precaution and note]\n* [Bullet point 2: ...]\n* ...\n\n**Future Suggestions or Recommendations**\n* [Bullet point 1: Recommendation]\n* [Bullet point 2: ...]\n* ...\n\nDo not include any introductory or concluding sentences outside of the specified headings and bullet points. Focus solely on providing the requested information under each heading in the bulleted format.\n" + userInfo);
            partsArray.put(part);

            content.put("parts", partsArray);

            prompt.put("contents", new JSONArray().put(content));

        } catch (Exception e) {
            e.printStackTrace();
            callback.onFailure("Error creating JSON body.");
            return;
        }

        RequestBody body = RequestBody.create(
                prompt.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(GEMINI_API_URL + "?key=" + GEMINI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject responseJson = new JSONObject(responseData);
                        JSONArray candidates = responseJson.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String aiOutput = parts.getJSONObject(0).getString("text");

                        saveAIResponseToFirestore(userId, aiOutput);
                        callback.onSuccess(aiOutput);

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onFailure("Error parsing AI response.");
                    }
                } else {
                    callback.onFailure("AI API error: " + response.message());
                }
            }
        });
    }

    private static void saveAIResponseToFirestore(String userId, String aiResponse) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(userId)
                .collection("ai_responses")
                .add(new AIResponse(aiResponse))
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "AI response saved successfully to Firestore.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving AI response: " + e.getMessage());
                });
    }
}
