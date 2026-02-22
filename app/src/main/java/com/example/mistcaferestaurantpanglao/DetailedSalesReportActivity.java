package com.example.mistcaferestaurantpanglao;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailedSalesReportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvReportTitle, tvReportPeriod, tvTotalSales, tvTotalOrders, tvAverageSale;
    private TextView tvHighestSale, tvLowestSale, tvNoOrders;
    private RecyclerView recyclerViewOrders;
    private ProgressBar progressBar;
    private FloatingActionButton fabDownloadPdf;

    private FirebaseFirestore db;
    private SalesOrderAdapter ordersAdapter;
    private List<Order> ordersList;

    private String reportType;
    private Date startDate;
    private Date endDate;

    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Analytics data
    private double totalSalesAmount = 0.0;
    private int totalOrdersCount = 0;
    private double averageOrderValue = 0.0;
    private double highestSaleAmount = 0.0;
    private double lowestSaleAmount = 0.0;
    private Map<String, Integer> itemSalesCount = new HashMap<>();
    private Map<Integer, Integer> hourlyOrderCount = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_sales_report);

        reportType = getIntent().getStringExtra("REPORT_TYPE");
        long startTimestamp = getIntent().getLongExtra("START_DATE", 0);
        long endTimestamp = getIntent().getLongExtra("END_DATE", 0);

        if (reportType == null || startTimestamp == 0 || endTimestamp == 0) {
            Toast.makeText(this, "Invalid report parameters", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startDate = new Date(startTimestamp);
        endDate = new Date(endTimestamp);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupFabButton();

        db = FirebaseFirestore.getInstance();
        loadSalesReport();
    }

    private void initializeViews() {
        tvReportTitle = findViewById(R.id.tvReportTitle);
        tvReportPeriod = findViewById(R.id.tvReportPeriod);
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvAverageSale = findViewById(R.id.tvAverageSale);
        tvHighestSale = findViewById(R.id.tvHighestSale);
        tvLowestSale = findViewById(R.id.tvLowestSale);
        tvNoOrders = findViewById(R.id.tvNoOrders);
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        progressBar = findViewById(R.id.progressBar);
        fabDownloadPdf = findViewById(R.id.fabDownloadPdf);

        String title = getReportTitle();
        tvReportTitle.setText(title);

        String period = displayDateFormat.format(startDate) + " - " + displayDateFormat.format(endDate);
        tvReportPeriod.setText(period);
    }

    private String getReportTitle() {
        if (reportType == null) return "Sales Report";

        switch (reportType.toLowerCase()) {
            case "daily":
                return "Daily Sales Report";
            case "weekly":
                return "Weekly Sales Report";
            case "monthly":
                return "Monthly Sales Report";
            default:
                return "Sales Report";
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sales Report");
        }
    }

    private void setupRecyclerView() {
        ordersList = new ArrayList<>();
        ordersAdapter = new SalesOrderAdapter(this, ordersList);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(ordersAdapter);
    }

    private void setupFabButton() {
        fabDownloadPdf.setOnClickListener(v -> {
            if (ordersList.isEmpty()) {
                Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }

            if (checkPermissions()) {
                generatePdfReport();
            } else {
                requestPermissions();
            }
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        } else {
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generatePdfReport();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdfReport();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generatePdfReport() {
        try {
            PdfDocument document = new PdfDocument();

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            drawReportTable(canvas);
            document.finishPage(page);

            String fileName = "SalesReport_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            document.writeTo(new FileOutputStream(file));
            document.close();

            Toast.makeText(this, "PDF saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void drawReportTable(Canvas canvas) {
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();
        Paint labelPaint = new Paint();
        Paint valuePaint = new Paint();
        Paint linePaint = new Paint();

        int black = 0xFF000000;
        int darkGray = 0xFF424242;
        int lightGray = 0xFFBDBDBD;

        titlePaint.setTextSize(28);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(black);
        canvas.drawText("MIST CAFE & RESTAURANT", 50, 60, titlePaint);

        headerPaint.setTextSize(16);
        headerPaint.setColor(darkGray);
        canvas.drawText("PANGLAO", 50, 85, headerPaint);

        headerPaint.setTextSize(20);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(black);
        canvas.drawText(getReportTitle(), 50, 120, headerPaint);

        paint.setTextSize(11);
        paint.setColor(darkGray);
        canvas.drawText("Report Period: " + displayDateFormat.format(startDate) + " - " +
                displayDateFormat.format(endDate), 50, 145, paint);

        canvas.drawText("Generated: " + new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(new Date()),
                50, 162, paint);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        linePaint.setColor(black);
        canvas.drawRect(40, 180, 555, 750, linePaint);

        int tableX = 50;
        int tableY = 200;
        int colWidth = 245;
        int rowHeight = 35;

        labelPaint.setTextSize(12);
        labelPaint.setColor(black);

        valuePaint.setTextSize(14);
        valuePaint.setFakeBoldText(true);
        valuePaint.setColor(black);

        linePaint.setStrokeWidth(1);
        linePaint.setColor(lightGray);

        headerPaint.setTextSize(13);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(black);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(black);
        canvas.drawRect(tableX, tableY - 5, tableX + colWidth * 2, tableY + 20, paint);
        canvas.drawText("FINANCIAL SUMMARY", tableX + 10, tableY + 12, headerPaint);

        tableY += 30;

        // Row 1: Total Sales
        canvas.drawText("Total Sales", tableX + 10, tableY + 20, labelPaint);
        canvas.drawText("₱ " + String.format(Locale.getDefault(), "%.2f", totalSalesAmount),
                tableX + colWidth + 10, tableY + 20, valuePaint);
        canvas.drawLine(tableX, tableY + rowHeight, tableX + colWidth * 2, tableY + rowHeight, linePaint);
        tableY += rowHeight;

        canvas.drawText("Total Orders", tableX + 10, tableY + 20, labelPaint);
        canvas.drawText(String.valueOf(totalOrdersCount), tableX + colWidth + 10, tableY + 20, valuePaint);
        canvas.drawLine(tableX, tableY + rowHeight, tableX + colWidth * 2, tableY + rowHeight, linePaint);
        tableY += rowHeight;

        canvas.drawText("Average Order Value", tableX + 10, tableY + 20, labelPaint);
        canvas.drawText("₱ " + String.format(Locale.getDefault(), "%.2f", averageOrderValue),
                tableX + colWidth + 10, tableY + 20, valuePaint);
        canvas.drawLine(tableX, tableY + rowHeight, tableX + colWidth * 2, tableY + rowHeight, linePaint);
        tableY += rowHeight;

        canvas.drawText("Highest Sale", tableX + 10, tableY + 20, labelPaint);
        canvas.drawText("₱ " + String.format(Locale.getDefault(), "%.2f", highestSaleAmount),
                tableX + colWidth + 10, tableY + 20, valuePaint);
        canvas.drawLine(tableX, tableY + rowHeight, tableX + colWidth * 2, tableY + rowHeight, linePaint);
        tableY += rowHeight;

        canvas.drawText("Lowest Sale", tableX + 10, tableY + 20, labelPaint);
        canvas.drawText("₱ " + String.format(Locale.getDefault(), "%.2f", lowestSaleAmount),
                tableX + colWidth + 10, tableY + 20, valuePaint);
        canvas.drawLine(tableX, tableY + rowHeight, tableX + colWidth * 2, tableY + rowHeight, linePaint);
        tableY += rowHeight + 15;

        canvas.drawRect(tableX, tableY - 5, tableX + colWidth * 2, tableY + 20, paint);
        canvas.drawText("SALES BY TIME PERIOD", tableX + 10, tableY + 12, headerPaint);

        tableY += 30;

        String[] timeAnalysis = analyzePeakHours();

        canvas.drawText("Peak Hours", tableX + 10, tableY + 20, labelPaint);
        valuePaint.setTextSize(12);
        canvas.drawText(timeAnalysis[0], tableX + colWidth + 10, tableY + 20, valuePaint);
        canvas.drawLine(tableX, tableY + rowHeight, tableX + colWidth * 2, tableY + rowHeight, linePaint);
        tableY += rowHeight;

        canvas.drawText("Slow Hours", tableX + 10, tableY + 20, labelPaint);
        canvas.drawText(timeAnalysis[1], tableX + colWidth + 10, tableY + 20, valuePaint);
        canvas.drawLine(tableX, tableY + rowHeight, tableX + colWidth * 2, tableY + rowHeight, linePaint);
        tableY += rowHeight + 15;

        canvas.drawRect(tableX, tableY - 5, tableX + colWidth * 2, tableY + 20, paint);
        canvas.drawText("PRODUCT PERFORMANCE", tableX + 10, tableY + 12, headerPaint);

        tableY += 30;

        String[] topItems = getTopSellingItems(3);

        canvas.drawText("Top Selling Items", tableX + 10, tableY + 15, labelPaint);
        tableY += 10;

        valuePaint.setTextSize(11);
        for (int i = 0; i < topItems.length && topItems[i] != null; i++) {
            canvas.drawText("• " + topItems[i], tableX + colWidth + 10, tableY + 15, valuePaint);
            tableY += 18;
        }

        canvas.drawLine(tableX, tableY + 10, tableX + colWidth * 2, tableY + 10, linePaint);
        tableY += 20;

        String[] bottomItems = getLowestSellingItems(3);

        canvas.drawText("Lowest Selling Items", tableX + 10, tableY + 15, labelPaint);
        tableY += 10;

        for (int i = 0; i < bottomItems.length && bottomItems[i] != null; i++) {
            canvas.drawText("• " + bottomItems[i], tableX + colWidth + 10, tableY + 15, valuePaint);
            tableY += 18;
        }

        paint.setTextSize(9);
        paint.setColor(darkGray);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText("This report is computer-generated and is valid without signature.", 50, 800, paint);
        canvas.drawText("Mist Cafe Restaurant • Danao, Panglao, Bohol", 50, 815, paint);
    }

    private String[] analyzePeakHours() {
        int maxOrders = 0;
        int minOrders = Integer.MAX_VALUE;
        List<Integer> peakHours = new ArrayList<>();
        List<Integer> slowHours = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : hourlyOrderCount.entrySet()) {
            if (entry.getValue() > maxOrders) {
                maxOrders = entry.getValue();
            }
            if (entry.getValue() < minOrders && entry.getValue() > 0) {
                minOrders = entry.getValue();
            }
        }

        for (Map.Entry<Integer, Integer> entry : hourlyOrderCount.entrySet()) {
            if (entry.getValue() == maxOrders) {
                peakHours.add(entry.getKey());
            }
            if (entry.getValue() == minOrders) {
                slowHours.add(entry.getKey());
            }
        }

        String peakTime = formatHours(peakHours) + " (" + maxOrders + " orders)";
        String slowTime = formatHours(slowHours) + " (" + minOrders + " orders)";

        return new String[]{peakTime, slowTime};
    }

    private String formatHours(List<Integer> hours) {
        if (hours.isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(hours.size(), 3); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%02d:00", hours.get(i)));
        }
        return sb.toString();
    }

    private String[] getTopSellingItems(int count) {
        String[] topItems = new String[count];
        List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(itemSalesCount.entrySet());
        sortedItems.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (int i = 0; i < Math.min(count, sortedItems.size()); i++) {
            Map.Entry<String, Integer> entry = sortedItems.get(i);
            topItems[i] = entry.getKey() + " (" + entry.getValue() + " sold)";
        }
        return topItems;
    }

    private String[] getLowestSellingItems(int count) {
        String[] bottomItems = new String[count];
        List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(itemSalesCount.entrySet());
        sortedItems.sort((a, b) -> a.getValue().compareTo(b.getValue()));

        for (int i = 0; i < Math.min(count, sortedItems.size()); i++) {
            Map.Entry<String, Integer> entry = sortedItems.get(i);
            bottomItems[i] = entry.getKey() + " (" + entry.getValue() + " sold)";
        }
        return bottomItems;
    }

    private void loadSalesReport() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoOrders.setVisibility(View.GONE);
        recyclerViewOrders.setVisibility(View.GONE);

        db.collection("archivedOrders")
                .limit(1)
                .get()
                .addOnSuccessListener(testSnapshot -> {
                    if (testSnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        showNoOrdersMessage();
                        return;
                    }

                    db.collection("archivedOrders")
                            .whereEqualTo("status", "paid")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                progressBar.setVisibility(View.GONE);

                                if (querySnapshot.isEmpty()) {
                                    showNoOrdersMessage();
                                    return;
                                }

                                filterAndProcessOrders(querySnapshot);
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                showNoOrdersMessage();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showNoOrdersMessage();
                });
    }

    private void filterAndProcessOrders(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        List<DocumentSnapshot> filteredDocs = new ArrayList<>();
        long startMs = startDate.getTime();
        long endMs = endDate.getTime();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            long orderTimestamp = getTimestampFromDocument(doc);

            if (orderTimestamp == 0) {
                continue;
            }

            if (orderTimestamp >= startMs && orderTimestamp <= endMs) {
                filteredDocs.add(doc);
            }
        }

        if (filteredDocs.isEmpty()) {
            showNoOrdersMessage();
            return;
        }

        processOrdersData(filteredDocs);
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

    private void processOrdersData(List<DocumentSnapshot> documents) {
        ordersList.clear();
        itemSalesCount.clear();
        hourlyOrderCount.clear();

        totalSalesAmount = 0.0;
        highestSaleAmount = 0.0;
        lowestSaleAmount = Double.MAX_VALUE;

        for (DocumentSnapshot doc : documents) {
            try {
                Order order = Order.fromFirestore(doc);

                if (order != null) {
                    double amount = order.getTotalAmount();

                    totalSalesAmount += amount;
                    if (amount > highestSaleAmount) {
                        highestSaleAmount = amount;
                    }
                    if (amount < lowestSaleAmount && amount > 0) {
                        lowestSaleAmount = amount;
                    }

                    ordersList.add(order);

                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(order.getTimestamp());
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    hourlyOrderCount.put(hour, hourlyOrderCount.getOrDefault(hour, 0) + 1);

                    List<OrderItem> items = order.getItems();
                    if (items != null) {
                        for (OrderItem item : items) {
                            String itemName = item.getItemName();
                            int quantity = item.getQuantity();
                            itemSalesCount.put(itemName, itemSalesCount.getOrDefault(itemName, 0) + quantity);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        if (ordersList.isEmpty()) {
            showNoOrdersMessage();
            return;
        }

        totalOrdersCount = ordersList.size();
        averageOrderValue = totalSalesAmount / totalOrdersCount;

        updateStatistics(totalSalesAmount, totalOrdersCount, averageOrderValue, highestSaleAmount,
                lowestSaleAmount == Double.MAX_VALUE ? 0.0 : lowestSaleAmount);

        recyclerViewOrders.setVisibility(View.VISIBLE);
        ordersAdapter.updateOrders(ordersList);
    }

    private void updateStatistics(double totalSales, int orderCount, double averageSale,
                                  double highestSale, double lowestSale) {
        tvTotalSales.setText("₱ " + String.format(Locale.getDefault(), "%.2f", totalSales));
        tvTotalOrders.setText(String.valueOf(orderCount));
        tvAverageSale.setText("₱ " + String.format(Locale.getDefault(), "%.2f", averageSale));
        tvHighestSale.setText("₱ " + String.format(Locale.getDefault(), "%.2f", highestSale));
        tvLowestSale.setText("₱ " + String.format(Locale.getDefault(), "%.2f", lowestSale));
    }

    private void showNoOrdersMessage() {
        tvNoOrders.setVisibility(View.VISIBLE);
        recyclerViewOrders.setVisibility(View.GONE);
        updateStatistics(0.0, 0, 0.0, 0.0, 0.0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}