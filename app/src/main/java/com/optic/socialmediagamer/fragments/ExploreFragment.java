package com.optic.socialmediagamer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.optic.socialmediagamer.R;

public class ExploreFragment extends Fragment {

    private TabLayout mTabLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Explorar");
            }
        }

        mTabLayout = view.findViewById(R.id.tabLayoutExplore);

        mTabLayout.addTab(mTabLayout.newTab().setText("Categorías"));
        mTabLayout.addTab(mTabLayout.newTab().setText("🔥 Trending"));
        mTabLayout.addTab(mTabLayout.newTab().setText("👥 LFG"));
        mTabLayout.addTab(mTabLayout.newTab().setText("🏆 Eventos"));
        mTabLayout.addTab(mTabLayout.newTab().setText("🌐 Comunidades"));
        mTabLayout.addTab(mTabLayout.newTab().setText("🏅 Ranking"));

        loadFragment(new FiltersFragment());

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment fragment;
                switch (tab.getPosition()) {
                    case 1: fragment = new TrendingFragment(); break;
                    case 2: fragment = new LFGFragment(); break;
                    case 3: fragment = new EventsFragment(); break;
                    case 4: fragment = new CommunitiesFragment(); break;
                    case 5: fragment = new LeaderboardFragment(); break;
                    default: fragment = new FiltersFragment(); break;
                }
                loadFragment(fragment);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        return view;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.containerExplore, fragment);
        transaction.commit();
    }
}
