package de.asummit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jsoup.Jsoup;
import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TagesschauTextExtractor {

    public static void main(String[] args) throws Exception {
        String url = "https://www.tagesschau.de/api2u/homepage/";
        String outputFilePath = "news.html";

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);

            JsonNode newsArray = rootNode.get("news");

            if (newsArray != null && newsArray.isArray()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                    writer.write("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n<meta charset=\"UTF-8\">\n<title>Tagesschau News</title>\n</head>\n<body>\n");

                    for (JsonNode newsItem : newsArray) {
                        String title = newsItem.path("title").asText("Kein Titel");

                        // Erzeuge eine ID-sichere Zeichenkette aus dem Titel (vereinfacht)
                        String id = title.toLowerCase()
                                .replaceAll("[^a-z0-9\\s-]", "") // entferne Sonderzeichen
                                .replaceAll("\\s+", "-");        // ersetze Leerzeichen durch '-'

                        writer.write("<div class=\"article\" id=\"" + id + "\">\n");
                        writer.write("<h2>" + escapeHtml(title) + "</h2>\n");

                        JsonNode contentArray = newsItem.get("content");

                        if (contentArray != null && contentArray.isArray()) {
                            StringBuilder topicText = new StringBuilder();

                            for (JsonNode contentPart : contentArray) {
                                if ("text".equals(contentPart.path("type").asText(null))) {
                                    String htmlValue = contentPart.path("value").asText("");
                                    String unescaped = StringEscapeUtils.unescapeHtml4(htmlValue);
                                    String cleanText = Jsoup.parse(unescaped).text();
                                    topicText.append(cleanText).append("\n\n");
                                }
                            }

                            String escapedText = escapeHtml(topicText.toString());
                            writer.write("<p>" + escapedText.replace("\n\n", "<br/><br/>") + "</p>\n");
                        } else {
                            writer.write("<p>(Keine Textinhalte für diesen Artikel gefunden)</p>\n");
                        }

                        writer.write("</div>\n\n");
                    }

                    writer.write("</body>\n</html>");
                    System.out.println("HTML-Datei wurde erfolgreich geschrieben: " + outputFilePath);
                }
            } else {
                System.out.println("Kein 'news'-Array in der JSON-Antwort gefunden.");
            }
        } else {
            System.out.println("Anfrage fehlgeschlagen mit Statuscode: " + response.statusCode());
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
