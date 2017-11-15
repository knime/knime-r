/****************************************************************
 *
 ****************************************************************/
function findXY(obj) {
  var x = 0;
  var y = 0;

  if (obj.offsetParent)  {
    while (obj.offsetParent) {
      x += parseFloat(obj.offsetLeft);
      y += parseFloat(obj.offsetTop);
      obj = obj.offsetParent;
    }
  }  else if (obj.x) {
    x += obj.x;
    y += obj.y;
  }

  pos = new Object();
  pos.x = x;
  pos.y = y;
  return pos;
} /* findXY() */


function findViewport() {
  var e = window;
  var a = 'inner';
  if (!('innerWidth' in e)) {
    a = 'client';
    e = document.documentElement || document.body;
  }
  return {
    width:e[a+'Width'],
    height:e[a+'Height']
  };
} /* viewport() */

function updateText(obj, str) {
  obj.innerText = str;
  obj.innerHTML = str;
} /* updateText() */


function updateLabel(id, text) {
  var obj = document.getElementById(id);
  DOM_setInnerText(obj, text); /* From Webcuts.js */
}

// Array.indexOf( value, begin, strict ) - Return index of the first 
// element that matches value.
// Source: http://4umi.com/web/javascript/array.htm#indexof
Array.prototype.indexOf = function(v, b, s) {
  for(var kk=+b || 0, ll=this.length; kk < ll; kk++) {
    if(this[kk] === v || s && this[kk] == v)
      return(kk);
  }
  return(-1);
};


function toggleById(id) {
  var obj = document.getElementById(id);
  if (obj == null)
    alert("ERROR: No such element: " + id);
  obj.isSelected = !(obj.isSelected || false);
  if (obj.isSelected) {
    highlightById(id); 
  } else {
    clearById(id);  
  }
  return obj.isSelected;
}

function selectById(id) {
  var obj = document.getElementById(id);
  if (obj == null)
    return false;
  obj.isSelected = true;
  highlightById(id);
}

function unselectById(id) {
  var obj = document.getElementById(id);
  if (obj == null)
    return false;
  obj.isSelected = false;
  clearById(id);  
}

function clearById(id) {
  var obj = document.getElementById(id);
  if (obj == null)
    return false;
/*  
  obj.style.visibility = 'hidden'; 
  obj.style.borderBottom = 'none';
*/
  obj.style.background = 'none';
} /* clearById() */


function highlightById(id) {
  var obj = document.getElementById(id);
  if (obj == null)
    return false;
/*  
  obj.style.visibility = 'visible'; 
  obj.style.borderBottom = '2px solid black';
*/
  obj.style.background = '#ccccff';
} /* highlightById() */


function addToFavorites(url, title) { 
  if (window.sidebar) { // Mozilla Firefox Bookmark
		window.sidebar.addPanel(title, url, "");
	} else if (window.external) { // IE Favorite
		window.external.AddFavorite(url, title); 
  }	else if (window.opera && window.print) { // Opera Hotlist
		return true; 
  } else { 
    alert("Sorry! Your browser doesn't support adding bookmarks.");
  }
}


function padWidthZeros(x, width) {
  var str = "" + x;
  while (width - str.length > 0)
    str = "0" + str;
  return(str);
}

/****************************************************************
 HISTORY:
 2012-10-21
 o Added findViewport() adopted from http://goo.gl/PDVhF.
 2008-06-23
 o BUG FIX: highlightById() had a trailing semicolon in color.
 2007-02-20
 o Added padWidthZeros().
 2007-02-06
 o Moved addToFavorites() from ChromsomeExplorer.js to here.
 2007-01-27
 o Created.
 ****************************************************************/
