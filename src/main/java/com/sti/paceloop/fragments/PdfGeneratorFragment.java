package com.sti.paceloop.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sti.paceloop.R;
import com.sti.paceloop.viewmodel.PdfViewModel;

public class PdfGeneratorFragment extends Fragment {

    private TextView tvFileName;
    private Button btnUploadPdf, btnGenerate;
    private Spinner spinnerDifficulty;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private Uri selectedPdfUri = null;
    private String selectedFileName = "";
    private PdfViewModel viewModel;

    private String reviewerResult = "";
    private String quizResult = "";
    private String answerResult = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_generator, container, false);
        
        viewModel = new ViewModelProvider(this).get(PdfViewModel.class);

        initViews(view);
        setupListeners();
        setupViewPager();
        setupObservers();

        return view;
    }

    private void initViews(View view) {
        tvFileName = view.findViewById(R.id.tvFileName);
        btnUploadPdf = view.findViewById(R.id.btnUploadPdf);
        btnGenerate = view.findViewById(R.id.btnGenerate);
        spinnerDifficulty = view.findViewById(R.id.spinnerDifficulty);
        progressBar = view.findViewById(R.id.progressBar);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
    }

    private void setupListeners() {
        btnUploadPdf.setOnClickListener(v -> openFilePicker());
        btnGenerate.setOnClickListener(v -> {
            if (selectedPdfUri != null) {
                String difficulty = spinnerDifficulty.getSelectedItem().toString();
                viewModel.generateContent(selectedPdfUri, difficulty, selectedFileName);
            }
        });
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnGenerate.setEnabled(!isLoading);
            btnUploadPdf.setEnabled(!isLoading);
        });

        viewModel.getStudyMaterial().observe(getViewLifecycleOwner(), material -> {
            if (material != null) {
                reviewerResult = material.reviewer;
                quizResult = material.quiz;
                answerResult = material.answers;
                
                if (viewPager.getAdapter() != null) {
                    //viewPager.getAdapter().notifyDataSetChanged();
                    ((ViewPagerAdapter) viewPager.getAdapter()).updateContentIds(reviewerResult, quizResult, answerResult);
                }
                Toast.makeText(getContext(), "Generation Complete!", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "AI Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        filePickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedPdfUri = result.getData().getData();
                    selectedFileName = getFileName(selectedPdfUri);
                    tvFileName.setText(selectedFileName);
                    btnGenerate.setEnabled(true);
                }
            }
    );

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Reviewer"); break;
                case 1: tab.setText("Quiz"); break;
                case 2: tab.setText("Answers"); break;
            }
        }).attach();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private class ViewPagerAdapter extends FragmentStateAdapter {
        
        private long id0 = 0;
        private long id1 = 1;
        private long id2 = 2;

        public ViewPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            String content = "";
            switch (position) {
                case 0: content = reviewerResult; break;
                case 1: content = quizResult; break;
                case 2: content = answerResult; break;
            }
            return ResultTabFragment.newInstance(content);
        }

        @Override
        public int getItemCount() {
            return 3; // We always have 3 tables
        }

        @Override
        public long getItemId(int position) {
            switch (position) {
                case 0: return id0;
                case 1: return id1;
                case 2: return id2;
                default: return 0;
            }
        }

        @Override
        public boolean containsItem(long itemId) {
            return itemId == id0 || itemId == id1 || itemId == id2;
        }

        public void updateContentIds(String reviewer, String quiz, String answers) {
            id0 = 0L + reviewer.hashCode();
            id1 = 1L + quiz.hashCode();
            id2 = 2L + answers.hashCode();
        }
    }
}
