package com.sti.paceloop.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.sti.paceloop.R;
import com.sti.paceloop.activities.LoginActivity;
import com.sti.paceloop.database.AppDatabase;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvUsername = view.findViewById(R.id.tvProfileUsername);
        TextView tvTotalFocus = view.findViewById(R.id.tvTotalFocus);
        TextView tvAvgBurnout = view.findViewById(R.id.tvAvgBurnout);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        SharedPreferences prefs = getActivity().getSharedPreferences("PaceLoop", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        int userId = prefs.getInt("userId", -1);

        tvUsername.setText(username);

        new Thread(() -> {
            int totalFocus = AppDatabase.getInstance(getContext()).appDao().getTotalFocusTime(userId);
            double avgBurnout = AppDatabase.getInstance(getContext()).appDao().getAverageBurnoutScore();
            
            getActivity().runOnUiThread(() -> {
                tvTotalFocus.setText(totalFocus + "m");
                tvAvgBurnout.setText(String.format("%.1f", avgBurnout));
            });
        }).start();

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }
}