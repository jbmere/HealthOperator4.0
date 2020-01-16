package com.upm.jgp.healthywear.ui.main.fragments.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.activity.MainActivity;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        switch (index){
            case 1:
                pageViewModel.setIndex(index);
                break;

            case 2:
                pageViewModel.setIndex(index);

                break;
            case 3:
                pageViewModel.setIndex(index);

                break;
        }

    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root;
        switch (getArguments().getInt(ARG_SECTION_NUMBER))
        {
            case 1: {
                if(MainActivity.isSmartbandConnected()) {
                    root = inflater.inflate(R.layout.fragment_tab_smartband, container, false);
                }else{
                    root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                    simpleTab(root);
                }
                break;
            }
            case 2: {
                if(MainActivity.isMmrConnected()) {
                    root = inflater.inflate(R.layout.content_tab_mmr, container, false);
                }else{
                    root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                    simpleTab(root);
                }
                break;
            }

            case 3: {
                root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                simpleTab(root);
                break;
            }
            default:
                root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                simpleTab(root);
                break;
        }

        return root;
    }

    private void simpleTab(View root){
        final TextView textView = root.findViewById(R.id.textview_tabs_section_label);
        pageViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                textView.setText(s);
            }
        });
    }
}