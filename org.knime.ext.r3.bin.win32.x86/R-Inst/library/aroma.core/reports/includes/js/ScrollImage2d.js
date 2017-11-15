/****************************************************************
 * ScrollImage2d()
 *
 * Author: Henrik Bengtson, hb@stat.berkeley.edu
 ****************************************************************/
function ScrollImage2d(id) {
  this.setImage = function(url) {
    var owner = this;

    /* Image loader to get the size of the (non-rescaled) image */
//    var myImage = new Image();
//    this.myImage = myImage;
//    myImage.onload = function() {
//      owner.imageWidth = this.width;
//      owner.imageHeight = this.height;
////alert(owner.imageWidth + " x " + owner.imageHeight);
//    }
//    myImage.src = url;

    this.image.onload = function() {
      owner.onLoad();
      owner.update();
      owner.imageWidth = this.width;
      owner.imageHeight = this.height;
    }

    /* Define onload() function */
    this.image.onerror = function() {
      /* alert('Image not loaded: ' + url); */
    }

    /* Start loading image */
    this.image.src = url;
  }

  this.getImageDimension = function() {
//alert(this.myImage.width + " x " + this.myImage.height);
    var res = Object();
//      alert(this.image.clientWidth);
    res.width = this.imageWidth;
    res.height = this.imageHeight;
    return(res);
  }

  this.getImageWidth = function() {
    return(this.image.width);
  }

  this.getImageHeight = function() {
    return(this.image.height);
  }

  this.getAspectRatio = function() {
    return (this.container.clientWidth / this.container.clientHeight);
  }

  this.update = function() {
    this.container.scrollLeft = this.x * this.container.scrollWidth;
    this.container.scrollTop = this.y * this.container.scrollHeight;
    var pos = findXY(this.image);
    this.xOffset = pos.x;
    this.yOffset = pos.y;
    var w = this.getImageWidth();
    var h = this.getImageHeight();
    this.image.style.left = Math.round(this.xOffset + w*this.x) + "px";
    this.image.style.top = Math.round(this.yOffset + h*this.y) + "px";
    this.image.width = this.width * this.container.clientWidth;
    var dim = this.getImageDimension();
    var aspect = this.width / this.height;
    this.image.height = aspect * this.height * this.container.clientWidth;
    /*
    this.container.style.height = winAspect*this.container.clientWidth;
    */
  }

  this.getXY = function() {
    var w = this.getImageWidth()/this.width;
    var h = this.getImageHeight()/this.height;
    var res = Object();
    res.x = Math.round(w*this.x);
    res.y = Math.round(w*this.y);
    return(res);
  }

  this.getDimension = function() {
    var res = Object();
    var dim = this.getImageDimension();
    res.width = Math.round(dim.width);
    res.height = Math.round(dim.height);
    return(res);
  }

  this.getRegion = function() {
    var xy = this.getXY();
    var dim = this.getDimension();
    var res = Object();
    res.x0 = xy.x;
    res.y0 = xy.y;
    res.x1 = xy.x + dim.width;
    res.y1 = xy.y + dim.height;
    return(res);
  }

  this.setRelXY = function(x,y) {
    x = Math.max(0, x);
    x = Math.min(x, 1);
    y = Math.max(0, y);
    y = Math.min(y, 1);
    this.x = x;
    this.y = y;
  }

  this.setSize = function(scale) {
    this.setRelDimension(scale, scale);
    this.update();
  }

  this.setRelDimension = function(width, height) {
    width = Math.max(0, width);
    height = Math.max(0, height);
    this.width = width;
    this.height = height;
  }

  this.setCursor = function(status) {
    this.container.style.cursor = status;
    this.image.style.cursor = status;
  }

  this.setupEventHandlers = function() {
    var owner = this;

    this.image.onmousedown = function() {
      var e = arguments[0] || event;
      var x0 = owner.x;
      var y0 = owner.y;
      var w = owner.getImageWidth();
      var h = owner.getImageHeight();
      var xStart = e.clientX;
      var yStart = e.clientY;
      var x = owner.container.scrollLeft + e.clientX;
      var y = owner.container.scrollTop + e.clientY;
      owner.setCursor("move");
      owner.image.onmousemove = null;
      owner.onmousedown();
  
      document.onmousemove = function() {
        var e = arguments[0] || event;
        var dx = (e.clientX - xStart)/w;
        var dy = (e.clientY - yStart)/h;
        owner.setRelXY(x0-dx, y0-dy);
        owner.update();
        owner.onmousemove();
        return false;
      }
  
      document.onmouseup = function() {
        document.onmousemove = null;
        owner.image.onmousemove = null;
        owner.setCursor("default");
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
  this.scale = 1;
  this.x = 0;
  this.y = 0;
  this.xOffset = 0;
  this.yOffset = 0;
  this.width = 1;
  this.height = 1;
  
  this.container = document.getElementById(id);
  this.image = document.getElementById(id + 'Image');
  this.imageWidth = 0;
  this.imageHeight = 0;

  this.setupEventHandlers();
} /* ScrollImage2d() */


/****************************************************************
 HISTORY:
 2012-02-02
 o KNOWN ISSUES: Internet Explorer fails to detect image size,
   i.e. it is always zero, even in onload() of the image.
 o Extracted ScrollImage2d.js.
 2008-06-23
 o Removed alert() for this.image.onerror(), because in 
   Firefox 3 these showed up although the image was loaded.
 2007-01-27
 o Created.
 ****************************************************************/
