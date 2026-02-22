package com.example.mistcaferestaurantpanglao;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private CardView startDateCard, endDateCard, reportPreviewCard;
    private TextView tvStartDate, tvStartDateSubtext, tvEndDate, tvEndDateSubtext;
    private Button btnThisWeek, btnThisMonth, btnThisYear;
    private Button btnLastWeek, btnLastMonth, btnCustom;
    private Button btnGenerateReport, btnViewDetailedReport;
    private TextView tvReportPeriod, tvReportTotalSales, tvReportOrderCount;

    private FirebaseFirestore db;
    private ListenerRegistration ordersListener;

    private Calendar startDate = null;
    private Calendar endDate = null;
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        startDateCard = view.findViewById(R.id.startDateCard);
        endDateCard = view.findViewById(R.id.endDateCard);
        reportPreviewCard = view.findViewById(R.id.reportPreviewCard);

        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvStartDateSubtext = view.findViewById(R.id.tvStartDateSubtext);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        tvEndDateSubtext = view.findViewById(R.id.tvEndDateSubtext);

        btnThisWeek = view.findViewById(R.id.btnThisWeek);
        btnThisMonth = view.findViewById(R.id.btnThisMonth);
        btnThisYear = view.findViewById(R.id.btnThisYear);
        btnLastWeek = view.findViewById(R.id.btnLastWeek);
        btnLastMonth = view.findViewById(R.id.btnLastMonth);
        btnCustom = view.findViewById(R.id.btnCustom);

        btnGenerateReport = view.findViewById(R.id.btnGenerateReport);
        btnViewDetailedReport = view.findViewById(R.id.btnViewDetailedReport);

        tvReportPeriod = view.findViewById(R.id.tvReportPeriod);
        tvReportTotalSales = view.findViewById(R.id.tvReportTotalSales);
        tvReportOrderCount = view.findViewById(R.id.tvReportOrderCount);

        db = FirebaseFirestore.getInstance();

        setupDateSelectionListeners();
        setupQuickSelectButtons();
        setupGenerateReportButton();

        return view;
    }

    private void setupDateSelectionListeners() {
        startDateCard.setOnClickListener(v -> showStartDatePicker());
        endDateCard.setOnClickListener(v -> showEndDatePicker());
    }

    private void showStartDatePicker() {
        Calendar cal = startDate != null ? startDate : Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    startDate = Calendar.getInstance();
                    startDate.set(year, month, dayOfMonth, 0, 0, 0);
                    startDate.set(Calendar.MILLISECOND, 0);
                    updateStartDateDisplay();
                    validateDateRange();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void showEndDatePicker() {
        Calendar cal = endDate != null ? endDate : Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    endDate = Calendar.getInstance();
                    endDate.set(year, month, dayOfMonth, 23, 59, 59);
                    endDate.set(Calendar.MILLISECOND, 999);
                    updateEndDateDisplay();
                    validateDateRange();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        if (startDate != null) {
            datePickerDialog.getDatePicker().setMinDate(startDate.getTimeInMillis());
        }

        datePickerDialog.show();
    }

    private void updateStartDateDisplay() {
        if (startDate != null) {
            tvStartDate.setText(displayDateFormat.format(startDate.getTime()));
            tvStartDateSubtext.setText("Start date selected");
        }
    }

    private void updateEndDateDisplay() {
        if (endDate != null) {
            tvEndDate.setText(displayDateFormat.format(endDate.getTime()));
            tvEndDateSubtext.setText("End date selected");
        }
    }

    private void validateDateRange() {
        if (startDate != null && endDate != null) {
            if (endDate.before(startDate)) {
                endDate = null;
                tvEndDate.setText("Select End Date");
                tvEndDateSubtext.setText("Must be after start date");
                btnGenerateReport.setEnabled(false);
            } else {
                btnGenerateReport.setEnabled(true);
                reportPreviewCard.setVisibility(View.GONE);
            }
        } else {
            btnGenerateReport.setEnabled(false);
        }
    }

    private void setupQuickSelectButtons() {
        btnThisWeek.setOnClickListener(v -> selectThisWeek());
        btnThisMonth.setOnClickListener(v -> selectThisMonth());
        btnThisYear.setOnClickListener(v -> selectThisYear());
        btnLastWeek.setOnClickListener(v -> selectLastWeek());
        btnLastMonth.setOnClickListener(v -> selectLastMonth());
        btnCustom.setOnClickListener(v -> clearDateSelection());
    }

    private void selectThisWeek() {
        Calendar now = Calendar.getInstance();
        startDate = Calendar.getInstance();
        startDate.setTime(getWeekStartDate(now));

        endDate = Calendar.getInstance();
        endDate.setTime(getWeekEndDate(now));

        updateStartDateDisplay();
        updateEndDateDisplay();
        validateDateRange();
    }

    private void selectThisMonth() {
        startDate = Calendar.getInstance();
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);

        endDate = Calendar.getInstance();
        endDate.set(Calendar.DAY_OF_MONTH, endDate.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 999);

        updateStartDateDisplay();
        updateEndDateDisplay();
        validateDateRange();
    }

    private void selectThisYear() {
        startDate = Calendar.getInstance();
        startDate.set(Calendar.MONTH, Calendar.JANUARY);
        startDate.set(Calendar.DAY_OF_MONTH, 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);

        endDate = Calendar.getInstance();
        endDate.set(Calendar.MONTH, Calendar.DECEMBER);
        endDate.set(Calendar.DAY_OF_MONTH, 31);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 999);

        updateStartDateDisplay();
        updateEndDateDisplay();
        validateDateRange();
    }

    private void selectLastWeek() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.WEEK_OF_YEAR, -1);

        startDate = Calendar.getInstance();
        startDate.setTime(getWeekStartDate(now));

        endDate = Calendar.getInstance();
        endDate.setTime(getWeekEndDate(now));

        updateStartDateDisplay();
        updateEndDateDisplay();
        validateDateRange();
    }

    private void selectLastMonth() {
        Calendar lastMonth = Calendar.getInstance();
        lastMonth.add(Calendar.MONTH, -1);

        startDate = Calendar.getInstance();
        startDate.set(lastMonth.get(Calendar.YEAR), lastMonth.get(Calendar.MONTH), 1);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);

        endDate = Calendar.getInstance();
        endDate.set(lastMonth.get(Calendar.YEAR), lastMonth.get(Calendar.MONTH),
                lastMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 999);

        updateStartDateDisplay();
        updateEndDateDisplay();
        validateDateRange();
    }

    private void clearDateSelection() {
        startDate = null;
        endDate = null;

        tvStartDate.setText("Select Start Date");
        tvStartDateSubtext.setText("Tap to choose");
        tvEndDate.setText("Select End Date");
        tvEndDateSubtext.setText("Tap to choose");

        btnGenerateReport.setEnabled(false);
        reportPreviewCard.setVisibility(View.GONE);
    }

    private void setupGenerateReportButton() {
        btnGenerateReport.setOnClickListener(v -> generateReport());
        btnViewDetailedReport.setOnClickListener(v -> viewDetailedReport());
    }

    private void generateReport() {
        if (startDate == null || endDate == null) {
            return;
        }

        btnGenerateReport.setEnabled(false);
        btnGenerateReport.setText("⏳ Generating...");

        db.collection("archivedOrders")
                .limit(1)
                .get()
                .addOnSuccessListener(testSnapshot -> {
                    if (testSnapshot.isEmpty()) {
                        showMessage("No paid orders found. Complete some orders first.", true);
                        return;
                    }

                    db.collection("archivedOrders")
                            .whereEqualTo("status", "paid")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot.isEmpty()) {
                                    showMessage("No paid orders in database.", true);
                                    return;
                                }

                                filterAndDisplayOrders(querySnapshot);
                            })
                            .addOnFailureListener(e -> {
                                showMessage("Error: " + e.getMessage(), true);
                            });
                })
                .addOnFailureListener(e -> {
                    showMessage("Database error: " + e.getMessage(), true);
                });
    }

    private void filterAndDisplayOrders(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        double totalSales = 0.0;
        int orderCount = 0;
        long startMs = startDate.getTimeInMillis();
        long endMs = endDate.getTimeInMillis();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            long orderTimestamp = getTimestampFromDocument(doc);

            if (orderTimestamp == 0) {
                continue;
            }

            if (orderTimestamp >= startMs && orderTimestamp <= endMs) {
                Object amountObj = doc.get("totalAmount");
                if (amountObj instanceof Number) {
                    double amount = ((Number) amountObj).doubleValue();
                    totalSales += amount;
                    orderCount++;
                }
            }
        }

        displayReportSummary(totalSales, orderCount);
    }

    private long getTimestampFromDocument(DocumentSnapshot doc) {
        Object timestampObj = doc.get("timestamp");

        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        } else if (timestampObj instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
        } else if (timestampObj instanceof Date) {
            return ((Date) timestampObj).getTime();
        }

        return 0;
    }

    private void displayReportSummary(double totalSales, int orderCount) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            btnGenerateReport.setEnabled(true);
            btnGenerateReport.setText("📊 Generate Sales Report");

            String periodText = displayDateFormat.format(startDate.getTime()) +
                    " - " + displayDateFormat.format(endDate.getTime());
            tvReportPeriod.setText(periodText);
            tvReportTotalSales.setText("₱ " + String.format(Locale.getDefault(), "%.2f", totalSales));
            tvReportOrderCount.setText(orderCount + " order" + (orderCount != 1 ? "s" : ""));

            reportPreviewCard.setVisibility(View.VISIBLE);

            String message = orderCount > 0
                    ? "Found " + orderCount + " paid orders"
                    : "No paid orders in selected period";
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void showMessage(String message, boolean resetButton) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (resetButton) {
                btnGenerateReport.setEnabled(true);
                btnGenerateReport.setText("📊 Generate Sales Report");
            }
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void viewDetailedReport() {
        if (startDate == null || endDate == null) {
            return;
        }

        Intent intent = new Intent(getActivity(), DetailedSalesReportActivity.class);
        intent.putExtra("REPORT_TYPE", "custom");
        intent.putExtra("START_DATE", startDate.getTimeInMillis());
        intent.putExtra("END_DATE", endDate.getTimeInMillis());

        startActivity(intent);
    }


    private Date getWeekStartDate(Calendar date) {
        Calendar cal = (Calendar) date.clone();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getWeekEndDate(Calendar date) {
        Calendar cal = (Calendar) date.clone();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_WEEK, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }
}