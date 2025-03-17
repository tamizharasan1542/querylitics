package cassandraPlayground;
import com.datastax.oss.driver.api.core.CqlSession;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/createTable")
public class CassandraAPIHandler extends HttpServlet {
    private static final String MOCKAROO_API_KEY = "e01c55e0";  // Mockaroo API key
    private static final int BATCH_SIZE = 500;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        JSONObject jsonResponse = new JSONObject();	
       
        try (BufferedReader reader = request.getReader()) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }

            JSONObject requestJson = new JSONObject(requestBody.toString());
            String dbName = requestJson.getString("database");
            String tableName = requestJson.getString("table");
            String replicationFactor = requestJson.getString("replicationFactor");
            JSONArray columnsArray = requestJson.getJSONArray("columns");

            jsonResponse = createTable(dbName, tableName, replicationFactor, columnsArray);
        } catch (Exception e) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", e.getMessage());
        }

        response.getWriter().write(jsonResponse.toString());
    }

    private static JSONObject createTable(String dbName, String tableName, String replicationFactor, JSONArray columnsArray) {
        JSONObject jsonResponse = new JSONObject();
        try (CqlSession session = CqlSession.builder().build()) {
            createKeyspace(session, dbName, replicationFactor);
            createTableStructure(session, dbName, tableName, columnsArray);
            String mockData = fetchMockDataFromMockaroo(columnsArray, 1000);
            batchInsertData(session, dbName, tableName, columnsArray, mockData, 10);
            jsonResponse.put("status", "success");
            jsonResponse.put("message", "Successfully created table " + tableName);
        } catch (Exception e) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", e.getMessage());
        }
        return jsonResponse;
    }

    private static void batchInsertData(CqlSession session, String dbName, String tableName, JSONArray columnsArray, String mockData, int numRecords) {
        JSONArray jsonArray = new JSONArray(mockData);
        int originalDataCount = jsonArray.length();

        for (int i = 0; i < numRecords; i += BATCH_SIZE) {
            StringBuilder batchQuery = new StringBuilder("BEGIN BATCH ");

            for (int j = i; j < Math.min(i + BATCH_SIZE, numRecords); j++) {
                JSONObject record = jsonArray.getJSONObject(j % originalDataCount);
                StringBuilder insertQuery = new StringBuilder("INSERT INTO " + dbName + "." + tableName + " (uuid_col, ");
                UUID newUuid = UUID.randomUUID();
                StringBuilder valuesPart = new StringBuilder("VALUES (" + newUuid + ", ");

                for (int k = 0; k < columnsArray.length(); k++) {
                    JSONObject column = columnsArray.getJSONObject(k);
                    String columnName = column.getString("name");
                    String columnType = column.getString("type");
                    insertQuery.append(columnName).append(", ");
                    valuesPart.append(formatValue(record, columnName, columnType)).append(", ");
                }

                insertQuery.setLength(insertQuery.length() - 2);
                valuesPart.setLength(valuesPart.length() - 2);
                insertQuery.append(") ").append(valuesPart).append("); ");

                batchQuery.append(insertQuery);
            }

            batchQuery.append(" APPLY BATCH;");
            session.executeAsync(batchQuery.toString());
        }
    }

    private static String formatValue(JSONObject record, String columnName, String columnType) {
        switch (columnType.toLowerCase()) {
            case "int":
                return String.valueOf(record.getInt(columnName));
            case "varchar":
                return "'" + record.getString(columnName).replace("'", "''") + "'";
            case "boolean":
                return String.valueOf(record.getBoolean(columnName));
            case "date":
                return "'" + record.getString(columnName) + "'";
            case "double":
            case "float":
                return String.valueOf(record.getDouble(columnName));
            default:
                return "'" + record.getString(columnName).replace("'", "''") + "'";
        }
    }

    private static void createKeyspace(CqlSession session, String dbName, String replicationFactor) {
        String query = String.format("CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class': 'SimpleStrategy', 'replication_factor': %s};", 
                                      dbName, replicationFactor);
        session.execute(query);
    }

    private static void createTableStructure(CqlSession session, String dbName, String tableName, JSONArray columnsArray) {
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS " + dbName + "." + tableName + " (uuid_col UUID PRIMARY KEY, ");
        for (int i = 0; i < columnsArray.length(); i++) {
            JSONObject column = columnsArray.getJSONObject(i);
            query.append(column.getString("name")).append(" ").append(mapCassandraType(column.getString("type"))).append(", ");
        }
        query.setLength(query.length() - 2);
        query.append(");");
        session.execute(query.toString());
    }

    private static String fetchMockDataFromMockaroo(JSONArray columnsArray, int numRecords) throws IOException {
        JSONArray fieldsArray = new JSONArray();
        for (int i = 0; i < columnsArray.length(); i++) {
            JSONObject column = columnsArray.getJSONObject(i);
            JSONObject field = new JSONObject();
            field.put("name", column.getString("name"));
            field.put("type", mapMockarooType(column.getString("type")));
            fieldsArray.put(field);
        }

        String apiUrl = "https://api.mockaroo.com/api/generate.json?key=" + MOCKAROO_API_KEY + "&count=" + numRecords;
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(fieldsArray.toString().getBytes("UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private static String mapCassandraType(String type) {
        switch (type.toLowerCase()) {
            case "int": return "int";
            case "varchar": return "text";
            case "float": return "double";
            case "date": return "timestamp";
            case "double": return "double";
            case "boolean": return "boolean";
            default: return "text";
        }
    }

    private static String mapMockarooType(String type) {
        switch (type.toLowerCase()) {
            case "int": return "Number";
            case "varchar": return "Full Name";
            case "float": return "Number";
            case "date": return "Datetime";
            case "double": return "Decimal";
            case "boolean": return "Boolean";
            default: return "String";
        }
    }
}
