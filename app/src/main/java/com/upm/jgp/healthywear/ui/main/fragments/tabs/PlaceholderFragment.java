package com.upm.jgp.healthywear.ui.main.fragments.tabs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.upm.jgp.healthywear.R;
import com.upm.jgp.healthywear.ui.main.activity.MainActivity;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.ScanMMR2Activity;
import com.upm.jgp.healthywear.ui.main.fragments.mmr.ScanMMRActivity;
import com.upm.jgp.healthywear.ui.main.fragments.smartband.ScanSmartBandActivity;

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
        pageViewModel.setIndex(index);
        /*switch (index){
            case 1:
                pageViewModel.setIndex(index);

                break;

            case 2:
                pageViewModel.setIndex(index);

                break;
            case 3:
                pageViewModel.setIndex(index);

                break;
        }*/

    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root;
        FloatingActionButton mFab;

        switch (getArguments().getInt(ARG_SECTION_NUMBER))
        {
            case 1: {
                if(MainActivity.isSmartbandConnected()) {
                    root = inflater.inflate(R.layout.content_tab_smartband, container, false);
                }else{
                    root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                    simpleTab(root);
                    //Add new SmartBand
                    mFab = root.findViewById(R.id.fab_tabs_section_label);
                    mFab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(view.getContext(), ScanSmartBandActivity.class);
                            startActivity(intent);
                        }
                    });
                }
                break;
            }
            case 2: {
                if(MainActivity.isMmrConnected()) {
                    root = inflater.inflate(R.layout.content_tab_mmr, container, false);
                }else{
                    root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                    simpleTab(root);
                    //Add new MMR
                    mFab = root.findViewById(R.id.fab_tabs_section_label);
                    mFab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(view.getContext(), ScanMMRActivity.class);
                            startActivity(intent);
                        }
                    });
                }
                break;
            }
            case 3: {
                if(MainActivity.isMmr2Connected()) {
                    root = inflater.inflate(R.layout.content_tab_mmr2, container, false);
                }else{
                    root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                    simpleTab(root);
                    //Add new MMR
                    mFab = root.findViewById(R.id.fab_tabs_section_label);
                    mFab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(view.getContext(), ScanMMR2Activity.class);
                            startActivity(intent);
                        }
                    });
                }
                break;
            }

            case 4: {
                root = inflater.inflate(R.layout.fragment_textview_tabs, container, false);
                simpleTab(root);
                //TODO add other devices
                mFab = root.findViewById(R.id.fab_tabs_section_label);
                mFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view, "Add other devices... (TBD)", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                });
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