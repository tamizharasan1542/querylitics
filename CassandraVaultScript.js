lucide.createIcons();

const CASSANDRA_QUERIES = [
  {
    name: "Create KeyspaceTable",
    queryEx: "CREATE TABLE user_sessions ( user_id UUID PRIMARY KEY, session_data TEXT ) WITH replication = { 'class': 'NetworkTopologyStrategy', 'datacenter1': 3, 'datacenter2': 2 }; \n\n\nINSERT INTO user_sessions (user_id, session_data) VALUES (<some-uuid>, 'active');",
    query: "SELECT * FROM user_sessions WHERE user_id = 2b6770b7-b01a-4a15-85a9-4f7663c341f2;",
    scenario: "Used when setting up a new application that needs its own isolated database space. For example, creating a separate keyspace for a new e-commerce platform to ensure data isolation from other applications.",
    icon: "images/Database.png"
  },
  {
    name: "Event Logging with TTL (Time-To-Live)",
    queryEx: "CREATE TABLE user_activity ( user_id UUID, event_time TIMESTAMP, event_type TEXT, details TEXT, PRIMARY KEY (user_id, event_time) ) WITH CLUSTERING ORDER BY (event_time DESC);",
    query: "INSERT INTO user_activity (user_id, event_time, event_type, details) VALUES (UUID(), toTimestamp(now()), 'LOGIN', 'User logged in') USING TTL 2592000;",
    scenario: "Logging user activity but automatically deleting logs after 30 days.",
    icon: "images/user.png"
  },
  {
    name: "Creating a Materialized View for Fast Lookups",
    queryEx: "CREATE TABLE tasks ( task_id UUID PRIMARY KEY, user_id UUID, priority INT, description TEXT ); \nCREATE MATERIALIZED VIEW tasks_by_priority AS SELECT task_id, user_id, priority, description FROM tasks WHERE priority IS NOT NULL AND task_id IS NOT NULL PRIMARY KEY (priority, task_id);",
    query: "SELECT * FROM tasks_by_priority WHERE priority = 1;",
    scenario: "A task management system filters tasks by user and priority.",
    icon: "images/task.png"
  },
  {
    name: "Counters for Analytics",
    queryEx: "CREATE TABLE page_views (article_id UUID PRIMARY KEY,views COUNTER); \nUPDATE page_views SET views = views + 1 WHERE article_id = 123e4567-e89b-12d3-a456-426614174000;",
    query: "SELECT views FROM page_views WHERE article_id = 323e4567-e89b-12d3-a456-426614174002",
    scenario: "Tracking the number of page views per article.",
    icon: "images/web-traffic.png"
  },
  {
    name: "Distributed Chat Application",
    queryEx: "CREATE TABLE chat_messages (chat_id UUID, message_id UUID, sender_id UUID, message TEXT, timestamp TIMESTAMP, PRIMARY KEY (chat_id, timestamp) ) WITH CLUSTERING ORDER BY (timestamp DESC);",
    query: "SELECT * FROM chat_messages WHERE chat_id = 223e4567-e89b-12d3-a456-426614174005 LIMIT 2;",
    scenario: "Storing chat messages with efficient querying for recent messages.",
    icon: "images/chat.png"
  },
  {
    name: "Secondary Index for Better Filtering",
    queryEx: "CREATE TABLE support_tickets ( ticket_id UUID PRIMARY KEY, user_id UUID, status TEXT, description TEXT ); \nCREATE INDEX status_idx ON support_tickets(status);",
    query: "SELECT * FROM support_tickets WHERE status = 'OPEN';",
    scenario: "A customer service system retrieves tickets by status.",
    icon: "images/customer-service.png"
  },
  {
    name: "Conditional Write with IF EXISTS (Lightweight Transactions)",
    queryEx: "CREATE TABLE users ( username TEXT PRIMARY KEY, email TEXT, password TEXT );",
    query: "INSERT INTO users (username, email, password) VALUES ('john_doe', 'john@example.com', 'securepassword') IF NOT EXISTS;",
    scenario: "A user registration system ensures usernames are unique.",
    icon: "images/favorite.png"
  },
  {
    name: "Create Index",
    queryEx: "CREATE TABLE users_details ( user_id UUID PRIMARY KEY, name TEXT, email TEXT, age INT, created_at TIMESTAMP ); \nCREATE INDEX email_idx ON users (email);",
    query: "SELECT * FROM users_details WHERE email = 'charlie@example.com' ALLOW FILTERING;",
    scenario: "Optimizing email-based user lookups, essential for login systems where users sign in with their email address instead of username. The index on 'email' speeds up queries without requiring a full table scan.",
    icon: "images/message.png"
  },
  {
    name: "JSON Query Support",
    queryEx: "CREATE TABLE product_cache ( product_id UUID PRIMARY KEY, name TEXT, price DECIMAL, last_updated TIMESTAMP );",
    query: "SELECT * FROM product_cache WHERE product_id = 111e4567-e89b-12d3-a456-426614174001;",
    scenario: "In an e-commerce system, frequently accessed product details can be stored in Cassandra as a cache to reduce load on the primary database. This table helps keep a fast-access version of product information.",
    icon: "images/shopping.png"
  },
  {
    name: "Time-Series Data Storage",
    queryEx: "CREATE TABLE sensor_readings (sensor_id UUID,timestamp TIMESTAMP,temperature FLOAT,humidity FLOAT,PRIMARY KEY (sensor_id, timestamp)) WITH CLUSTERING ORDER BY (timestamp DESC);",
    query: "SELECT * FROM sensor_readings WHERE sensor_id = 999e4567-e89b-12d3-a456-426614174009 LIMIT 4;",
    scenario: "Storing IoT sensor readings where new data keeps arriving, and recent data is queried frequently",
    icon: "images/sensor.png"
  }
];

