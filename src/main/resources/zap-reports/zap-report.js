/**
 * JSON structure of an alert object in the alerts array 
 *
{
  "other":"",
  "param":"",
  "alert":"Incomplete or No Cache-control and Pragma HTTP Header Set",
  "evidence":"no-cache=\"set-cookie, set-cookie2\"",
  "confidence":"Medium",
  "solution":"Whenever possible ensure the cache-control HTTP header is set with no-cache, no-store, must-revalidate, private; and that the pragma HTTP header is set with no-cache.",
  "url":"https://10.9.106.93/en/Home",
  "reference":"https://www.owasp.org/index.php/Session_Management_Cheat_Sheet#Web_Content_Caching",
  "id":"0",
  "risk":"Low",
  "description":"The cache-control and pragma HTTP header have not been set properly or are missing allowing the browser and proxies to cache content.",
  "attack":"",
  "messageId":"1",
  "cweid":"525",
  "wascid":"0"
}
*/

var ZAP = (function() {
    var reportTemplate = "<table> \
    <tr><td class=\"key\">Id</td><td class=\"value\">{{id}}</td></tr> \
    <tr><td class=\"key\">Risk</td><td class=\"value\">{{risk}}</td></tr> \
    <tr><td class=\"key\">URL</td><td class=\"value\">{{url}}</td></tr> \
    <tr><td class=\"key\">Alert</td><td class=\"value\">{{alert}}</td></tr> \
    <tr><td class=\"key\">Confidence</td><td class=\"value\">{{confidence}}</td></tr> \
    <tr><td class=\"key\">Description</td><td class=\"value\">{{description}}</td></tr> \
    <tr><td class=\"key\">Evidence</td><td class=\"value\">{{evidence}}</td></tr> \
    <tr><td class=\"key\">Params</td><td class=\"value\">{{param}}</td></tr> \
    <tr><td class=\"key\">Attack</td><td class=\"value\">{{attack}}</td></tr> \
    <tr><td class=\"key\">Other</td><td class=\"value\">{{other}}</td></tr> \
    <tr><td class=\"key\">Message ID</td><td class=\"value\">{{messageId}}</td></tr> \
    <tr><td class=\"key\">Solution</td><td class=\"value\">{{solution}}</td></tr> \
    <tr><td class=\"key\">CWEid</td><td class=\"value\">{{cweid}}</td></tr> \
    <tr><td class=\"key\">WASCid</td><td class=\"value\">{{wascid}}</td></tr> \
    <tr><td class=\"key\">Reference</td><td class=\"value\"><a href=\"{{reference}}\">{{reference}}</a></td></tr> \
    </table>";
    
    var printStyle = "<link rel=\"stylesheet\" href=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/themes/smoothness/jquery-ui.css\"> \
        <style> \
        table { \
            border-collapse: collapse; \
            border: 2px black solid; \
            font: 12px sans-serif; \
        } \
        td { \
            border: 1px black solid; \
            padding: 5px; \
        } \
        </style>";
    
	var riskLow = 0, 
		riskHigh = 0, 
		riskMedium = 0;
	var jsonData;
	var keys = [ 'id', 'risk', 'url', 'alert', 'fp' ];
    var sortingColumns = ['id', 'risk', 'url'];
    var filterFP = function(d) { if (d['fp'] === 'false') return d; };
    var filter = null;
    
    var loadTable = function(jsonFile, tableDiv, riskDiv, chartDiv) {
        d3.json(jsonFile, function(error, jsonObject)  {
//            if (error)
//                return console.warn(error);
            
            jsonData = jsonObject.alerts;
            drawTable(jsonData, tableDiv);
            buildStatistics(jsonData, riskDiv);
            buildChart(chartDiv);
        });
    };

    // Model
    var toggleFalsePositive = function(id) {
        for (var i=0; i<jsonData.length; i++) {
            jsonRec = jsonData[i];
            if (jsonRec['id'] === id){
                if (jsonRec['fp'] === 'true')
                    jsonRec['fp'] = 'false';
                else
                    jsonRec['fp'] = 'true';
                break;
            }
        }
    };
    
    // Controllers
    var drawTable = function(jsonData, anchor) {
        var table = d3.select("#" + anchor).append("table"); 
        var	thead = table.append("thead");
        var tbody = table.append("tbody");

        thead.append("tr").selectAll("th").data(keys)
                .enter().append("th")
                .text( function(key, index) {
                            return key; });
         
        var rows = tbody.selectAll("tr").data(jsonData).enter().append("tr")
                        .on("dblclick", function(d) { return showZAPRecord(d); });
        
        var cells = rows.selectAll("td").data(
                    function(row) {
                        // Return the row value
                        return keys.map(function(key) {
                            if (key === 'fp') {
                                row['fp'] = 'false';
                                return {
                                    column : key,
                                    value : row[key]
                                };
                            } 
                            else {
                                return {
                                    column : key,
                                    value : row[key]
                                };
                            };
                        });
                    }).enter().append("td")
                              .attr("style", "font-family: Courier")
                              .text(function(d) { return d.value; });
                              
        d3.selectAll("thead th").data(sortingColumns).on("click", function(k) {
            rows.sort(function(a,b) {
            	if (k === "risk") {
					var test = {"Low":0, "Medium":1, "High": 2 };
					return test[b[k]] - test[a[k]];
            	}
            	else if (k === "id") {
            		return parseInt(a[k],10) < parseInt(b[k],10) ? -1 : parseInt(a[k],10) > parseInt(b[k],10) ? 1 : 0;
            	}
            	else {
		            return a[k] < b[k] ? -1 : a[k] > b[k] ? 1 : 0; 
            	}
            });
        });        
        
    };
        
    var updateTable = function(filter) {
        var rows;
        var table = d3.select("#AlertsTable_ph"); 
        
        var tbody = table.select("tbody");
        
        if ((filter === undefined) || (filter === null)) {
            rows = tbody.selectAll("tr").data(jsonData);
        }
        else {
            rows = tbody.selectAll("tr").data(jsonData.filter(filter));
        }
        
        var cells = rows.selectAll("td").data(
                    function(row) {
                        // Return the row value
                        return keys.map(function(key) {
                            return {
                                column : key,
                                value : row[key]
                            };
                        });
                    });
                    
        cells.enter().append("td");
        cells.text(function(d) { return d.value; });
        cells.exit().remove();

        var cells_in_new_rows = rows.enter().append('tr')
                                            .selectAll('td')
                                            .data(function(row) {
                                                // Return the row value
                                                return keys.map(function(key) {
                                                    return {
                                                        column : key,
                                                        value : row[key]
                                                    };
                                                });
                                            });

        cells_in_new_rows.enter().append('td').attr("style", "font-family: Courier");

        cells_in_new_rows.text(function(d) { return d.value; });

        rows.exit().remove();
    };
  
    buildStatistics = function(jsonData, anchor) {
        for(var i=0; i<jsonData.length; i++) {
            var alert = jsonData[i];
            
            if (alert.risk === 'Low') {
                riskLow += 1;
            } else if (alert.risk === 'Medium') {
                riskMedium += 1;
            } else if (alert.risk === 'High') {
                riskHigh += 1;
            }
        }
        d3.select("#" + anchor + "High").html(riskHigh.toString());
        d3.select("#" + anchor + "Medium").html(riskMedium.toString());
        d3.select("#" + anchor + "Low").html(riskLow.toString());
    };
    
    
    buildChart = function(anchor) {
        var pie = new d3pie(anchor, {
            "header" : {
                "title" : {
                    "text" : "Risks",
                    "fontSize" : 22,
                    "font" : "verdana"
                },
                "subtitle" : {
                    "text" : "Subtitle 1",
                    "color" : "ZAProxy security tests",
                    "fontSize" : 10,
                    "font" : "verdana"
                },
                "titleSubtitlePadding" : 12
            },
            "footer" : {
                "text" : "Source: ZAProxy risks",
                "color" : "#999999",
                "fontSize" : 11,
                "font" : "open sans",
                "location" : "bottom-center"
            },
            "size" : {
                "canvasHeight" : 400,
                "canvasWidth" : 400,
                "pieOuterRadius" : "88%"
            },
            "data" : {
                "content" : [ {
                    "label" : "Low Risk",
                    "value" : riskLow,
                    "color" : "Yellow"
                }, {
                    "label" : "Medium Risk",
                    "value" : riskMedium,
                    "color" : "Orange"
                }, {
                    "label" : "High Risk",
                    "value" : riskHigh,
                    "color" : "Red"
                } ]
            },
            "labels" : {
                "outer" : {
                    "pieDistance" : 32
                },
                "inner" : {
                    "format" : "value"
                },
                "mainLabel" : {
                    "font" : "verdana"
                },
                "percentage" : {
                    "color" : "#e1e1e1",
                    "font" : "verdana",
                    "decimalPlaces" : 0
                },
                "value" : {
                    "color" : "#000000",
                    "font" : "verdana"
                },
                "lines" : {
                    "enabled" : true,
                    "color" : "#cccccc"
                },
                "truncation" : {
                    "enabled" : true
                }
            },
            "effects" : {
                "pullOutSegmentOnClick" : {
                    "effect" : "linear",
                    "speed" : 400,
                    "size" : 8
                }
            }
        });
    }; // buildChar
    
    var showZAPRecord = function(jsonRec) {
        d3.select("#dialog").html(Mustache.render(reportTemplate, jsonRec));
        $("#dialog").dialog({
            position: {my: "center", at: "center", of: window.top},
            width: 700,
            height: 600,
            buttons: {
                "False Positive": function () {
                    $(this).dialog("close");
                    toggleFalsePositive(jsonRec.id);
                    updateTable(filter)
                }
            }
        });
    };
    
    var printTotalReports = function(anchorToPrint, extrasAnchor, filter) {
    
        var mywindow = window.open('', 'Zap Report', '');
        var printDiv = $(anchorToPrint).clone(true);
        var rows;
        
        if ((filter === undefined) || (filter === null)) {
            rows = jsonData;
        }
        else {
            rows = jsonData.filter(filter);
        }
      
        $.each(rows, function(index, obj) { 
            printDiv.find(extrasAnchor).append("<p>" + Mustache.render(reportTemplate, obj) + "</p>"); 
        });
        
        mywindow.document.write('<html><head><title>Zap Report</title>');
        mywindow.document.write(printStyle);
        mywindow.document.write('</head><body>');
        mywindow.document.write(printDiv.html());
        mywindow.document.write('</body></html>');

        mywindow.document.close();
        mywindow.focus();

        mywindow.print();
        mywindow.close();
        
        return true;        
    };
    
    var saveReportAsDocx = function(anchorToPrint, extrasAnchor, filter) {
    
        var rows;
        var printDiv = $(anchorToPrint);
        var printDivClone = $(anchorToPrint).clone(true);
        
        if ((filter === undefined) || (filter === null)) {
            rows = jsonData;
        }
        else {
            rows = jsonData.filter(filter);
        }        

        /* Create SVG */
        var svg = printDiv.find("#AlertsChart_ph > svg")[0];
        var svgData = new XMLSerializer().serializeToString( svg );
        var svgSize = svg.getBoundingClientRect();
        var cnvs = document.createElement( "canvas" );
        cnvs.width = svgSize.width;
        cnvs.height = svgSize.height;        
        cnvs.setAttribute("id", "CANVAS");
        var ctx = cnvs.getContext( "2d" );
        var img = document.createElement( "img" );
        img.setAttribute( "src", "data:image/svg+xml;base64," + btoa( svgData ) );
        img.onload = function() {
            ctx.drawImage( img, 0, 0 );
            var svg = printDivClone.find("#AlertsChart_ph > svg")[0];
            svg.remove();
            var imgPNG = document.createElement( "img" );
            imgPNG.setAttribute( "src", cnvs.toDataURL() );
            printDivClone.find("#AlertsChart_ph").append(imgPNG);
        };
        
        /* Add the reports */
        $.each(rows, function(index, obj) { 
            printDivClone.find(extrasAnchor).append("<p>" + Mustache.render(reportTemplate, obj) + "</p>"); 
        });
        
        /* Create full report */
        /* totReport = '<html><head><title>Zap Report</title>';
        totReport += printStyle;
        totReport += '</head><body>';
        totReport += printDiv.html();
        totReport += '</body></html>'; */
        
        var mywindow = window.open('', 'Zap Report', '');
        mywindow.document.write('<html><head><title>Zap Report</title>');
        mywindow.document.write(printStyle);
        mywindow.document.write('</head><body>');
        mywindow.document.write(printDivClone.html());
        mywindow.document.write('</body></html>');

        mywindow.document.close();
        mywindow.addEventListener("focus", function load(event){
            window.removeEventListener("focus", load, false); //remove listener, no longer needed
            var converted = htmlDocx.asBlob(mywindow.document.documentElement.innerHTML);
            saveAs(converted, 'test.docx');
        });
        
        mywindow.focus();

/*        jQHTML = $.parseHTML(totReport)
        jQHTML.find("html").wordExport(); */
        
        mywindow.close();

        return true;        
    };
    
	return {
		buildPage: function(jsonFile, tableDiv, riskDiv, chartDiv) {
            loadTable(jsonFile, tableDiv, riskDiv, chartDiv);
        },
        toggleFalsePositives: function() {
            if (filter === filterFP)
                filter = null;
            else
                filter = filterFP;
            updateTable(filter);
        },
        printReports: function(anchor, anchorExtras) {
            printTotalReports(anchor, anchorExtras, filter)
        },
        saveAsDocx: function(anchor, anchorExtras) {
            saveReportAsDocx(anchor, anchorExtras, filter)
        }
    };
})();

$( document ).ready( function(e) {
            ZAP.buildPage("zaproxy.json", "AlertsTable_ph", "Risk", "AlertsChart_ph");			
        } );

$(function() {
    $( "#removeFP" )
        .button()
        .click(function( event ) {
            ZAP.toggleFalsePositives();
        });
});

$(function() {
    $( "#printFP" )
        .button()
        .click(function( event ) {
            ZAP.printReports("#PrintDiv", "#AppendReports");
        });
});

$(function() {
    $( "#saveAsDocx" )
        .button()
        .click(function( event ) {
            ZAP.saveAsDocx("#PrintDiv", "#AppendReports");
        });
});