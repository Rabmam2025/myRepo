package de.asummit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class TagesschauTextExtractorIntegrationTest {
    
    private MockWebServer mockWebServer;
    private Path outputPath;
    
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        outputPath = tempDir.resolve("test-news.html");
    }
    
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
    
    @Test
    void processValidJsonResponse() throws Exception {
        // Given: a mock news API response
        String jsonResponse = "{"
            + "\"news\": [{"
            + "  \"title\": \"Test News Article\","
            + "  \"content\": [{"
            + "    \"type\": \"text\","
            + "    \"value\": \"This is a test article content.\""
            + "  }]"
            + "}]"
            + "}";
            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(jsonResponse));
            
        // When: running the extractor with our mock server URL
        String[] args = new String[0]; // args aren't used but method requires it
        System.setProperty("tagesschau.api.url", 
            mockWebServer.url("/").toString());
        System.setProperty("output.file.path", 
            outputPath.toString());
            
        TagesschauTextExtractor.main(args);
        
        // Then: verify the output file exists and contains expected content
        assertTrue(Files.exists(outputPath), 
            "Output file should be created");
            
        String content = Files.readString(outputPath);
        assertThat(content)
            .contains("Test News Article")
            .contains("This is a test article content.")
            .contains("<html")
            .contains("</html>");
    }
    
    @Test
    void handleErrorResponse() throws Exception {
        // Given: a server error response
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Server Error"));
            
        // When/Then: verify error handling
        String[] args = new String[0];
        System.setProperty("tagesschau.api.url", 
            mockWebServer.url("/").toString());
        System.setProperty("output.file.path", 
            outputPath.toString());
            
        TagesschauTextExtractor.main(args);
        
        // Verify no output file is created on error
        assertThat(Files.exists(outputPath))
            .as("Output file should not be created on error")
            .isFalse();
    }
    
    @Test
    void handleEmptyNewsArray() throws Exception {
        // Given: valid JSON but empty news array
        String jsonResponse = "{\"news\": []}";
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(jsonResponse));
            
        // When: running the extractor
        String[] args = new String[0];
        System.setProperty("tagesschau.api.url", 
            mockWebServer.url("/").toString());
        System.setProperty("output.file.path", 
            outputPath.toString());
            
        TagesschauTextExtractor.main(args);
        
        // Then: verify basic HTML structure is present
        assertTrue(Files.exists(outputPath), 
            "Output file should be created even with no news");
        String content = Files.readString(outputPath);
        assertThat(content)
            .contains("<html")
            .contains("</html>")
            .doesNotContain("<div class=\"article\"");
    }
}