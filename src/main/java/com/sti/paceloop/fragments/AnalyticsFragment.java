package com.sti.paceloop.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.sti.paceloop.R;
import com.sti.paceloop.database.AppDatabase;
import com.sti.paceloop.database.MoodEntry;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private LineChart burnoutChart;
    private PieChart focusPieChart;
    private TextView tvProductivityScore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        burnoutChart = view.findViewById(R.id.burnoutChart);
        focusPieChart = view.findViewById(R.id.focusPieChart);
        tvProductivityScore = view.findViewById(R.id.tvProductivityScore);

        loadData();

        return view;
    }

    private void loadData() {
        SharedPreferences prefs = getActivity().getSharedPreferences("PaceLoop", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        new Thread(() -> {
            List<MoodEntry> moods = AppDatabase.getInstance(getContext()).appDao().getRecentMoods(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000));
            int totalFocusTime = AppDatabase.getInstance(getContext()).appDao().getTotalFocusTime(userId);
            
            getActivity().runOnUiThread(() -> {
                setupLineChart(moods);
                setupPieChart(totalFocusTime);
                calculateProductivity(totalFocusTime);
            });
        }).start();
    }

    private void setupLineChart(List<MoodEntry> moods) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < moods.size(); i++) {
            entries.add(new Entry(i, (float) moods.get(i).burnoutScore));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Burnout Score");
        dataSet.setColor(Color.parseColor("#003E7E"));
        dataSet.setCircleColor(Color.parseColor("#FBE122"));
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        burnoutChart.setData(lineData);
        burnoutChart.getDescription().setEnabled(false);
        burnoutChart.invalidate();
    }

    private void setupPieChart(int focusMinutes) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(focusMinutes, "Focus"));
        entries.add(new PieEntry(30, "Break")); // Placeholder

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#003E7E"), Color.parseColor("#FBE122")});
        
        PieData pieData = new PieData(dataSet);
        focusPieChart.setData(pieData);
        focusPieChart.getDescription().setEnabled(false);
        focusPieChart.setCenterText("Focus Ratio");
        focusPieChart.invalidate();
    }

    private void calculateProductivity(int focusMinutes) {
        // Simple formula: Focus Time / (Focus + Break) * 100
        int totalActive = focusMinutes + 30; // 30 is placeholder for breaks
        if (totalActive == 0) return;
        int score = (focusMinutes * 100) / totalActive;
        tvProductivityScore.setText(score + "%");
    }
}