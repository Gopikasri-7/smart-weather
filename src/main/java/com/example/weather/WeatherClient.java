package com.example.weather;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.util.Optional;

public class WeatherClient {
    private static final Gson gson = new Gson();

    // Get current weather by city (data/2.5/weather)
    public static Optional<String> fetchCurrentByCity(String city) {
        try {
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s",
                    URLEncoder.encode(city, "UTF-8"),
                    URLEncoder.encode(Config.API_KEY, "UTF-8")
            );
            String resp = httpGet(url);
            if (resp == null) return Optional.empty();
            return Optional.of(resp);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Encoding error: " + e.getMessage());
            return Optional.empty();
        }
    }

    // Get 5-day / 3-hour forecast by city (data/2.5/forecast)
    public static Optional<String> fetch5DayForecastByCity(String city) {
        try {
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric&appid=%s",
                    URLEncoder.encode(city, "UTF-8"),
                    URLEncoder.encode(Config.API_KEY, "UTF-8")
            );
            String resp = httpGet(url);
            if (resp == null) return Optional.empty();
            return Optional.of(resp);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Encoding error: " + e.getMessage());
            return Optional.empty();
        }
    }

    // Helper for HTTP GET using HttpURLConnection
    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                System.err.println("No response stream. HTTP code: " + code);
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            if (code != 200) {
                System.err.println("HTTP " + code + " response: " + sb.toString());
                return null;
            }
            return sb.toString();
        } catch (IOException e) {
            System.err.println("HTTP request failed: " + e.getMessage());
            return null;
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            if (conn != null) conn.disconnect();
        }
    }
}
