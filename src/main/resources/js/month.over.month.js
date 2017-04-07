monthOverMonth = function() {
  var monthOverMonth = {
    version: "0.0.1"
  };

 monthOverMonth.draw = function(lineHeight,margin,lr,svgSelector,regressionDash,trendColor,m_msg,r2,div,tooltip,tooltipFont,momTitle,momDescription,momAverage) {
  var marginLineHeight = 1.2*lineHeight;
  var iconSize = 13;
  var refSize = iconSize*2.5;
  var half = refSize/2.0;
  var refAxisStroke= 0.9;
  var refGridStroke = 0.25;
  var refStroke = 1;
  var graphColor = "#8899FF";
  var gridSpace = refSize / 6.0;
  var momMidX = margin.left - half - 3; 
  var momMidY = margin.trend/2.0; 
  var x_graph_middle = momMidX; 
  var y_graph_middle = momMidY + 2*marginLineHeight; 
  var xshift = -10;
  var y0 = -refSize;
  var pcmsg_y = y0-refSize;
  var percentChange = momAverage > 0 ? lr.slope/momAverage: 0;
  if ("NaN"==""+percentChange) {
    percentChange=0;
  }
  var pc = percentChange*100;
  var pcsign = pc >= 0 ? "+" : "";
  var pcmsg = pcsign + pc.toFixed(1) + "%"; 
  var yshift = trendHeight - refSize - 6;
  var refWidth = 6*gridSpace;
  var mom = d3.select(svgSelector)
      .append("g");

  // reference horizontal
  for (i = -3; i <= 3; i++) { 
    var strokewidth = i==0?refAxisStroke:refGridStroke;
    mom.append("line")
     .attr("x1", x_graph_middle-half)
     .attr("x2", x_graph_middle+half)
     .attr("y1", y_graph_middle + i*gridSpace)
     .attr("y2", y_graph_middle + i*gridSpace)
     .attr("stroke-width", strokewidth)
     .attr("stroke", graphColor);
  }

  // reference vertical
  for (i = -3; i <= 3; i++) { 
    var strokewidth = i==0?refAxisStroke:refGridStroke;
    mom.append("line")
     .attr("x1", x_graph_middle + i*gridSpace)
     .attr("x2", x_graph_middle + i*gridSpace)
     .attr("y1", y_graph_middle - half)
     .attr("y2", y_graph_middle + half)
     .attr("stroke-width", strokewidth)
     .attr("stroke", graphColor);
  }

  // reference slope
  if (-1.0 <= percentChange && percentChange <= 1.0) {
    mom.append("line")
     .attr("x1", x_graph_middle-half)
     .attr("x2", x_graph_middle+half)
     .attr("y1", y_graph_middle + percentChange * half)
     .attr("y2", y_graph_middle - percentChange * half)
     .attr("stroke-width", refStroke)
     .style("stroke-dasharray", (regressionDash)) 
     .attr("stroke", trendColor);
  } else {
    var direction = percentChange > 0 ? -1 : 1; 
    mom.append("line")
     .attr("x1", x_graph_middle - half / percentChange)
     .attr("x2", x_graph_middle + half / percentChange)
     .attr("y1", y_graph_middle + half)
     .attr("y2", y_graph_middle - half)
     .attr("stroke-width", refStroke)
     .style("stroke-dasharray", (regressionDash)) 
     .attr("stroke", trendColor);
  }
  mom.append("text")
      .attr("x", margin.left)
      .attr("y", momMidY-3*marginLineHeight)
      .attr("dy", ".35em")
      .style("font-size", fontSize)
      .style("text-anchor", "end")
      .style("fill", "black")
      .text(momTitle)
      .on("mouseover", function(d) {      
            div.transition()        
                .duration(200)      
                .style("opacity", .9);      
            div .html(momDescription)  
                .style("left", (d3.event.pageX) + "px")     
                .style("top", (d3.event.pageY - 28) + "px")
                .style("width", jQuery.fn.textWidth(momDescription,tooltipFont)*tooltip + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });

  mom.append("text")
      .attr("x", margin.left)
      .attr("y", momMidY-2*marginLineHeight)
      .attr("dy", ".35em")
      .style("font-size", fontSize)
      .style("text-anchor", "end")
      .style("fill", "black")
      .text(m_msg + " MoM")
        .on("mouseover", function(d) {      
            div.transition()        
                .duration(200)      
                .style("opacity", .9);      
            div .html("Expected change in "+momTitle+" month-over-month: " + m_msg)  
                .style("left", (d3.event.pageX) + "px")     
                .style("top", (d3.event.pageY - 28) + "px")
                .style("width", jQuery.fn.textWidth("Expected change in "+momTitle+" month-over-month: " + m_msg,tooltipFont)*tooltip + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });
  mom.append("text")
      .attr("x", margin.left)
      .attr("y", momMidY-1*marginLineHeight)
      .attr("dy", ".35em")
      .style("font-size", fontSize)
      .style("text-anchor", "end")
      .style("fill", "black")
      .text(pcmsg)
        .on("mouseover", function(d) {      
            div.transition()        
                .duration(200)      
                .style("opacity", .9);      
            div .html("Expected percent change in "+momTitle+" month-over-month: "+pcmsg)  
                .style("left", (d3.event.pageX) + "px")     
                .style("top", (d3.event.pageY - 28) + "px")
                .style("width", jQuery.fn.textWidth("Expected percent change in "+momTitle+" month-over-month: "+pcmsg,tooltipFont)*tooltip + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });

  // R2 label
  var r2msg = "R<sup>2</sup> correlation coefficient of "+r2+" implies "+Math.round(r2*100)+"% confidence month-over-month trend will continue.";
  var r2X = margin.left-40;
  mom.append("text")
      .attr("x", r2X)
      .attr("y", momMidY)
      .attr("dy", ".35em")
      .style("font-size", fontSize)
      .text("R")  
        .on("mouseover", function(d) {      
            div.transition()        
                .duration(200)      
                .style("opacity", .9);      
            div .html(r2msg)  
                .style("left", (d3.event.pageX) + "px")     
                .style("top", (d3.event.pageY - 28) + "px")
                .style("width", jQuery.fn.textWidth(r2msg,tooltipFont)*tooltip + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });
  mom.append("text")
      .attr("x", margin.left)
      .attr("y", momMidY)
      .attr("dy", ".35em")
      .style("font-size", fontSize)
      .style("text-anchor", "end")
      .text(r2)  
        .on("mouseover", function(d) {      
            div.transition()        
                .duration(200)      
                .style("opacity", .9);      
            div .html(r2msg)  
                .style("left", (d3.event.pageX) + "px")     
                .style("top", (d3.event.pageY - 28) + "px")
                .style("width", jQuery.fn.textWidth(r2msg,tooltipFont)*tooltip + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });
  mom.append("text")
      .attr("x", r2X + 8)
      .attr("y", momMidY-0.2*marginLineHeight)
      .attr("dy", ".35em")
      .style("font-size", fontSize-3)
      .text("2")  
        .on("mouseover", function(d) {      
            div.transition()        
                .duration(200)      
                .style("opacity", .9);      
            div .html(r2msg)  
                .style("left", (d3.event.pageX) + "px")     
                .style("top", (d3.event.pageY - 28) + "px")
                .style("width", jQuery.fn.textWidth(r2msg,tooltipFont)*tooltip + "px");    
            })                  
        .on("mouseout", function(d) {       
            div.transition()        
                .duration(500)      
                .style("opacity", 0);   
        });
  return mom;
 }

return monthOverMonth;
}();
