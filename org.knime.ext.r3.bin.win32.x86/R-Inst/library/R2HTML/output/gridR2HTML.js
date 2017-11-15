/*
  Javascript functions for R2HTML grid export functions
  Eric Lecoutre 
  09/11/2004

  -  colorNA
     Used at the moment to flag NA values in red
     Later, we will implement proper templates for classes "numeric" "factor" 
     and so on which will include this color formatting
  
  -  ReadExternalData 
     Will read an external TAB delimited file and use it for the grid data
     Data should include column header in the first row.
     Used by R function HTMLgrid
  
  -  ReadInlineData
     Will set up a grid using data provided direclty within the HTML
     as a javascript array. 
*/



function colorNA(){
    var value = this.getItemProperty("value");
    return ((value == "NaN")|| isNaN(value)) ? "red" : " ";
 }    



// var R-numeric = new 


function ReadExternalData(URLfic,classes,asDF,ID,showimages){
	// create ActiveWidgets data model - text-based table
	var table = new Active.Text.Table;
	// provide data URL - plain text comma-separated file
	table.setURL(URLfic);
	var Datalen = 0;
	var myData =[];
	var TestmyData =[];
	var myColumns =[];
	var CloneColumns =[];
	var CloneData = [];
	var ColSelected =[];
	var NumColSel = 0;
	var Xsentence ="";
	var maxcol = 150;
	var lastcol = 0;
	var ColLength = [];
  var obj = new Active.Controls.Grid;
	obj.setId(ID);

  var stylesheet = document.styleSheets[document.styleSheets.length-1];
	obj.setStatusProperty("code", "loading"); 
	var defaultResponse = table.response;
	table.response = function(data){
	defaultResponse.call(table, data);
	Datalen=table.getCount();
//  alert("***"+ classes[0]);

	// load second CSV line (max 150 cols) to calculate last not empty field
	var x=1;
	var y=0;
	Xsentence = 'TestmyData.push([table.getText( ' + x + ', ' + y + ')';
	y=1; 
	for(y=1; y< maxcol; y++) { Xsentence += ', table.getText( ' + x + ', ' + y + ')'; }
	Xsentence += '])';
	eval(Xsentence);

	// calculate ths CSV file last data column 
	for(var x=0; x< maxcol; x++) { if(TestmyData[0][x]!=table.getText(0, x)) {lastcol = x+1; } }
	//alert(lastcol);

	TestmyData = [];

	NumColSel = lastcol;
	for(var x=0; x< lastcol; x++) { ColSelected.push([x]); }
	LoadData();
	function LoadData() {
		// load first CSV line into array myColumns (note: remove next line -- if myColumns load as array 
		// take header size also
		for(var x=0; x< lastcol; x++) { 
			myColumns.push([table.getText( 0, x)]); 
			ColLength.push((myColumns[x])[0].length);
			}

		CloneColumns = myColumns ; 

		// load CSV data lines 2 to bottom into array myData 
		var x=1; // x=0 -- if myColumns load as array or in a separate CSV file 
		var y=0;

		while( x< Datalen) {
			Xsentence = 'myData.push([table.getText( ' + x + ', ' + y + ')';
			var tmpsize = table.getText(x,y).length;
			if (tmpsize> ColLength[y]) ColLength[y]= tmpsize

			y=1; 
			for(y=1; y< lastcol; y++) { 
				Xsentence += ', table.getText( ' + x + ', ' + y + ')'; 
				var tmpsize = table.getText(x,y).length;
				if (tmpsize> ColLength[y]) ColLength[y]= tmpsize
				}
			Xsentence += '])';
			eval(Xsentence);
		y=0;
		x++; }

		// clone the arrays and use tem as default
		CloneColumns = myColumns ; 
		CloneData = myData ;

		// load data javascript objects
		obj.setRowProperty("count", Datalen-1);
		obj.setColumnProperty("count", NumColSel);
		obj.setDataProperty("text", function(i, j){return CloneData[i][j]});
//		obj.setColumnProperty("text", function(i){return CloneColumns[i]});

			if (showimages==1) {
    obj.setColumnProperty("text", function(i){
    var tmp="<img src='"+classes[i]+".gif'>"+myColumns[i]
    return tmp});
 }
 else {
    obj.setColumnProperty("text", function(i){return myColumns[i]});
 }

    // write good CSS to "auto-size" the grid


		
 

		var TableWidth=0;
		for(var x=0; x <lastcol;x++){
			var colWidth = 20+(ColLength[x]*7);
      if (showimages==1) colWidth=colWidth+8;
      CloneColumns[x]<- "<img src='"+ classes[x] +".gif'>"+ CloneColumns[x]
      if (asDF==1){
        // add styles depending on classes of the column
        if (classes[x]=="numeric")
        {
           // obj.getColumnTemplate(x).setStyle("text-align", "right");
           
           	stylesheet.addRule("#"+ID+ " .active-column-"+x,"text-align: right");
//      alert(obj.getTopTemplate(x));
//            obj.getTopTemplate(x).setStyle("background", "numeric.gif");
  
              //alert(colorNA());
            obj.getColumnTemplate(x).setStyle("color",colorNA);
        }
        else
        {
         //   obj.setTemplate("column", factor, x);
        }
     }

			stylesheet.addRule("#"+ID+ " .active-column-"+x,"width:"+colWidth+"px");
			stylesheet.addRule("#"+ID,"font-size: 79%");
			stylesheet.addRule("#"+ID,"font-family: 'verdana','helvetica','sans-serif'");
			stylesheet.addRule("#"+ID,"border-bottom: 1px solid threedlightshadow");
      			
    //	font-family: "Verdana", "Helvetica", "sans-serif";
 			TableWidth += colWidth;
		}
		//TableWidth += 1;

		stylesheet.addRule("#"+ID, "width:"+TableWidth+"px");
		var TableHeight=(40+(Datalen-1)*18);
  		stylesheet.addRule("#"+ID, "height:"+TableHeight+"px");
	

  
  }
	// let the browser paint the grid
	window.setTimeout(function(){
	obj.setStatusProperty("code", "");
	obj.refresh();
	}, 0);
	}

	// writes obj / One could also imagine returning directly obj for more operations on it...
	obj.setRowHeaderWidth("0px");
	obj.setColumnHeaderHeight("27px");
   // Multiselect ON
   obj.setProperty("selection/multiple", true);

	
	// Add roll-over effect
    var row = new Active.Templates.Row;
  row.setEvent("onmouseover", "mouseover(this, 'active-row-highlight')");
  row.setEvent("onmouseout", "mouseout(this, 'active-row-highlight')");
  obj.setTemplate("row", row);
	// start asyncronous data retrieval
	table.request();

//	alert("ID:"+obj.getId()+ "   " + obj);
	document.write(obj);
}




