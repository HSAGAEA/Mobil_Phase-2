package com.sti.paceloop.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiAIManager {
    private static final String API_KEY = "AIzaSyCp2uTq8B4eJq6gYAsjEmcDfoMam10_WjE";
    private GenerativeModelFutures model;
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool(); // OPTIMIZATION

    public GeminiAIManager() {
        try {
            GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", API_KEY);
            model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e("GeminiAI", "Initialization failed", e);
        }
    }

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(Throwable throwable);
    }

    public interface AIResponseCallback {
        void onSuccess(String reviewer, String quiz, String answerKey);
        void onError(String error);
    }

    public void getRecommendation(int stress, int sleep, int mood, int coping, double score, GeminiCallback callback) {
        if (model == null) {
            callback.onError(new Exception("Gemini model not initialized"));
            return;
        }

        String prompt = String.format(Locale.US,
                "Role: Expert Student Wellness Mentor. " +
                        "Task: Provide a concise recommendation (max 2 sentences) for a student: " +
                        "Stress: %d/10, Sleep: %d hrs, Mood: %d/5, Coping: %d/5, Burnout: %.1f/10. " +
                        "Tone: Encouraging.",
                stress, sleep, mood, coping, score);

        generateText(prompt, callback);
    }

    // OPTIMIZATION: Advanced prompt engineering for weighted dynamic thresholds
    public void detectFatigue(int idleTimeSeconds, int interactionCount, int mistakes, GeminiCallback callback) {
        if (model == null) {
            callback.onError(new Exception("Gemini model not initialized"));
            return;
        }

        String prompt = String.format(Locale.US,
                "Analyze mobile learning behavior. " +
                        "Context: Idle time = %d sec, Screen touches = %d, Mistakes = %d. " +
                        "Logic: High idle time (>60s) AND low touches means fatigue. High touches but high mistakes means frustration. " +
                        "Respond strictly with ONE of these phrases based on severity: " +
                        "'Continue' (if focused), 'Warning: Mild fatigue' (if drifting), or 'Take a short break' (if high burnout).",
                idleTimeSeconds, interactionCount, mistakes);

        generateText(prompt, callback);
    }

    private void generateText(String prompt, GeminiCallback callback) {
        Content content = new Content.Builder().addText(prompt).build();
        try {
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            response.addListener(() -> {
                try {
                    GenerateContentResponse result = response.get();
                    String text = result.getText();
                    if (text == null) callback.onError(new Exception("Empty response"));
                    else callback.onSuccess(text.trim());
                } catch (Exception e) {
                    callback.onError(e);
                }
            }, aiExecutor);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public void generateStudyMaterial(Context context, Uri pdfUri, String difficulty, AIResponseCallback callback) {
        if (model == null) {
            callback.onError("Gemini model not initialized");
            return;
        }

        try (InputStream stream = context.getContentResolver().openInputStream(pdfUri)) {
            if (stream == null) {
                callback.onError("Could not open PDF file");
                return;
            }

            byte[] pdfBytes = readBytes(stream);

            String promptText = "Act as an expert educator. Based on the provided PDF content, generate a highly structured study guide.\n" +
                    "Difficulty: " + difficulty + "\n" +
                    "Please format your exact response with these three exact headers: [REVIEWER], [QUIZ], [ANSWERS].";

            Content content = new Content.Builder()
                    .addBlob("application/pdf", pdfBytes)
                    .addText(promptText)
                    .build();

            ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

            responseFuture.addListener(() -> {
                try {
                    GenerateContentResponse result = responseFuture.get();
                    String responseText = result.getText();
                    if (responseText == null) {
                        callback.onError("Empty response from AI");
                    } else {
                        parseAndReturnResponse(responseText, callback);
                    }
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }, aiExecutor);

        } catch (Exception e) {
            callback.onError("Error reading PDF: " + e.getMessage());
        }
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void parseAndReturnResponse(String response, AIResponseCallback callback) {
        if (response == null || response.isEmpty()) {
            callback.onError("Empty response from AI");
            return;
        }

        String reviewer = "", quiz = "", answers = "";
        int reviewerIdx = response.indexOf("[REVIEWER]");
        int quizIdx = response.indexOf("[QUIZ]");
        int answersIdx = response.indexOf("[ANSWERS]");

        if (reviewerIdx != -1) {
            int end = (quizIdx > reviewerIdx) ? quizIdx : (answersIdx > reviewerIdx ? answersIdx : response.length());
            reviewer = response.substring(reviewerIdx + 10, end).trim();
        }
        if (quizIdx != -1) {
            int end = (answersIdx > quizIdx) ? answersIdx : response.length();
            quiz = response.substring(quizIdx + 6, end).trim();
        }
        if (answersIdx != -1) {
            answers = response.substring(answersIdx + 9).trim();
        }

        callback.onSuccess(reviewer, quiz, answers);
    }
}