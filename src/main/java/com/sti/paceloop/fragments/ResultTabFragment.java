package com.sti.paceloop.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.sti.paceloop.R;

public class ResultTabFragment extends Fragment {

    private static final String ARG_CONTENT = "content";

    public static ResultTabFragment newInstance(String content) {
        ResultTabFragment fragment = new ResultTabFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTENT, content);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result_tab, container, false);
        TextView tvContent = view.findViewById(R.id.tvContent);
        
        if (getArguments() != null) {
            String content = getArguments().getString(ARG_CONTENT);
            if (content != null && !content.isEmpty()) {
                tvContent.setText(content);
            }
        }
        return view;
    }
}