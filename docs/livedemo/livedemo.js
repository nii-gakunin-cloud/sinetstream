var ctx = document.getElementById('myChart').getContext('2d');
var myChart = new Chart(ctx, {
    type: 'line',
    data: {
        labels: [],
        datasets: []
    },
    options: {
        responsive: true,
        title: {
            display: true,
            fontSize: 16,
            text: 'Visualization of Temperature and Humidity Based on DHT11 Sensor'
        },
        scales: {
            xAxes: [{
                type: 'time',
                time: {
                    parser: moment.HTML5_FMT.DATETIME_LOCAL_MS,
                    minUnit: 'minute'
                },
                gridLines: {
                    drawOnChartArea: false
                }
            }],
            yAxes: [{
                id: 'temperature',
                type: 'linear',
                display: true,
                position: 'left',
                scaleLabel: {
                    display: true,
                    labelString: 'Temperature(\u2103)'
                },
                gridLines: {
                    drawOnChartArea: false
                },
                ticks: {
                    beginAtZero: true
                }
            }, {
                id: 'humidity',
                type: 'linear',
                display: true,
                position: 'right',
                scaleLabel: {
                    display: true,
                    labelString: 'Humidity(%)'
                },
                gridLines: {
                    drawOnChartArea: false
                },
                ticks: {
                    suggestedMax: 100,
                    beginAtZero: true
                }
            }]
        },
        legend: {
            position: 'right'
        },
        plugins: {
            colorschemes: {
                scheme: 'brewer.SetOne3'
            }
        }
    }
});

function updateChart() {
    let s3_url = 'https://nii-dp-livedemo.s3-ap-northeast-1.amazonaws.com/livedemo-user1-kafka-dht11/sensor.json';
    $.getJSON(s3_url).done(function( data ) {
        myChart.data.datasets.splice(0);
        $.each( data, function(idx) {
            myChart.data.datasets.push(
                Object.assign({
                    fill: false,
                    yAxisID: this.label
                }, this)
            );
        });
        myChart.update({duration: 0});
    });
}

updateChart();
setInterval(updateChart, 30000);
