package cassandraPlayground;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.ResultSet;

@WebServlet("/fetchData")
public class FetchDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*"); // Allow requests from any origin
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS"); // Allow POST and GET methods
        response.setHeader("Access-Control-Allow-Headers", "Content-Type"); // Allow content-type headers

        // Read the incoming JSON request body
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }

        // Parse the JSON to get database and table
        JSONObject jsonRequest = new JSONObject(requestBody.toString());
        String databaseName = jsonRequest.getString("database");
        String tableName = jsonRequest.getString("table");
        System.out.println(databaseName+tableName);

        try (PrintWriter out = response.getWriter()) {
            // Establish a Cassandra session
            try (CqlSession session = CqlSession.builder().build()) {  // Adjust with your Cassandra contact point if necessary
                // Set the keyspace (database)
                session.execute("USE " + databaseName);

                // Query to fetch all records from the table
                String query = "SELECT * FROM " + tableName;
                ResultSet resultSet = session.execute(query);

                JSONArray jsonArray = new JSONArray();

                // Iterate through the results and convert each row to JSON
                for (Row row : resultSet) {
                    JSONObject rowJson = new JSONObject();
                    // Iterate over all the columns in the row
                    row.getColumnDefinitions().forEach(columnDef -> {
                        String columnName = columnDef.getName().toString();
                        Object columnValue = row.getObject(columnName);
                        rowJson.put(columnName, columnValue);
                    });
                    jsonArray.put(rowJson);
                }

                // Send the result as a JSON array
                out.print(jsonArray.toString());
            } catch (Exception e) {
                // Handle errors (e.g., connection errors, invalid queries)
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}
