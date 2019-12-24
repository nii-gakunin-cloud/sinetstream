var chart_opts = {
        responsive: true,
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
    };
var ctx1 = document.getElementById('myChart1').getContext('2d');
var ctx2 = document.getElementById('myChart2').getContext('2d');

var myChart1 = new Chart(ctx1, {
    type: 'line',
    data: {
        labels: [],
        datasets: []
    },
    options: Object.assign({
                 title: {
                     display: true,
                     fontSize: 16,
                     text: 'Visualization of Temperature and Humidity Based on DHT11 Sensor (Last 5 minutes)'
        }
             }, chart_opts)
});
var myChart2 = new Chart(ctx2, {
    type: 'line',
    data: {
        labels: [],
        datasets: []
    },
    options: Object.assign({
                 title: {
                     display: true,
                     fontSize: 16,
                     text: 'Visualization of Temperature and Humidity Based on DHT11 Sensor (Last 24 hours)'
        }
             }, chart_opts)
});

function updateChart(chart, target) {
    let s3_url = 'https://nii-dp-livedemo.s3-ap-northeast-1.amazonaws.com/livedemo-user1-kafka-dht11/' + target;
    $.getJSON(s3_url).done(function( data ) {
        chart.data.datasets.splice(0);
        $.each( data, function(idx) {
            chart.data.datasets.push(
                Object.assign({
                    fill: false,
                    yAxisID: this.label
                }, this)
            );
        });
        chart.update({duration: 0});
    });
}


function updateChart1() {
    updateChart(myChart1, 'sensor.json');
}
function updateChart2() {
    updateChart(myChart2, 'sensor10m.json');
}


updateChart1();
updateChart2();
setInterval(updateChart1, 30000);
setInterval(updateChart2, 300000);
