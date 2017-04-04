linearRegression = function() {
  var linearRegression = {
    version: "0.0.1"
  };

 // calculate slope, intercept, r-squared given y and x values   
 linearRegression.calculate = function(y,x) {
  var r = {};
  var n = y.length;
  var sum_x = 0;
  var sum_y = 0;
  var sum_xy = 0;
  var sum_xx = 0;
  var sum_yy = 0;

  for (var i = 0; i < y.length; i++) {
   sum_x += x[i];
   sum_y += y[i];
   sum_xy += (x[i]*y[i]);
   sum_xx += (x[i]*x[i]);
   sum_yy += (y[i]*y[i]);
  }

  r['slope'] = (n * sum_xy - sum_x * sum_y) / (n*sum_xx - sum_x * sum_x);
  r['intercept'] = (sum_y - r.slope * sum_x)/n;
  r['r2'] = Math.pow((n*sum_xy - sum_x*sum_y)/Math.sqrt((n*sum_xx-sum_x*sum_x)*(n*sum_yy-sum_y*sum_y)),2);
 
  return r;
 };

 // update data by adding the right value for "regression" to each entry
 linearRegression.updateDataWithIt = function(data,it) {
  for (var i=0;i<data.length;i++) {
   data[i]["regression"] = (it.slope * i) + it.intercept; 
  }
 }

 linearRegression.get = function(data,getfn,lastPercentDone) {
  var known_x = [];
  var known_y = [];
  for (var i=0;i<data.length;i++) {
    if (i==data.length-1) {
     known_x.push(i-1+lastPercentDone);
    } else {
     known_x.push(i);
    }
    known_y.push(getfn(data[i]));
  }
  return linearRegression.calculate(known_y, known_x);
 }
 
return linearRegression;
}();
