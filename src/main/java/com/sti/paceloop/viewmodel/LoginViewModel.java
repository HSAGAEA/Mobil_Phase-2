package com.sti.paceloop.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.sti.paceloop.database.User;
import com.sti.paceloop.repository.AppRepository;
import com.sti.paceloop.utils.SecurityUtils;

public class LoginViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            errorLiveData.setValue("Please fill in all fields");
            return;
        }

        loadingLiveData.setValue(true);
        String hashedInput = SecurityUtils.hashPassword(password);

        repository.getUser(username, user -> {
            loadingLiveData.postValue(false);
            if (user != null && user.passwordHash.equals(hashedInput)) {
                userLiveData.postValue(user);
            } else {
                errorLiveData.postValue("Invalid username or password");
            }
        });
    }
}