let currentQueryIndex = 0;
let viewedQueries = new Set();
let completionPopupShown = false;

function updateQueryDisplay() {
  const query = CASSANDRA_QUERIES[currentQueryIndex];
  document.getElementById('selectedQueryName').textContent = query.name;
  document.getElementById('scenarioIcon').src = query.icon;
  document.getElementById('scenarioText').textContent = query.scenario;
  document.getElementById('queryContent').textContent = query.queryEx.trim();
  
  viewedQueries.add(currentQueryIndex);
  if (viewedQueries.size === CASSANDRA_QUERIES.length && !completionPopupShown) {
    showCompletionPopup();
  }
}

function copyQuery() {
  const query = CASSANDRA_QUERIES[currentQueryIndex].query.trim();
  navigator.clipboard.writeText(query);
  const notification = document.getElementById('copyNotification');
  notification.classList.remove('hidden');
  setTimeout(() => notification.classList.add('hidden'), 2000);
}

function executeQuery() {
  const query = CASSANDRA_QUERIES[currentQueryIndex].query.trim();
  fetch('http://192.168.121.1:8080/GenerateDataUI/executeCassQuery', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query: query }),
  })
  .then(response => response.json())
  .then(data => {
    const popup = document.getElementById('resultPopup');
    if (data.error) {
      document.getElementById('queryResult').textContent = `Error: ${data.error}`;
      document.getElementById('queryResult').style.color = 'red';
    } else {
      document.getElementById('queryResult').textContent = 
        `Execution Time: ${data.executionTimeMs} ms\nStatus: ${data.status}\n\nExecuted Query:\n${query}`;
      document.getElementById('queryResult').style.color = 'white';
    }
    popup.style.display = 'block';
  })
  .catch(error => {
    console.error('Error:', error);
    const popup = document.getElementById('resultPopup');
    document.getElementById('queryResult').textContent = 'Failed to execute query.';
    document.getElementById('queryResult').style.color = 'red';
    popup.style.display = 'block';
  });
}

function closeResultPopup() {
  document.getElementById('resultPopup').style.display = 'none';
}

function showCompletionPopup() {
  document.getElementById('completionPopup').style.display = 'block';
  completionPopupShown = true;
}

function closeCompletionPopup() {
  document.getElementById('completionPopup').style.display = 'none';
}

function continueFunction() {
  if (window.parent && window.parent.analyse) {
    window.parent.analyse();
  } else {
    console.error("Parent window or configure function not found");
  }
  closeCompletionPopup();
}

document.getElementById('nextQuery').addEventListener('click', () => {
  currentQueryIndex = (currentQueryIndex + 1) % CASSANDRA_QUERIES.length;
  updateQueryDisplay();
});

document.getElementById('prevQuery').addEventListener('click', () => {
  currentQueryIndex = (currentQueryIndex - 1 + CASSANDRA_QUERIES.length) % CASSANDRA_QUERIES.length;
  updateQueryDisplay();
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'ArrowRight') {
    currentQueryIndex = (currentQueryIndex + 1) % CASSANDRA_QUERIES.length;
    updateQueryDisplay();
  } else if (e.key === 'ArrowLeft') {
    currentQueryIndex = (currentQueryIndex - 1 + CASSANDRA_QUERIES.length) % CASSANDRA_QUERIES.length;
    updateQueryDisplay();
  } else if (e.key === 'Enter') {
    executeQuery();
  }
});

updateQueryDisplay();