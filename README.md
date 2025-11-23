# 🌦️ Smart Weather Forecasting App

A Java-based Weather Forecasting Application that provides real-time weather, 5-day forecast, automatic alerts, emoji-based UI, and CSV export features using the OpenWeather API.  
This project is console-based, clean, powerful, and perfect for resume, viva, and GitHub portfolio.

---

## 🚀 Features

* ✔ **Live Weather Data** using OpenWeather API
* ✔ **5-Day Aggregated Forecast**
* ✔ **Weather Emojis** for easy visualization (☀️ 🌧️ 🌫️ ❄️)
* ✔ **Feels-Like Temperature, Humidity & Wind Speed**
* ✔ **Auto Alerts System**
  * `alert temp<20` → alerts when temperature drops
  * `alert temp>30` → alerts for high temperature
  * `alert rain` → alerts if rain is expected
* ✔ **Data Caching (SQLite)** to reduce API calls
* ✔ **CSV Export**
  * `export chennai` → creates `chennai_forecast.csv`
* ✔ **ASCII Temperature Charts**
* ✔ **Clean and Structured CLI Interface**
* ✔ **Java 8 + Maven Project**

---

## 🛠 Technologies Used

| Technology | Purpose |
|------------|---------|
| ☕ Java 8 | Core programming language |
| 📦 Maven | Dependency & project management |
| 🌍 OpenWeather API | Weather & forecast data source |
| 🗄️ SQLite | Local caching of city weather data |
| 📘 Gson | JSON parsing |
| 🖥️ ANSI Colors | Rich CLI formatting |

---

## 📂 Project Structure

```
smart-weather/
├─ src/main/java/com/example/weather/
│  ├─ Main.java
│  ├─ WeatherClient.java
│  ├─ ForecastPrinter.java
│  ├─ CacheDB.java
│  ├─ Config.java
│  ├─ AlertManager.java
│  └─ CSVExporter.java
├─ pom.xml
├─ alerts.txt
├─ sample_forecast.csv (optional)
└─ chennai_forecast.csv (exported file)
```

---

## 🖥 How It Works

**1. Run the app**
```bash
mvn exec:java
```

**2. Get weather**
```
Chennai
```

**3. Set alerts**
```
alert temp<20
alert rain
```

**4. Export forecast**
```
export chennai
```

---

## 🎯 Perfect For

| User Type | Use Case |
|-----------|----------|
| 👨‍🎓 Students | API integration + Java project |
| 💼 Developers | CLI data app example |
| 📊 Data Science | CSV export + processing |
| 🎤 Viva/College | High-quality project demonstration |
