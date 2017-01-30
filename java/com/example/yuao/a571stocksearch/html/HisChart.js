/** 
 * Version 2.0
 */
var Markit = {};
/**
 * Define the InteractiveChartApi.
 * First argument is symbol (string) for the quote. Examples: AAPL, MSFT, JNJ, GOOG.
 * Second argument is duration (int) for how many days of history to retrieve.
 */
Markit.InteractiveChartApi = function (container, symbol, name) {
    this.symbol = symbol.toUpperCase();
    this.duration = 1095;
    this.container = container;
    this.name = name;
    this.PlotChart();
};

Markit.InteractiveChartApi.prototype.PlotChart = function () {
    var mother = this;
    //Make JSON request for timeseries data
    $.getJSON( phpURL + '?symbol=' + this.symbol + '&chart=true', function (data) {
        mother.render(data);
    });
};

//Markit.InteractiveChartApi.prototype.getInputParams = function(){
//    return {
//        Normalized: false,
//        NumberOfDays: this.duration,
//        DataPeriod: "Day",
//        Elements: [
//            {
//                Symbol: this.symbol,
//                Type: "price",
//                Params: ["ohlc"] //ohlc, c = close only
//            }
//        ]
//        //,LabelPeriod: 'Week',
//        //LabelInterval: 1
//    }
//};

Markit.InteractiveChartApi.prototype._fixDate = function(dateIn) {
    var dat = new Date(dateIn);
    return Date.UTC(dat.getFullYear(), dat.getMonth(), dat.getDate());
};

Markit.InteractiveChartApi.prototype._getOHLC = function(json) {
    var dates = json.Dates || [];
    var elements = json.Elements || [];
    var chartSeries = [];

    if (elements[0]){

        for (var i = 0, datLen = dates.length; i < datLen; i++) {
            var dat = this._fixDate( dates[i] );
            var pointData = [
                dat,
                elements[0].DataSeries['open'].values[i],
                elements[0].DataSeries['high'].values[i],
                elements[0].DataSeries['low'].values[i],
                elements[0].DataSeries['close'].values[i]
            ];
            chartSeries.push( pointData );
        };
    }
    return chartSeries;
};

Markit.InteractiveChartApi.prototype.render = function(data) {
    //console.log(data)
    // split the data set into ohlc and volume
    var ohlc = this._getOHLC(data);
    
    // create the chart
    $(this.container).highcharts('StockChart', {
        
        rangeSelector: {
            selected: 0,
            buttons: [{
                  type: 'week',
                  text: '1w',
                }, {
                  type: 'month',
                  count: 1,
                  text: '1m'
                }, {
                  type: 'month',
                  count: 3,
                  text: '3m'
                }, {
                  type: 'month',
                  count: 6,
                  text: '6m'
                }, {
                  type: 'ytd',
                  text: 'YTD'
                  
                }, {
                  type: 'year',
                  count: 1,
                  text: '1y'
                }, {
                  type: 'all',
                  text: 'All'
                }],
                inputEnabled: false
        },

        title: {
            text: this.symbol + ' Stock Value'
        },

        yAxis: {
            title: {
                text: 'Stock Value'
            }
        },
        
        series: [{
                name : this.symbol,
                data : ohlc,
            		
                type : 'area',
                threshold : null,
                tooltip : {
                    valueDecimals : 2,
                    valuePrefix: '$'
                },
                fillColor : {
                    linearGradient : {
                        x1: 0,
                        y1: 0,
                        x2: 0,
                        y2: 1
                    },
                    stops : [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                    ]
                }
            }],
        exporting: {
            enabled: false
        }
    });
};
                                 
