package com.example.weather;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public class CacheDB {
    private static final String URL = "jdbc:sqlite:weather_cache.db";
    private static final long TTL_SECONDS = 60 * 30; // 30 minutes cache TTL

    public static void init() {
        try (Connection c = DriverManager.getConnection(URL);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS cache (" +
                    "city TEXT PRIMARY KEY, " +
                    "json TEXT NOT NULL, " +
                    "fetched_at INTEGER NOT NULL)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<String> getCached(String city) {
        try (Connection c = DriverManager.getConnection(URL);
             PreparedStatement ps = c.prepareStatement("SELECT json, fetched_at FROM cache WHERE city = ?")) {
            ps.setString(1, city.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                long fetched = rs.getLong("fetched_at");
                if (Instant.now().getEpochSecond() - fetched > TTL_SECONDS) {
                    // stale
                    return Optional.empty();
                }
                return Optional.of(rs.getString("json"));
            }
        } catch (SQLException e) {
            System.err.println("Cache read error: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static void put(String city, String json) {
        // Use upsert; works with modern sqlite-jdbc
        try (Connection c = DriverManager.getConnection(URL);
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO cache(city,json,fetched_at) VALUES(?,?,?) " +
                             "ON CONFLICT(city) DO UPDATE SET json=excluded.json, fetched_at=excluded.fetched_at")) {
            ps.setString(1, city.toLowerCase());
            ps.setString(2, json);
            ps.setLong(3, Instant.now().getEpochSecond());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Cache write error: " + e.getMessage());
            // fallback: try update/insert manually
            try (Connection c = DriverManager.getConnection(URL)) {
                try (PreparedStatement upd = c.prepareStatement("UPDATE cache SET json=?, fetched_at=? WHERE city=?")) {
                    upd.setString(1, json);
                    upd.setLong(2, Instant.now().getEpochSecond());
                    upd.setString(3, city.toLowerCase());
                    int rows = upd.executeUpdate();
                    if (rows == 0) {
                        try (PreparedStatement ins = c.prepareStatement("INSERT INTO cache(city,json,fetched_at) VALUES(?,?,?)")) {
                            ins.setString(1, city.toLowerCase());
                            ins.setString(2, json);
                            ins.setLong(3, Instant.now().getEpochSecond());
                            ins.executeUpdate();
                        }
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Cache fallback failed: " + ex.getMessage());
            }
        }
    }
}
