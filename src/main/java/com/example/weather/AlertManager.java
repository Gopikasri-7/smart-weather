package com.example.weather;

import com.google.gson.*;
import java.io.*;
import java.util.*;

/**
 * Simple alert manager that persists alerts in alerts.txt (one per line).
 * Supported alert formats:
 *  - temp<NUMBER    (e.g. temp<20)  -> triggers when CURRENT temperature < NUMBER (°C)
 *  - temp>NUMBER    (e.g. temp>30)  -> triggers when CURRENT temperature > NUMBER (°C)
 *  - rain           -> triggers if forecast contains "rain" or "shower" within next 5 days
 */
public class AlertManager {
    private static final String ALERT_FILE = "alerts.txt";
    private final List<String> alerts = new ArrayList<>();

    public AlertManager() {
        load();
    }

    private void load() {
        alerts.clear();
        File f = new File(ALERT_FILE);
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) alerts.add(line);
            }
        } catch (IOException e) {
            System.err.println("Failed to load alerts: " + e.getMessage());
        }
    }

    private void save() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(ALERT_FILE))) {
            for (String a : alerts) {
                w.write(a);
                w.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to save alerts: " + e.getMessage());
        }
    }

    public List<String> listAlerts() {
        return new ArrayList<>(alerts);
    }

    public boolean addAlert(String alert) {
        alert = alert.trim().toLowerCase();
        if (alert.isEmpty()) return false;
        if (alerts.contains(alert)) return false;
        alerts.add(alert);
        save();
        return true;
    }

    public boolean removeAlert(int index) {
        if (index < 1 || index > alerts.size()) return false;
        alerts.remove(index - 1);
        save();
        return true;
    }

    /**
     * Check alerts against the combined JSON (wrapper {current, forecast}).
     * Returns list of triggered alert messages (human readable).
     */
    public List<String> checkAlerts(String city, String combinedJson) {
        List<String> triggered = new ArrayList<>();
        if (combinedJson == null) return triggered;
        JsonObject root;
        try {
            root = JsonParser.parseString(combinedJson).getAsJsonObject();
        } catch (Exception e) {
            return triggered;
        }

        // current temperature (C)
        Double currentTemp = null;
        try {
            JsonObject current = root.getAsJsonObject("current");
            if (current != null && current.has("main") && current.getAsJsonObject("main").has("temp")) {
                currentTemp = current.getAsJsonObject("main").get("temp").getAsDouble();
            } else if (current != null && current.has("temp")) {
                currentTemp = current.get("temp").getAsDouble();
            }
        } catch (Exception ignored) {}

        // forecast check for rain/shower
        boolean rainExpected = false;
        try {
            JsonObject forecast = root.getAsJsonObject("forecast");
            if (forecast != null && forecast.has("list")) {
                JsonArray list = forecast.getAsJsonArray("list");
                for (JsonElement e : list) {
                    try {
                        JsonObject item = e.getAsJsonObject();
                        if (item.has("weather")) {
                            JsonArray w = item.getAsJsonArray("weather");
                            if (w.size() > 0) {
                                String desc = w.get(0).getAsJsonObject().get("description").getAsString().toLowerCase();
                                if (desc.contains("rain") || desc.contains("shower") || desc.contains("drizzle")) {
                                    rainExpected = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ex) { /* skip */ }
                }
            }
        } catch (Exception ignored) {}

        for (String a : alerts) {
            if (a.startsWith("temp<")) {
                try {
                    double v = Double.parseDouble(a.substring(5));
                    if (currentTemp != null && currentTemp < v) {
                        triggered.add(String.format("Temp Alert: %s current %.1f°C < %.1f°C", city, currentTemp, v));
                    }
                } catch (NumberFormatException ignored) {}
            } else if (a.startsWith("temp>")) {
                try {
                    double v = Double.parseDouble(a.substring(5));
                    if (currentTemp != null && currentTemp > v) {
                        triggered.add(String.format("Temp Alert: %s current %.1f°C > %.1f°C", city, currentTemp, v));
                    }
                } catch (NumberFormatException ignored) {}
            } else if (a.equals("rain")) {
                if (rainExpected) {
                    triggered.add(String.format("Rain Alert: Rain expected in %s in the forecast.", city));
                }
            } else {
                // unknown alert type - ignore or future expansion
            }
        }

        return triggered;
    }
}
