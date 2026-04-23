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
import androidx.lifecycle.ViewModelProvider;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.sti.paceloop.R;
import com.sti.paceloop.database.MoodEntry;
import com.sti.paceloop.viewmodel.AnalyticsViewModel;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private LineChart burnoutChart;
    private PieChart focusPieChart;
    private TextView tvProductivityScore;
    private AnalyticsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        burnoutChart = view.findViewById(R.id.burnoutChart);
        focusPieChart = view.findViewById(R.id.focusPieChart);
        tvProductivityScore = view.findViewById(R.id.tvProductivityScore);

        setupObservers();

        return view;
    }

    private void setupObservers() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("PaceLoop", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        viewModel.getRecentMoods().observe(getViewLifecycleOwner(), this::setupLineChart);
        
        viewModel.getTotalFocusTime(userId).observe(getViewLifecycleOwner(), focusTime -> {
            int time = (focusTime != null) ? focusTime : 0;
            setupPieChart(time);
            calculateProductivity(time);
        });
    }

    private void setupLineChart(List<MoodEntry> moods) {
        if (moods == null || moods.isEmpty()) return;
        
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < moods.size(); i++) {
            entries.add(new Entry(i, (float) moods.get(i).burnoutScore));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Burnout Score");
        dataSet.setColor(Color.parseColor("#003E7E"));
        dataSet.setCircleColor(Color.parseColor("#FBE122"));
        dataSet.setLineWidth(2f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#003E7E"));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        burnoutChart.setData(lineData);
        burnoutChart.getDescription().setEnabled(false);
        burnoutChart.getXAxis().setDrawGridLines(false);
        burnoutChart.animateX(1000);
        burnoutChart.invalidate();
    }

    private void setupPieChart(int focusMinutes) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(focusMinutes, "Focus"));
        entries.add(new PieEntry(30, "Rest")); // Static for now

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#003E7E"), Color.parseColor("#FBE122")});
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        
        PieData pieData = new PieData(dataSet);
        focusPieChart.setData(pieData);
        focusPieChart.getDescription().setEnabled(false);
        focusPieChart.setCenterText("Focus Ratio");
        focusPieChart.setHoleRadius(60f);
        focusPieChart.animateY(1000);
        focusPieChart.invalidate();
    }

    private void calculateProductivity(int focusMinutes) {
        int totalActive = focusMinutes + 30;
        if (totalActive == 0) {
            tvProductivityScore.setText("0%");
            return;
        }
        int score = (focusMinutes * 100) / totalActive;
        tvProductivityScore.setText(score + "%");
    }
}
