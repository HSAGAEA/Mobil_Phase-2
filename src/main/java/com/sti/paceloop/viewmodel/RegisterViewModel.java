package com.sti.paceloop.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sti.paceloop.database.User;
import com.sti.paceloop.repository.AppRepository;
import com.sti.paceloop.utils.SecurityUtils;

public class RegisterViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
    }

    public LiveData<Boolean> getRegisterSuccess() {
        return registerSuccess;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public void register(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            errorLiveData.setValue("Please fill in all fields");
            return;
        }

        if (password.length() < 6) {
            errorLiveData.setValue("Password must be at least 6 characters");
            return;
        }

        loadingLiveData.setValue(true);
        repository.getUser(username, existingUser -> {
            if (existingUser != null) {
                loadingLiveData.postValue(false);
                errorLiveData.postValue("Username already exists");
            } else {
                String hashedPassword = SecurityUtils.hashPassword(password);
                User newUser = new User(username, hashedPassword);
                repository.insertUser(newUser, () -> {
                    loadingLiveData.postValue(false);
                    registerSuccess.postValue(true);
                });
            }
        });
    }
}
