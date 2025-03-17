package analyzer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.Connection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;


import com.mysql.cj.protocol.x.SyncFlushDeflaterOutputStream;

@WebServlet("/queryAnalyzer")
public class QueryAnalyzer extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
//    String assId = "928000000056307";
    String assId = "928000000076267"; 	
    private static String conversationId = null;
	TokenManager tManager = new TokenManager();
	String accessToken = tManager.getValidAccessToken();
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    System.out.println("Inside doPost");

	    // Read JSON body from request
	    StringBuilder jsonBuffer = new StringBuilder();
	    String line;
	    try (BufferedReader reader = request.getReader()) {
	        while ((line = reader.readLine()) != null) {
	            jsonBuffer.append(line);
	        }
	    }

	    // Convert string to JSON
	    JSONObject jsonRequest = new JSONObject(jsonBuffer.toString());
	    String queryContent = jsonRequest.optString("content", "").trim();

	    System.out.println("Received Query: " + queryContent);

	    if (queryContent.isEmpty()) {
	        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query content is required");
	        return;
	    }

	    // Generate a conversation ID for tracking AI responses
	    String convoId = generateConversationId(queryContent);
	    System.out.println("Using convo ID: " + convoId);

	    // Fetch AI response
	    String aiResponse = getAIResponse(convoId);

	    // Retry logic if AI response is null
	    int retryCount = 0;
	    while (aiResponse == null && retryCount < 3) {
	        try {
	            Thread.sleep(1500);  // Wait for 1.5 seconds
	            aiResponse = getAIResponse(convoId);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	        retryCount++;
	    }

	    // If AI response is still null, return an error message
	    if (aiResponse == null || aiResponse.isEmpty()) {
	    	aiResponse = getAIResponse(convoId);
//	        aiResponse = "AI is still processing the request. Please try again later.";
	    }

	    // Prepare JSON response
	    JSONObject jsonResponse = new JSONObject();
	    jsonResponse.put("result", aiResponse);

	    // Send response to frontend
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    response.getWriter().write(jsonResponse.toString());

	    System.out.println("Sent Response: " + jsonResponse);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		System.out.print("hello");
	    doPost(request, response);  // Redirect GET to POST
	    }

	private String generateConversationId(String userMessage) throws IOException {
	    JSONObject requestBody = new JSONObject();
	//    String assId = getAssId(accessToken);
	    requestBody.put("assistant_id", assId);
	
	    JSONArray messages = new JSONArray();
	    JSONObject message = new JSONObject();
	    message.put("role", "user");
	//        message.put("content", userMessage); 
	    message.put("content", userMessage != null ? userMessage.trim() : ""); // Ensure content is not empty
	
	    messages.put	(message);
	
	    requestBody.put("messages", messages);
	
	    System.out.println("Request Body: " + requestBody);
	
	    RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody.toString());
	    Request request = new Request.Builder()
	            .url("https://platformai.csez.zohocorpin.com/internalapi/v2/ai/assistant/chat")
	            .addHeader("portal_id", "ZS")
	            .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)  // FIXED: Corrected OAuth token format
	            .addHeader("Content-Type", "application/json")
	            .post(body)
	            .build();
	
	    OkHttpClient client = new OkHttpClient();
	    try (Response response = client.newCall(request).execute()) {
	        String responseBody = response.body() != null ? response.body().string() : "";
	        System.out.println("Full Response: " + responseBody);
	
	        if (response.isSuccessful()) {
	            JSONObject jsonResponse = new JSONObject(responseBody);
	            return jsonResponse.optJSONObject("data").optString("conversation_id", null);
	        } else {
	            System.out.println("Request failed with code: " + response.code());
	            System.out.println("Error response: " + responseBody);
	        }
	    }
	    return null;
	}


	public String getAIResponse(String conversationId) throws IOException  {
	
	    String url = "https://platformai.csez.zohocorpin.com/internalapi/v2/ai/assistant/chat?conversation_id=" + conversationId;
	
	    Request request = new Request.Builder()
	            .url(url)  
	            .addHeader("portal_id", "ZS") 
	            .addHeader("Authorization", "Zoho-oauthtoken " + accessToken) 
	            .addHeader("Content-Type", "application/json")
	            .get()
	            .build();
	    
	    System.out.println("AI request ------" + request);
	    
	    OkHttpClient client = new OkHttpClient();
	    
	    try (Response response = client.newCall(request).execute()) {
	        if (response.isSuccessful() && response.body() != null) {
	            
	            // Store response body in a variable
	            String responseBody = response.body().string();
	            
	            System.out.println("Response Body: " + responseBody);  // Debugging
	            
	            JSONObject jsonResponse = new JSONObject(responseBody);
	            
	            if (jsonResponse.has("data")) {  
	                JSONObject dataObject = jsonResponse.getJSONObject("data");  // Get the "data" key
	                if(dataObject.getString("status").equals("in_progress") || dataObject.getString("status").equals("queued") ) {
	                	//return null;
	                	getAIResponse(conversationId);
	                }
	                if (dataObject.has("messages")) {  
	                    JSONArray messagesArray = dataObject.getJSONArray("messages");  // Get the "messages" array
	                    
	                    if (messagesArray.length() > 0) {  
	                        JSONObject firstMessage = messagesArray.getJSONObject(0);  // Get the first message
	                        
	                        if (firstMessage.has("content")) {  
	                            String aiResponse = firstMessage.getString("content");  
	                            System.out.println("AI Response: " + aiResponse);
	                            return aiResponse;
	                        } else {
	                            System.out.println("'content' key not found in the first message.");
	                        }
	                    } else {
	                        System.out.println("'messages' array is empty.");
	                    }
	                } else {
	                    System.out.println("'messages' key is missing in 'data'.");
	                }
	            } else {
	                System.out.println("'data' key is missing in the response.");
	            }
	        } else {
	            System.out.println("Request failed with code: " + response.code());
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return "";
	}
	
	@Override
	public void destroy() {
	    super.destroy();
	}
	static {
	    try {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	    } catch (ClassNotFoundException e) {
	        System.err.println("MySQL JDBC Driver not found.");
	            e.printStackTrace();
	        }
	    }
	}
