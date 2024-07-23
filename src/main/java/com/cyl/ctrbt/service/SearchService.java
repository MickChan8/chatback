package com.cyl.ctrbt.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cyl.ctrbt.entity.BingNewsResult;
import com.cyl.ctrbt.entity.SearchResult;
import com.cyl.ctrbt.entity.Weather;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    private static final String BING_SEARCH_URL = "https://www.bing.com/search?q=";
    private static final String BING_NEWS_SEARCH_URL = "https://api.bing.microsoft.com/v7.0/news/search?textDecorations=True&textFormat=HTML&count=10&sortBy=Date&q=";

    @Value("${bing.secret_key}")
    private String bingKey;

    public String getPageText(String url){

        StringBuilder textContent = new StringBuilder();

        try {
          // 使用Jsoup连接并获取网页文档
          Document document = Jsoup.connect(url).get();

          // 获取文档中的所有元素
          Elements elements = document.getAllElements();

          // 遍历所有元素并提取文本内容
          for (Element element : elements) {
            // 忽略脚本和样式内容
            if (!element.tagName().equals("script") && !element.tagName().equals("style")) {
              // 获取元素的文本内容
              String text = element.ownText();
              if (!text.isEmpty()) {
                textContent.append(text).append("\n");
              }
            }
          }

          // 输出提取的文本内容
//          System.out.println(textContent.toString());

        } catch (IOException e) {
          e.printStackTrace();
        }

        return textContent.toString();
    }

    public List<BingNewsResult> searchBingNews(String query) throws IOException {

        String searchUrl = BING_NEWS_SEARCH_URL + URLEncoder.encode(query, "UTF-8");

        URL url = new URL(searchUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", bingKey);

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

        return JSONUtil.parseObj(response).getJSONArray("value").toList(BingNewsResult.class);
    }

    public List<SearchResult> searchBing(String query) throws IOException {
        List<SearchResult> results = new ArrayList<>();
        String searchUrl = BING_SEARCH_URL + query.replace(" ", "+");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(searchUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    System.out.println("Request failed with status code: " + statusCode);
                    return results;
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Document doc = Jsoup.parse(entity.getContent(), null, searchUrl);
                    Elements resultElements = doc.select(".b_algo");

                    for (Element resultElement : resultElements) {
                        String title = resultElement.select("h2 a").text();
                        String url = resultElement.select("h2 a").attr("href");
                        String summary = resultElement.select(".b_caption p").text();

                        if (!title.isEmpty() && !url.isEmpty()) {
                            SearchResult result = new SearchResult(title, url, summary);
                            results.add(result);
                        }
                    }
                } else {
                    System.out.println("Response entity is null.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public List<SearchResult> searchBaidu(String query) throws  IOException{
        List<SearchResult> results = new ArrayList<>();
        //百度
        String url = "https://www.baidu.com/s?wd=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

        // Initialize HttpClient
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // Execute the request
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // Parse the response HTML using Jsoup
                Document doc = Jsoup.parse(entity.getContent(), "UTF-8", "");

                // Select the search result links
                Elements links = doc.select("h3.t a");
                for (Element link : links) {
                    String linkText = link.text();
                    String absoluteUrl = link.attr("abs:href");

                    SearchResult result = new SearchResult(linkText, absoluteUrl, "");
                    results.add(result);

                }
            }
        }
        return results;
    }

}
