package com.example.weather;

import com.google.gson.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class CSVExporter {

    /**
     * Export aggregated 5-day forecast from combinedJson and return status message.
     * combinedJson should be the wrapper: {"current":..., "forecast": ...}
     */
    public static String export(String city, String combinedJson) {
        if (combinedJson == null) return "No data provided.";

        try {
            JsonObject root = JsonParser.parseString(combinedJson).getAsJsonObject();
            JsonObject forecastObj = root.getAsJsonObject("forecast");

            if (forecastObj == null || !forecastObj.has("list")) {
                return "No forecast data to export.";
            }

            JsonArray list = forecastObj.getAsJsonArray("list");

            // Aggregate min/max temperatures per date
            Map<String, double[]> daily = new LinkedHashMap<>();

            for (JsonElement e : list) {
                try {
                    JsonObject item = e.getAsJsonObject();
                    long dt = item.get("dt").getAsLong();

                    String date = Instant.ofEpochSecond(dt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                    JsonObject main = item.getAsJsonObject("main");
                    double temp = main.get("temp").getAsDouble();

                    if (!daily.containsKey(date)) daily.put(date, new double[]{temp, temp});
                    else {
                        double[] mm = daily.get(date);
                        mm[0] = Math.min(mm[0], temp);
                        mm[1] = Math.max(mm[1], temp);
                    }
                } catch (Exception ex) {
                    // skip malformed list entry
                }
            }

            // Create CSV file
            String safeCity = city.trim().toLowerCase().replaceAll("\\s+", "_");
            String fileName = safeCity + "_forecast.csv";
            try (FileWriter fw = new FileWriter(fileName)) {
                fw.write("date,min_temp,max_temp\n");

                int count = 0;
                for (Map.Entry<String, double[]> en : daily.entrySet()) {
                    if (count++ >= 5) break; // limit to 5 days
                    fw.write(String.format("%s,%.2f,%.2f\n", en.getKey(), en.getValue()[0], en.getValue()[1]));
                }
            }

            return "CSV exported: " + fileName;

        } catch (IOException ex) {
            return "Failed to export CSV: " + ex.getMessage();
        } catch (Exception ex) {
            return "Export failed: invalid forecast data.";
        }
    }
}
