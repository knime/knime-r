/****************************************************************
 * Scrollbar2d()
 *
 * Author: Henrik Bengtsson, hb@stat.berkeley.edu
 ****************************************************************/
function Scrollbar2d(id) {
  this.setImage = function(url) {
    var owner = this;

    /* Define onload() function */
    this.image.onload = function() {
      owner.onLoad();
      owner.update();
    }

    /* Define onload() function */
    this.image.onerror = function() {
    /* alert('Image not loaded: ' + url); */
    }

    /* Start loading image */
    this.image.src = url;
  }

  this.getImageWidth = function() {
    return(this.image.width);
/*    return(this.image.width-5); */
  }

  this.getImageHeight = function() {
    return(this.image.height);
/*    return(this.image.height-4); */
  }

  this.update = function() {
    var pos = findXY(this.image);
    this.xOffset = pos.x;
    this.yOffset = pos.y;
    var w = this.getImageWidth();
    var h = this.getImageHeight();
    var bw = 2; /* Border width */
    this.marker.style.width = Math.round(w*this.width) + "px";
    this.marker.style.height = Math.round(h*this.height) + "px";
    this.marker.style.left = Math.round(this.xOffset + w*this.x - bw) + "px";
    this.marker.style.top = Math.round(this.yOffset + h*this.y - bw) + "px";
    this.marker.style.border = bw + 'px solid black';
  }

  this.getRegion = function() {
    var res = Object();
    res.x0 = this.x;
    res.y0 = this.y;
    res.x1 = this.x + this.width;
    res.y1 = this.y + this.height;
    return res;
  }

  this.setRelXY = function(x,y) {
    x = Math.max(0, x);
    x = Math.min(x, 1-this.width);
    y = Math.max(0, y);
    y = Math.min(y, 1-this.height);
    this.x = x;
    this.y = y;
  }

  this.setSize = function(scale) {
    this.setRelDimension(scale, scale);
  }

  this.setRelDimension = function(width, height) {
    var xMid = this.x + this.width/2;
    var yMid = this.y + this.height/2;
    width = Math.max(0, width);
    height = Math.max(0, height);
    width = Math.min(width, 1);
    height = Math.min(height, 1);
    this.width = width;
    this.height = height;
    this.setRelXY(xMid - this.width/2, yMid - this.height/2);
  }

  this.setCursor = function(status) {
    this.marker.style.cursor = status;
  /*
    this.container.style.cursor = status;
    this.image.style.cursor = status;
  */
  }

  this.setupEventHandlers = function() {
    var owner = this;

    var containerOnClick = function() {
      var w = owner.getImageWidth();
      var h = owner.getImageHeight();
      var e = arguments[0] || event;
      var pos = findXY(owner.marker);
      var dx = (e.clientX - pos.x)/w - owner.width;
      var dy = (e.clientY - pos.y)/h - owner.height;
      owner.setRelXY(dx,dy);
      owner.update();
    }

    /*
    this.container.onclick = containerOnClick;
    */

    this.marker.onmousedown = function() {
      var x0 = owner.x;
      var y0 = owner.y;
      var w = owner.getImageWidth();
      var h = owner.getImageHeight();
      var e = arguments[0] || event;
      var xStart = e.clientX;
      var yStart = e.clientY;
      owner.setCursor('move');
      owner.onmousedown();
  
      document.onmousemove = function() {
        var e = arguments[0] || event;
        var dx = (e.clientX - xStart)/w;
        var dy = (e.clientY - yStart)/h;
        owner.setRelXY(x0+dx, y0+dy);
        owner.update();
        owner.onmousemove();
        return false;
      }
  
      document.onmouseup = function() {
        document.onmousemove = null;
        owner.setCursor('default');
        owner.onmouseup();
        return false;
      }
  
      return false;
    }
  }

  this.onLoad = function() {}
  this.onmousedown = function() {}
  this.onmousemove = function() {}
  this.onmouseup = function() {}


  /* Initialize */
  this.x = 0;
  this.y = 0;
  this.xOffset = 0;
  this.yOffset = 0;
  this.width = 1;
  this.height = 1;
  
  this.container = document.getElementById(id);
  this.image = document.getElementById(id + 'Image');
  this.marker = document.getElementById(id + 'Marker');
  this.imageWidth = 0;
  this.imageHeight = 0;

  this.setupEventHandlers();
} /* Scrollbar2d() */


/****************************************************************
 HISTORY:
 2012-10-18
 o BUG FIX: Tried to assign a CSS style with syntax error,
   i.e. 'solid; black; 2px'. Also adjusting for the border width.
 2012-03-06
 o Update getImageWidth()/getImageHeight() to no longer
   subtract an ad hoc padding/inner margin.
 2012-02-02
 o Extracted Scrollbar2d.js.
 2008-06-23
 o Removed alert() for this.image.onerror(), because in 
   Firefox 3 these showed up although the image was loaded.
 2007-01-27
 o Created.
 ****************************************************************/
