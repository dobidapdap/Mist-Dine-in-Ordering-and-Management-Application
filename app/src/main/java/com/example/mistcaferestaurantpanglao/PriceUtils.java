package com.example.mistcaferestaurantpanglao;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Locale;

public class PriceUtils {

    /**
     * @param document
     * @param fieldName
     * @return
     */
    public static String getPriceFromDocument(DocumentSnapshot document, String fieldName) {
        try {
            Object priceObj = document.get(fieldName);
            return formatPrice(priceObj);
        } catch (Exception e) {
            return "₱0.00";
        }
    }

    /**
     * @param priceObj
     * @return
     */
    public static String formatPrice(Object priceObj) {
        if (priceObj == null) {
            return "₱0.00";
        }

        if (priceObj instanceof String) {
            String priceStr = ((String) priceObj).trim();

            priceStr = priceStr.replace("₱", "").replace("PHP", "").trim();

            if (priceStr.isEmpty()) {
                return "₱0.00";
            }

            try {
                double priceValue = Double.parseDouble(priceStr);
                return String.format(Locale.getDefault(), "₱%.2f", priceValue);
            } catch (NumberFormatException e) {
                return priceStr.startsWith("₱") ? priceStr : "₱" + priceStr;
            }
        }
        else if (priceObj instanceof Number) {
            double priceValue = ((Number) priceObj).doubleValue();
            return String.format(Locale.getDefault(), "₱%.2f", priceValue);
        }

        return "₱0.00";
    }

    /**
     * @param priceString
     * @return
     */
    public static double getPriceValue(String priceString) {
        if (priceString == null || priceString.trim().isEmpty()) {
            return 0.0;
        }

        try {
            String cleanPrice = priceString.replace("₱", "")
                    .replace("PHP", "")
                    .replace(",", "")
                    .trim();
            return Double.parseDouble(cleanPrice);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * @param price
     * @return
     */
    public static String formatPriceValue(double price) {
        return String.format(Locale.getDefault(), "₱%.2f", price);
    }

    /**
     * @param priceString
     * @return
     */
    public static String removeCurrencySymbol(String priceString) {
        if (priceString == null) {
            return "0.00";
        }

        return priceString.replace("₱", "")
                .replace("PHP", "")
                .replace(",", "")
                .trim();
    }

    /**
     * @param priceString
     * @return
     */
    public static boolean isValidPrice(String priceString) {
        if (priceString == null || priceString.trim().isEmpty()) {
            return false;
        }

        try {
            String cleanPrice = removeCurrencySymbol(priceString);
            double value = Double.parseDouble(cleanPrice);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param priceString
     * @return
     */
    public static String ensurePesoSign(String priceString) {
        if (priceString == null || priceString.trim().isEmpty()) {
            return "₱0.00";
        }

        priceString = priceString.trim();
        if (priceString.startsWith("₱")) {
            return priceString;
        }

        return "₱" + priceString;
    }

    /**
     * @param price
     * @param asNumber
     * @return
     */
    public static Object toStorageFormat(double price, boolean asNumber) {
        if (asNumber) {
            return price;
        } else {
            return formatPriceValue(price);
        }
    }
}