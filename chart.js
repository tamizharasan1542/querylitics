let dbChart;

function initializeChart() {
    const ctx = document.getElementById('dbChart').getContext('2d');
    
    // Default performance data
    const defaultData = {
        labels: ['MySQL', 'PostgreSQL', 'Cassandra', 'MongoDB'],
        datasets: [
            {
                label: 'Query Time (ms)',
                data: [120, 85, 95, 75],
                borderColor: '#3b82f6',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                fill: true,
                tension: 0.4,
                pointRadius: 6,
                pointBackgroundColor: '#3b82f6',
                borderWidth: 2
            },
            {
                label: 'Write Time (ms)',
                data: [95, 70, 85, 65],
                borderColor: '#10b981',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                fill: true,
                tension: 0.4,
                pointRadius: 6,
                pointBackgroundColor: '#10b981',
                borderWidth: 2
            }
        ]
    };

    dbChart = new Chart(ctx, {
        type: 'line',
        data: defaultData,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        color: '#f3f4f6',
                        font: {
                            size: 14,
                            weight: 'bold'
                        },
                        padding: 20
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(17, 24, 39, 0.9)',
                    titleColor: '#f3f4f6',
                    bodyColor: '#f3f4f6',
                    padding: 12,
                    cornerRadius: 8,
                    callbacks: {
                        label: (tooltipItem) => `${tooltipItem.dataset.label}: ${tooltipItem.raw} ms`
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(243, 244, 246, 0.1)'
                    },
                    ticks: {
                        color: '#f3f4f6',
                        font: {
                            size: 12
                        },
                        callback: (value) => `${value} ms`
                    },
                    title: {
                        display: true,
                        text: 'Response Time (milliseconds)',
                        color: '#f3f4f6',
                        font: {
                            size: 14,
                            weight: 'bold'
                        }
                    }
                },
                x: {
                    grid: {
                        color: 'rgba(243, 244, 246, 0.1)'
                    },
                    ticks: {
                        color: '#f3f4f6',
                        font: {
                            size: 12,
                            weight: 'bold'
                        }
                    }
                }
            },
            animations: {
                tension: {
                    duration: 1000,
                    easing: 'easeInOutCubic'
                }
            }
        }
    });

    // Add hover animation
    ctx.canvas.addEventListener('mousemove', (event) => {
        const points = dbChart.getElementsAtEventForMode(event, 'nearest', { intersect: true }, false);
        if (points.length) {
            ctx.canvas.style.cursor = 'pointer';
        } else {
            ctx.canvas.style.cursor = 'default';
        }
    });
}

// Function to fetch database timings
async function fetchDBTimings() {
    try {
        const reqData = {
            tableName: "testtable9",
            columns: [{
                name: "id",
                type: "int"
            }],
            numRecords: 10
        };

        const response = await fetch('CompareWith4DB', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(reqData)
        });

        const data = await response.json();
        
        if (data.message === "success") {
            const dbLabels = data.results.map(db => db.dbName);
            const queryTimes = data.results.map(db => db.timeTaken);
            const writeTimes = data.results.map(db => db.timeTaken * 0.8); // Simulated write times
            
            updateChart(dbLabels, queryTimes, writeTimes);
        } else {
            console.error("Error fetching data:", data.error);
        }
    } catch (error) {
        console.error("Request failed:", error);
    }
}

// Function to update chart with new data
function updateChart(labels, queryTimes, writeTimes) {
    dbChart.data.labels = labels;
    dbChart.data.datasets[0].data = queryTimes;
    dbChart.data.datasets[1].data = writeTimes;
    dbChart.update();
}