package com.sti.paceloop.viewmodel;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sti.paceloop.database.GeneratedMaterial;
import com.sti.paceloop.repository.AppRepository;
import com.sti.paceloop.utils.GeminiAIManager;

public class PdfViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final GeminiAIManager aiManager;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<StudyMaterial> studyMaterial = new MutableLiveData<>();

    public PdfViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        aiManager = new GeminiAIManager();
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<StudyMaterial> getStudyMaterial() {
        return studyMaterial;
    }

    public void generateContent(Uri pdfUri, String difficulty, String fileName) {
        isLoading.setValue(true);
        aiManager.generateStudyMaterial(getApplication(), pdfUri, difficulty, new GeminiAIManager.AIResponseCallback() {
            @Override
            public void onSuccess(String reviewer, String quiz, String answerKey) {
                isLoading.postValue(false);
                studyMaterial.postValue(new StudyMaterial(reviewer, quiz, answerKey));
                
                // Save to DB via Repository
                GeneratedMaterial material = new GeneratedMaterial();
                material.fileName = fileName;
                material.reviewerContent = reviewer;
                material.quizContent = quiz;
                material.answerKeyContent = answerKey;
                material.difficulty = difficulty;
                material.timestamp = System.currentTimeMillis();
                repository.insertMaterial(material);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorLiveData.postValue(error);
            }
        });
    }

    public static class StudyMaterial {
        public final String reviewer;
        public final String quiz;
        public final String answers;

        public StudyMaterial(String reviewer, String quiz, String answers) {
            this.reviewer = reviewer;
            this.quiz = quiz;
            this.answers = answers;
        }
    }
}
