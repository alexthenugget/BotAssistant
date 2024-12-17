package com.github.ALEX_THE_NUGGET.BOT_ASSISTANT;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherHandler {
    private final String weatherToken = System.getenv("WEATHER_TOKEN");
    private YandexTranslate translateText = new YandexTranslate();

    private String returnResponse(String cityName) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder responseContent = new StringBuilder();
        String inputLine = null;
        try {
            String urlString = String.format("https://pro.openweathermap.org/data/2.5/weather?q=%s&appid=%s", cityName, weatherToken);
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            if (status > 299) {
                throw new IOException("HTTP request failed with status: " + status);
            }
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((inputLine = reader.readLine()) != null) {
                responseContent.append(inputLine);
            }
            return responseContent.toString();

        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            return "Error fetching data for city: " + cityName;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    public String returnWeather(String cityName) throws IOException {
        String response = returnResponse(cityName);
        JSONObject jsonResponse = new JSONObject(response);
        String temp = String.valueOf(jsonResponse.getJSONObject("main").getDouble("temp") - 273.15);
        String feelsLike = String.valueOf(jsonResponse.getJSONObject("main").getDouble("feels_like") - 273.15);
        String tempMin = String.valueOf(jsonResponse.getJSONObject("main").getDouble("temp_min") - 273.15);
        String tempMax = String.valueOf(jsonResponse.getJSONObject("main").getDouble("temp_max") - 273.15);
        String atmospherePressure = String.valueOf(jsonResponse.getJSONObject("main").getDouble("pressure"));
        String windSpeed = String.valueOf(jsonResponse.getJSONObject("wind").getDouble("speed"));
        JSONArray arrWeather = jsonResponse.getJSONArray("weather");
        String weather = "";
        String description = "";
        for (int i = 0; i < arrWeather.length(); i++) {
            weather = translateText.translate(arrWeather.getJSONObject(i).getString("main"), "ru");
            description = translateText.translate(arrWeather.getJSONObject(i).getString("description"), "ru");

        }
        return "Температура воздуха: " + temp + "\n" +
                "Ощущается как: " + feelsLike + "\n" +
                "Минимальная температура: " + tempMin + "\n" +
                "Максимальная температура: " + tempMax + "\n" +
                "Атмосферное давление: " + atmospherePressure + "\n" +
                "Скорость ветра: " + windSpeed + "\n" +
                "Погодные явления: " + weather + "\n" +
                "Описание: " + description;
    }
}
