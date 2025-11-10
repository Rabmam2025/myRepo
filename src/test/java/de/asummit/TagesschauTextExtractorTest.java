package de.asummit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for TagesschauTextExtractor.
 *
 * These tests avoid network calls and only verify small, testable
 * pieces of the utility class (private escapeHtml method and
 * utility-class invariants).
 */
public class TagesschauTextExtractorTest {

    @Test
    void escapeHtml_nullReturnsEmpty() throws Exception {
        Method escape = TagesschauTextExtractor.class
                .getDeclaredMethod("escapeHtml", String.class);
        escape.setAccessible(true);

        String result = (String) escape.invoke(null, (String) null);
        assertEquals("", result, "null input should return empty string");
    }

    @Test
    void escapeHtml_escapesSpecialChars() throws Exception {
        Method escape = TagesschauTextExtractor.class
                .getDeclaredMethod("escapeHtml", String.class);
        escape.setAccessible(true);

        String input = "<&\"'" + "a"; // includes < & " '
        String expected = "&lt;&amp;&quot;&#39;a";
        String result = (String) escape.invoke(null, input);
        assertEquals(expected, result);
    }

    @Test
    void classIsUtility() throws Exception {
        Class<TagesschauTextExtractor> cls = TagesschauTextExtractor.class;
        // Class should be final
        assertTrue(Modifier.isFinal(cls.getModifiers()), "Class should be final");

        // Constructor should be present and private
        Constructor<?> ctor = cls.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor should be private");
    }
}
