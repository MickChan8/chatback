package com.cyl.ctrbt.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.entity.Weather;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class WeatherService {

    @Value("{$gaode.secret_key}")
    private String gaode_key;

    // 发送请求
    public String getWeather() throws IOException {

        URL url = new URL("https://restapi.amap.com/v3/weather/weatherInfo?key=5f79c1717dd5bc6e090fcb6dcd04a8cf&city=310000");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // 取得返回
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }catch(Exception e){
            // 读取错误信息
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                StringBuilder responseErr = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseErr.append(responseLine.trim());
                }
                System.out.println("Error Details: " + responseErr.toString());
            }
        }
        finally {
            connection.disconnect();
        }

        System.out.println("------Get Weather------:");
        System.out.println(response.toString());
        System.out.println("\n");

        JSONObject jsonResponse = JSONUtil.parseObj(response);
        Weather live = jsonResponse.getJSONArray("lives").get(0, Weather.class );

        return "今日上海天气"+live.getWeather()+"，"
            +"温度"+live.getTemperature()+"摄氏度，"
            +live.getWinddirection()+"风"
            +live.getWindpower()+"级，"
            +"空气湿度"+live.getHumidity();
    }
}
