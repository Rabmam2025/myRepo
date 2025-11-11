package de.asummit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class intentionally contains a SQL injection vulnerability
 * to verify that CodeQL can detect it. Remove this class after verification.
 */
public final class VulnerableExample {

    private VulnerableExample() {
        // Utility class
    }

    /**
     * VULNERABLE: This method is intentionally vulnerable to SQL injection.
     * It concatenates user input directly into a SQL query string.
     * CodeQL should detect this as a CWE-89 SQL Injection vulnerability.
     *
     * @param userId user input (untrusted)
     * @param connection database connection
     * @return result set from vulnerable query
     * @throws SQLException if a database error occurs
     */
    public static ResultSet getUserDataVulnerable(
            final String userId,
            final Connection connection) throws SQLException {

        // VULNERABLE: userId is concatenated directly into SQL query
        String query = "SELECT * FROM users WHERE user_id = " + userId;
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);
    }

    /**
     * SAFE: This method is safe. It uses parameterized queries
     * (prepared statements) instead of string concatenation.
     * CodeQL should NOT flag this as vulnerable.
     *
     * @param userId user input (untrusted)
     * @param connection database connection
     * @return result set from safe query
     * @throws SQLException if a database error occurs
     */
    public static ResultSet getUserDataSafe(
            final String userId,
            final Connection connection) throws SQLException {

        // SAFE: Uses parameterized query with placeholder
        String query = "SELECT * FROM users WHERE user_id = ?";
        var preparedStmt = connection.prepareStatement(query);
        preparedStmt.setString(1, userId);
        return preparedStmt.executeQuery();
    }
}
