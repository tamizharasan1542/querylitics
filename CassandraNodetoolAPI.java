import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/nodetool/status")
public class CassandraNodetoolAPI extends HttpServlet {
    private static final String NODETOOL_PATH = "/Downloads/apache-cassandra-4.1.7/bin/nodetool"; // Update this path

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject jsonResponse = new JSONObject();
        try {
            List<String> nodetoolOutput = executeNodetoolStatus();
            JSONArray nodesArray = parseNodetoolOutput(nodetoolOutput);
            jsonResponse.put("status", "success");
            jsonResponse.put("nodes", nodesArray);
        } catch (Exception e) {
            jsonResponse.put("status", "error");
            jsonResponse.put("message", e.getMessage());
        }

        response.getWriter().write(jsonResponse.toString());
    }

    private List<String> executeNodetoolStatus() throws IOException, InterruptedException {
        List<String> outputLines = new ArrayList<>();
        
        ProcessBuilder processBuilder = new ProcessBuilder(NODETOOL_PATH, "status");
        processBuilder.redirectErrorStream(true); // Merge error stream with output
        
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            outputLines.add(line);
        }

        process.waitFor();
        return outputLines;
    }

    private JSONArray parseNodetoolOutput(List<String> outputLines) {
        JSONArray nodesArray = new JSONArray();
        boolean startParsing = false;

        for (String line : outputLines) {
            if (line.startsWith("UN") || line.startsWith("DN")) {
                startParsing = true;
            }

            if (startParsing) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 6) {
                    JSONObject nodeData = new JSONObject();
                    nodeData.put("status", parts[0]); // UN = Up Normal, DN = Down
                    nodeData.put("address", parts[1]);
                    nodeData.put("load", parts[2]);
                    nodeData.put("tokens", parts[3]);
                    nodeData.put("owns", parts[4]);
                    nodeData.put("host_id", parts[5]);
                    nodesArray.put(nodeData);
                }
            }
        }
        return nodesArray;
    }
}