function ReadInlineData(data,columns,nrow,ncol,classes,asDF,obj,showimages){
  	//	create ActiveWidgets Grid javascript object
    var stylesheet = document.styleSheets[document.styleSheets.length-1];
    var ID= obj.getId()
  	//	set number of rows/columns
  	obj.setRowProperty("count", nrow);
  	obj.setColumnProperty("count", ncol);
  	//	provide cells and headers text
  	 obj.setDataProperty("text", function(i, j){return data[i][j]});
	 if (showimages==1) {
    obj.setColumnProperty("text", function(i){
    var tmp="<img src='"+classes[i]+".gif'>"+columns[i]
    return tmp});
   }
  else {
    obj.setColumnProperty("text", function(i){return columns[i]});
  }

  	var ColLength = [];

  // Check columns content length
		for(var x=0; x< ncol; x++) { ColLength.push((columns[x]).length);}
		var x=0; 
		var y=0;
		while( x< nrow) {
			for(y=0; y< ncol; y++) { 
  	 	 	var tmpsize =  (data[x][y]).length;
	     		if (tmpsize> ColLength[y]) ColLength[y]= tmpsize
				}
  		y=0;
		x++; }

		// write good CSS to "auto-size" the grid
		var TableWidth=0;
		for(var x=0; x <ncol;x++){
			var colWidth = 20+(ColLength[x]*7);
      if (showimages==1) colWidth=colWidth+8;
      if (asDF==1){
        // add styles depending on classes of the column
        if (classes[x]=="numeric")
        {
            // obj.getColumnTemplate(x).setStyle("text-align", "right");
            stylesheet.addRule("#"+ID+ " .active-column-"+x,"text-align: right");
            obj.getColumnTemplate(x).setStyle("color",colorNA);
        }
        else
        {
         //   obj.setTemplate("column", factor, x);
        }
     }
			stylesheet.addRule("#"+ID+ " .active-column-"+x,"width:"+colWidth+"px");
			stylesheet.addRule("#"+ID,"font-size: 79%");
			stylesheet.addRule("#"+ID,"font-family: 'verdana','helvetica','sans-serif'");
			stylesheet.addRule("#"+ID,"border-bottom: 1px solid threedlightshadow");
 			TableWidth += colWidth;
		}

		stylesheet.addRule("#"+ID, "width:"+TableWidth+"px");
		var TableHeight=(40+(nrow)*18);
    stylesheet.addRule("#"+ID, "height:"+TableHeight+"px");

    obj.setRowHeaderWidth("0px");
	  obj.setColumnHeaderHeight("27px");
      // Multiselect ON
    obj.setProperty("selection/multiple", true);

  	// Add roll-over effect
      var row = new Active.Templates.Row;
      row.setEvent("onmouseover", "mouseover(this, 'active-row-highlight')");
      row.setEvent("onmouseout", "mouseout(this, 'active-row-highlight')");
      obj.setTemplate("row", row);
    	// writes obj / One could also imagine returning directly obj for more operations on it...   
    	document.write(obj);
}


