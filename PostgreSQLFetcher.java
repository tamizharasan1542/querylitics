package executePage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import org.json.JSONArray;
import org.json.JSONObject;

public class PostgreSQLFetcher {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "tam@123";

    public static JSONObject fetchData(String databaseName, String tableName) {
        JSONObject jsonResponse = new JSONObject();
        Connection conn = null;
        Statement stmt = null;

        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL + databaseName, DB_USER, DB_PASSWORD);
            stmt = conn.createStatement();

            String query = "SELECT * FROM " + tableName + " LIMIT 5";
            long startTime = System.nanoTime();
            ResultSet rs = stmt.executeQuery(query);
            long endTime = System.nanoTime();

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            JSONArray columnNames = new JSONArray();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.put(rsmd.getColumnName(i));
            }

            JSONArray data = new JSONArray();
            while (rs.next()) {
                JSONArray row = new JSONArray();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rs.getString(i));
                }
                data.put(row);
            }

            double executionTimeMs = (endTime - startTime) / 1_000_000.0;
            jsonResponse.put("columns", columnNames);
            jsonResponse.put("data", data);
            jsonResponse.put("execution_time_ms", executionTimeMs);

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            jsonResponse.put("error", e.getMessage());
        }
        return jsonResponse;
    }

    public static JSONObject executeQuery(String databaseName, String query) {
        JSONObject jsonResponse = new JSONObject();
        Connection conn = null;
        Statement stmt = null;

        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(DB_URL + databaseName, DB_USER, DB_PASSWORD);
            stmt = conn.createStatement();

            long startTime = System.nanoTime();
            boolean hasResultSet = stmt.execute(query);
            long endTime = System.nanoTime();
            double executionTimeMs = (endTime - startTime) / 1_000_000.0;

            jsonResponse.put("execution_time_ms", executionTimeMs);

            if (!hasResultSet) {
                int updateCount = stmt.getUpdateCount();
                jsonResponse.put("rows_affected", updateCount);
            }

            stmt.close();
            conn.close();

        } catch (Exception e) {
            jsonResponse.put("error", e.getMessage());
        }

        return jsonResponse;
    }
}
