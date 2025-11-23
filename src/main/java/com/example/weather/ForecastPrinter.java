package com.example.weather;

import com.google.gson.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Prints a friendly, colored weather summary with emojis, feels-like, humidity, wind.
 * Uses basic ANSI escape codes for coloring (works on modern terminals).
 */
public class ForecastPrinter {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ANSI colors
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";

    public static void printSummaryCombined(String city, String combinedJson) {
        JsonObject root = JsonParser.parseString(combinedJson).getAsJsonObject();

        JsonObject current = root.getAsJsonObject("current");
        double temp = Double.NaN;
        double feels = Double.NaN;
        int humidity = -1;
        double wind = Double.NaN;
        String weatherDesc = "";

        if (current != null) {
            try {
                if (current.has("main")) {
                    JsonObject main = current.getAsJsonObject("main");
                    if (main.has("temp")) temp = main.get("temp").getAsDouble();
                    if (main.has("feels_like")) feels = main.get("feels_like").getAsDouble();
                    if (main.has("humidity")) humidity = main.get("humidity").getAsInt();
                } else {
                    if (current.has("temp")) temp = current.get("temp").getAsDouble();
                    if (current.has("feels_like")) feels = current.get("feels_like").getAsDouble();
                }
                if (current.has("wind")) {
                    JsonObject w = current.getAsJsonObject("wind");
                    if (w.has("speed")) wind = w.get("speed").getAsDouble();
                }
                if (current.has("weather")) {
                    JsonArray wa = current.getAsJsonArray("weather");
                    if (wa.size() > 0) weatherDesc = wa.get(0).getAsJsonObject().get("description").getAsString();
                }
            } catch (Exception ignored) {}
        }

        String emoji = emojiFor(weatherDesc);
        String title = String.format("%s %sWeather for %s%s", emoji, CYAN, city, RESET);

        System.out.println();
        System.out.print(title + " — ");
        if (!Double.isNaN(temp)) {
            System.out.printf("%s%.1f°C%s", YELLOW, temp, RESET);
        } else {
            System.out.print("N/A");
        }
        if (!Double.isNaN(feels)) {
            System.out.printf(" (Feels like %s%.1f°C%s)", MAGENTA, feels, RESET);
        }
        if (humidity >= 0) {
            System.out.printf("  Humidity: %s%d%%%s", GREEN, humidity, RESET);
        }
        if (!Double.isNaN(wind)) {
            System.out.printf("  Wind: %s%.1f m/s%s", CYAN, wind, RESET);
        }
        System.out.printf("  Condition: %s%s%s\n", YELLOW, weatherDesc, RESET);

        // Print aggregated 5-day forecast same as before (if present)
        JsonObject forecastObj = root.getAsJsonObject("forecast");
        if (forecastObj == null || !forecastObj.has("list")) {
            System.out.println("No forecast data available.");
            return;
        }

        JsonArray list = forecastObj.getAsJsonArray("list");
        Map<String, double[]> dailyMinMax = new LinkedHashMap<>();
        for (JsonElement e : list) {
            try {
                JsonObject item = e.getAsJsonObject();
                long dt = item.get("dt").getAsLong();
                String date = Instant.ofEpochSecond(dt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                JsonObject main = item.getAsJsonObject("main");
                double t = main.get("temp").getAsDouble();
                if (!dailyMinMax.containsKey(date)) dailyMinMax.put(date, new double[]{t, t});
                else {
                    double[] mm = dailyMinMax.get(date);
                    mm[0] = Math.min(mm[0], t);
                    mm[1] = Math.max(mm[1], t);
                }
            } catch (Exception ex) { /* skip */ }
        }

        System.out.println();
        System.out.println("5-day aggregated forecast:");
        int count = 0;
        for (Map.Entry<String, double[]> en : dailyMinMax.entrySet()) {
            if (count++ >= 5) break;
            double min = en.getValue()[0], max = en.getValue()[1];
            String dayEmoji = dayEmojiFor(min, max);
            System.out.printf(" %s — %s%.1f%s/%s%.1f%s %s\n",
                    en.getKey(),
                    GREEN, min, RESET,
                    YELLOW, max, RESET,
                    dayEmoji);
        }

        // ASCII chart
        System.out.println();
        System.out.println("5-day highs (ASCII):");
        count = 0;
        for (Map.Entry<String, double[]> en : dailyMinMax.entrySet()) {
            if (count++ >= 5) break;
            double max = en.getValue()[1];
            int bars = (int) Math.round(max);
            System.out.printf("%s | ", en.getKey());
            for (int b = 0; b < Math.max(0, bars); b++) System.out.print("#");
            System.out.printf(" %s%.1f°C%s\n", YELLOW, max, RESET);
        }
        System.out.println();
    }

    // Map brief weather description to emoji
    private static String emojiFor(String desc) {
        if (desc == null) return "";
        String d = desc.toLowerCase();
        if (d.contains("clear")) return "☀️";
        if (d.contains("cloud")) return "☁️";
        if (d.contains("rain") || d.contains("shower") || d.contains("drizzle")) return "🌧️";
        if (d.contains("thunder")) return "⛈️";
        if (d.contains("snow")) return "❄️";
        if (d.contains("mist") || d.contains("fog") || d.contains("haze")) return "🌫️";
        return "🌤️";
    }

    // Day-level emoji based on temps (simple)
    private static String dayEmojiFor(double min, double max) {
        if (max >= 35) return " 🔥";
        if (max >= 30) return " ☀️";
        if (max >= 25) return " 🌤️";
        if (max >= 20) return " 🌦️";
        if (max >= 10) return " 🧥";
        return " ❄️";
    }
}
