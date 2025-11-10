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

/**
 * Utility to fetch Tagesschau JSON and write simplified HTML output.
 */
public final class TagesschauTextExtractor {

    /** Default API endpoint URL for fetching Tagesschau news data. */
    private static final String DEFAULT_API_URL =
        "https://www.tagesschau.de/api2u/homepage/";

    /** Default output file path for the generated HTML. */
    private static final String DEFAULT_OUTPUT_FILE = "news.html";

    /**
     * Private constructor to prevent instantiation.
     */
    private TagesschauTextExtractor() {
        // Utility class.
    }

    /**
     * Main entry point for the text extractor.
     *
     * @param args command line arguments (not used)
     * @throws Exception on network or IO errors
     */
    public static void main(final String[] args) throws Exception {
        final String url = System.getProperty("tagesschau.api.url",
            DEFAULT_API_URL);
        final String outputFilePath = System.getProperty("output.file.path",
            DEFAULT_OUTPUT_FILE);
        final int httpOk = 200;

        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = client.send(
            request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == httpOk) {
            String responseBody = response.body();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            JsonNode newsArray = rootNode.get("news");

            if (newsArray != null && newsArray.isArray()) {
                try (BufferedWriter writer =
                        new BufferedWriter(new FileWriter(outputFilePath))) {

                    writer.write("<!DOCTYPE html>\n");
                    writer.write("<html lang=\"de\">\n");
                    writer.write("<head>\n");
                    writer.write("<meta charset=\"UTF-8\">\n");
                    writer.write("<title>Tagesschau News</title>\n");
                    writer.write("</head>\n");
                    writer.write("<body>\n");

                    for (JsonNode newsItem : newsArray) {
                        String title = newsItem.path("title")
                            .asText("Kein Titel");

                        String id = title.toLowerCase()
                            .replaceAll("[^a-z0-9\\s-]", "")
                            .replaceAll("\\s+", "-");

                        writer.write("<div class=\"article\" id=\"");
                        writer.write(id);
                        writer.write("\">\n");

                        writer.write("<h2>");
                        writer.write(escapeHtml(title));
                        writer.write("</h2>\n");

                        JsonNode contentArray = newsItem.get("content");

                        if (contentArray != null && contentArray.isArray()) {
                            StringBuilder topicText = new StringBuilder();

                            for (JsonNode contentPart : contentArray) {
                                if ("text".equals(contentPart
                                        .path("type")
                                        .asText(null))) {

                                    String htmlValue = contentPart
                                        .path("value")
                                        .asText("");
                                    String unescaped = StringEscapeUtils
                                        .unescapeHtml4(htmlValue);
                                    String cleanText = Jsoup.parse(unescaped)
                                        .text();
                                    topicText.append(cleanText)
                                        .append("\n\n");
                                }
                            }

                            String escapedText = escapeHtml(
                                topicText.toString());
                            writer.write("<p>");
                            writer.write(escapedText.replace(
                                "\n\n", "<br/><br/>"));
                            writer.write("</p>\n");
                        } else {
                            writer.write("<p>");
                            writer.write("(Keine Textinhalte für "
                                + "diesen Artikel gefunden)");
                            writer.write("</p>\n");
                        }

                        writer.write("</div>\n\n");
                    }

                    writer.write("</body>\n");
                    writer.write("</html>");
                    System.out.print(
                        "HTML-Datei wurde erfolgreich geschrieben: ");
                    System.out.println(outputFilePath);
                }
            } else {
                System.out.println(
                    "Kein 'news'-Array in der JSON-Antwort gefunden.");
            }
        } else {
            System.out.print("Anfrage fehlgeschlagen mit Statuscode: ");
            System.out.println(response.statusCode());
        }
    }

    /**
     * Escapes HTML special characters in a string.
     *
     * @param text input text that may contain HTML special characters
     * @return escaped text safe for embedding in HTML
     */
    private static String escapeHtml(final String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
