package com.dianerverotect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Example: Set text in the fragment
        TextView textView = view.findViewById(R.id.text_history_placeholder); // Assuming you have a TextView with this ID in your layout
        if (textView != null) {
            textView.setText("History Fragment Content");
        }

        return view;
    }
}
