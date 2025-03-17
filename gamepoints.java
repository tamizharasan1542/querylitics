import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/game")
public class gamepoints extends HttpServlet {
    public Connection connection;
    public Statement stmt;

    @Override
    public void init() throws ServletException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/leaderboard", "username", "TAM@5989");
            stmt = connection.createStatement();
        } catch (Exception e) {
            System.out.println("error" + e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();
        JSONArray topContestants = new JSONArray();

        try {
            int number = Integer.parseInt(request.getParameter("empid"));
            int points = Integer.parseInt(request.getParameter("points"));
            System.out.println("EmpID: " + number + " Points: " + points);

          
            String queryString = "INSERT INTO battle (empid, score) VALUES(" + number + "," + points + ");";
            stmt.executeUpdate(queryString);

           
            String fetchTop10Query = "SELECT empid, score FROM battle ORDER BY score DESC LIMIT 10;";
            ResultSet rs = stmt.executeQuery(fetchTop10Query);

           
            while (rs.next()) {
                JSONObject contestant = new JSONObject();
                contestant.put("empid", rs.getInt("empid"));
                contestant.put("score", rs.getInt("score"));
                topContestants.put(contestant);
            }

           
            jsonResponse.put("message", "success");
            jsonResponse.put("topContestants", topContestants);
            
        } catch (Exception e) {
            jsonResponse.put("message", "failure");
            jsonResponse.put("error", e.getMessage());
        }

        out.print(jsonResponse.toString());
        out.flush();
    }
    @Override
    public void destroy() {
    	
    }
}
