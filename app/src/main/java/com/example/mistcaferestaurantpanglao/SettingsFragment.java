package com.example.mistcaferestaurantpanglao;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        LinearLayout llManageMenu = view.findViewById(R.id.llManageMenu);
        LinearLayout llManagePeople = view.findViewById(R.id.llManagePeople);
        LinearLayout llOrderHistory = view.findViewById(R.id.llOrderHistory);
        LinearLayout llEditMenu = view.findViewById(R.id.llEditMenu);
        LinearLayout llGenerateQR = view.findViewById(R.id.llGenerateQR);
        LinearLayout llHelp = view.findViewById(R.id.llHelp);
        LinearLayout llTermsOfService = view.findViewById(R.id.llTermsOfService);
        LinearLayout llAbout = view.findViewById(R.id.llAbout);
        LinearLayout llRatingStatistics = view.findViewById(R.id.llRatingStatistics);

        llManageMenu.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ManageMenu.class));
        });

        llEditMenu.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditMenuActivity.class));
        });

        llManagePeople.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ManagePeople.class));
        });

        llGenerateQR.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), GenerateQrActivity.class));
        });

        llOrderHistory.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), OrderHistory.class));
        });

        llHelp.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GenericContentActivity.class);
            intent.putExtra(GenericContentActivity.EXTRA_LAYOUT_ID, R.layout.activity_help_support);
            intent.putExtra(GenericContentActivity.EXTRA_TITLE, "Help & Support");
            startActivity(intent);
        });

        llTermsOfService.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GenericContentActivity.class);
            intent.putExtra(GenericContentActivity.EXTRA_LAYOUT_ID, R.layout.activity_terms_of_service);
            intent.putExtra(GenericContentActivity.EXTRA_TITLE, "Terms of Service");
            startActivity(intent);
        });

        llAbout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GenericContentActivity.class);
            intent.putExtra(GenericContentActivity.EXTRA_LAYOUT_ID, R.layout.activity_about);
            intent.putExtra(GenericContentActivity.EXTRA_TITLE, "About");
            startActivity(intent);
        });

        llRatingStatistics.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), RatingStatisticsActivity.class));
        });

        return view;
    }
}