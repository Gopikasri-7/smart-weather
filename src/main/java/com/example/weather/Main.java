package com.example.weather;

import java.util.Optional;
import java.util.Scanner;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Smart Weather Forecasting App (Console) - Enhanced");
        if (Config.API_KEY == null || Config.API_KEY.trim().isEmpty() || Config.API_KEY.contains("<PUT_YOUR_KEY_HERE>")) {
            System.err.println("ERROR: Set your OpenWeather API key in Config or environment variable OPENWEATHER_API_KEY.");
            return;
        }

        CacheDB.init();
        AlertManager alerts = new AlertManager();
        Scanner sc = new Scanner(System.in);

        printHelp();

        while (true) {
            System.out.print("\nEnter command or city ('help' for commands): ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            String cmd = line.toLowerCase();

            if (cmd.equals("exit")) break;
            if (cmd.equals("help")) { printHelp(); continue; }

            // Export CSV command: export <city>
            if (cmd.startsWith("export ")) {
                String c = line.substring(7).trim();
                if (c.isEmpty()) {
                    System.out.println("Usage: export <city>");
                    continue;
                }
                Optional<String> cached = CacheDB.getCached(c);
                if (!cached.isPresent()) {
                    System.out.println("City not found in cache. Please fetch it first by typing the city name.");
                } else {
                    String msg = CSVExporter.export(c, cached.get());
                    System.out.println(msg);
                }
                continue;
            }

            // Alert commands
            if (cmd.startsWith("alert ")) {
                String body = line.substring(6).trim().toLowerCase();
                if (body.isEmpty()) System.out.println("Usage: alert temp<20  OR alert temp>30  OR alert rain");
                else {
                    boolean ok = alerts.addAlert(body);
                    if (ok) System.out.println("Alert added: " + body);
                    else System.out.println("Alert already exists or invalid.");
                }
                continue;
            }
            if (cmd.equals("alerts")) {
                List<String> list = alerts.listAlerts();
                if (list.isEmpty()) System.out.println("No alerts set.");
                else {
                    System.out.println("Saved alerts:");
                    for (int i = 0; i < list.size(); i++) {
                        System.out.printf(" %d) %s\n", i+1, list.get(i));
                    }
                }
                continue;
            }
            if (cmd.startsWith("remove ")) {
                try {
                    int idx = Integer.parseInt(cmd.substring(7).trim());
                    boolean ok = alerts.removeAlert(idx);
                    if (ok) System.out.println("Removed alert #" + idx);
                    else System.out.println("Invalid alert index.");
                } catch (NumberFormatException e) {
                    System.out.println("Usage: remove <alert-number>");
                }
                continue;
            }

            // sample shortcut
            if (cmd.equals("sample")) {
                String sampleJson = "{"
                        + "\"current\":{\"dt\":1700000000,\"temp\":30.2,\"feels_like\":31.1,\"weather\":[{\"description\":\"clear sky\"}],\"wind\":{\"speed\":2.3},\"main\":{\"humidity\":65}},"
                        + "\"forecast\":{\"list\":["
                        + "{\"dt\":1700000000,\"main\":{\"temp\":31.0}},"
                        + "{\"dt\":1700038800,\"main\":{\"temp\":29.5}},"
                        + "{\"dt\":1700125200,\"main\":{\"temp\":28.0}}"
                        + "]}"
                        + "}";
                CacheDB.put("sample", sampleJson);
                ForecastPrinter.printSummaryCombined("SampleCity", sampleJson);
                List<String> trig = alerts.checkAlerts("SampleCity", sampleJson);
                for (String t : trig) System.out.println("\u001B[31m[ALERT]\u001B[0m " + t);
                continue;
            }

            // Try cache
            Optional<String> cached = CacheDB.getCached(line);
            if (cached.isPresent()) {
                System.out.println("Using cached data (fresh).");
                ForecastPrinter.printSummaryCombined(line, cached.get());
                List<String> trig = alerts.checkAlerts(line, cached.get());
                for (String t : trig) System.out.println("\u001B[31m[ALERT]\u001B[0m " + t);
                continue;
            }

            // Fetch current + forecast via free endpoints
            Optional<String> currentJson = WeatherClient.fetchCurrentByCity(line);
            if (!currentJson.isPresent()) {
                System.out.println("Failed to fetch current weather.");
                continue;
            }
            Optional<String> forecastJson = WeatherClient.fetch5DayForecastByCity(line);
            if (!forecastJson.isPresent()) {
                System.out.println("Failed to fetch forecast.");
                continue;
            }

            String combined = "{\"current\":" + currentJson.get() + ",\"forecast\":" + forecastJson.get() + "}";
            CacheDB.put(line, combined);
            ForecastPrinter.printSummaryCombined(line, combined);

            // check alerts
            List<String> triggered = alerts.checkAlerts(line, combined);
            for (String t : triggered) {
                System.out.println("\u001B[31m[ALERT]\u001B[0m " + t);
            }
        }

        sc.close();
        System.out.println("Bye!");
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  <city name>          Fetch weather for city (eg: Tirupati)");
        System.out.println("  sample               Show local sample data");
        System.out.println("  export <city>        Export cached city's 5-day forecast to CSV (city_forecast.csv)");
        System.out.println("  alert temp<20        Add alert when current temp < 20°C");
        System.out.println("  alert temp>30        Add alert when current temp > 30°C");
        System.out.println("  alert rain           Add alert when rain is expected in forecast");
        System.out.println("  alerts               List saved alerts");
        System.out.println("  remove <n>           Remove alert number n (see list)");
        System.out.println("  help                 Show this help");
        System.out.println("  exit                 Quit");
    }
}
