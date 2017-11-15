/****************************************************************
 * ArrayExplorer()
 *
 * Author: Henrik Bengtsson, henrik.bengtsson@aroma-project.org
 ****************************************************************/
function ArrayExplorer() {
  /************************************************************************
   * Generic methods
   ************************************************************************/
  this.version = "3.5"

  this.error = function(msg) {
    console.log("ArrayExplorer v" + this.version + " ERROR: " + msg);
  }


  /************************************************************************
   * Methods for setting up chip types, samples, color maps & scales
   ************************************************************************/
  this.setChipTypes = function(chipTypes) {
    this.chipTypes = chipTypes;

    if (chipTypes.length > 1) {
      var s = 'Chip types: ';
      for (var kk=0; kk < chipTypes.length; kk++) {
        var chipType = chipTypes[kk];
        s = s + '[<span id="chipType' + chipType + '"><a href="javascript:explorer.setChipType(\'' + chipType + '\');">' + chipType + '</a></span>]'; 
      }
      s = s + '<br>';
      updateLabel('chipTypeLabel', s);
    }
  }

  this.setSamples = function(samples) {
    this.samples = samples;
    if (samples.length > 1) {
      var s = 'Samples: ';
      for (var kk=0; kk < samples.length; kk++) {
        var sample = samples[kk];
        var name = sample;
        if (this.sampleAliases != null)
          name = this.sampleAliases[kk];
        s = s + '[<span id="sample' + sample + '"><a href="javascript:explorer.setSample(\'' + sample + '\');">' + name + '</a></span>]<span style="font-size:1%"> </span>';
      }
      s = s + ' ';
      updateLabel('samplesLabel', s);

      var sample = this.sample;
      this.sample = null;
      this.setSample(sample);
    }
  }

  this.setSampleAliases = function(aliases) {
    this.sampleAliases = aliases;
  }


  this.setColorMaps = function(colorMaps) {
    this.colorMaps = colorMaps;

    if (colorMaps.length > 0) {
      var s = 'Color map: ';
      for (var kk=0; kk < colorMaps.length; kk++) {
        var colorMap = colorMaps[kk];
        var name = colorMap;
        if (this.colorMapAliases != null)
          name = this.colorMapAliases[kk];
        s = s + '[<span id="colorMap' + colorMap + '"><a href="javascript:explorer.setColorMap(\'' + colorMap + '\');">' + name + '</a></span>]'; 
      }
      s = s + '<br>';
      updateLabel('colorMapLabel', s);

      var colorMap = this.colorMap;
      this.colorMap = null;
      this.setColorMap(colorMap);
    }
  }

  this.setColorMapAliases = function(aliases) {
    this.colorMapAliases = aliases;
  }


  this.setScales = function(scales) {
    function padWidthZeros(x, width) {
      var str = "" + x;
      while (width - str.length > 0)
        str = "0" + str;
      return(str);
    }
   
    this.scales = scales;
    var zWidth = Math.round(Math.log(Math.max(scales)) / Math.log(10) + 0.5);
    var s = 'Zoom: ';
    for (var kk=0; kk < scales.length; kk++) {
      var scale = scales[kk];
      s = s + '[<span id="zoom' + scale + '"><a href="javascript:explorer.setScale(' + scale + ');">x' + padWidthZeros(scale, zWidth) + '</a></span>]'; 
    }
    s = s + '<br>';
    updateLabel('zoomLabel', s);

    var scale = this.scale;
    this.scale = null;
    this.setScale(scale);
  }


  /************************************************************************
   * Methods for updating the display
   ************************************************************************/
  this.showIndicator = function(state) {
    var statusImage = document.getElementById('statusImage');
    if (state) {
      statusImage.style.visibility = 'visible';
    } else {
      statusImage.style.visibility = 'hidden';
    }
  }

  this.setStatus = function(state) {
    if (state == "") {
      this.showIndicator(false);
      this.image2d.image.style.filter = "alpha(opacity=100)";
      this.image2d.image.style.opacity = 1.0;
    } else if (state == "wait") {
      this.showIndicator(true);
      this.image2d.image.style.filter = "alpha(opacity=50)";
      this.image2d.image.style.opacity = 0.50;
    }
  }

  this.getImageUrl = function() {
    if (this.sample == "" || this.colorMap == "" || this.chipType == "") {
      return(null);
    }
    var imgName = this.sample + "," + this.colorMap + ".png";
    var pathname = this.chipType + '/spatial/' + imgName;
    return(pathname);
  }

  this.updateImage = function() {
    var pathname = this.getImageUrl();
    if (pathname == null) {
      return(false);
    }
    this.loadCount = 2;
    this.setStatus('wait');
    this.nav2d.setImage(pathname);
    this.image2d.setImage(pathname);

    /* Update the title of the page */
    var title = location.href;
    title = title.substring(0, title.lastIndexOf('\/'));
    title = title.substring(title.lastIndexOf('\/')+1);
    title = title + '/' + pathname;
    document.title = title;
  }

  this.decreaseLoadCount = function() {
    this.loadCount = this.loadCount - 1;
    if (this.loadCount <= 0) {
      this.loadCount = 0;
      this.setStatus("");
    }
  }

  /************************************************************************
   * Methods for changing chip type, sample, color map & scale
   ************************************************************************/
  this.setChipType = function(chipType) {
    if (this.chipType != chipType) {
      // Update list of available samples, color maps and zooms
      this.onSetChipType(chipType);

      clearById('chipType' + this.chipType);
      highlightById('chipType' + chipType);
      this.chipType = chipType;
      this.updateImage();
    }
  }

  this.setSample = function(sample) {
    if (this.sample != sample) {
      clearById('sample' + this.sample);
      highlightById('sample' + sample);

      this.sample = sample;

      var pos = sample.indexOf(',');
      var tags = "";
      if (pos != -1) {
        tags = sample.substring(pos+1);
        sample = sample.substring(0, pos);
      }
      updateLabel('sampleLabel', sample);
      updateLabel('sampleTags', tags);
      this.updateImage();
    }
  }

  this.setColorMap = function(map) {
    if (this.colorMap != map) {
      clearById('colorMap' + this.colorMap);
      highlightById('colorMap' + map);
      this.colorMap = map;
      this.updateImage();
    }
  }

  this.setScale = function(scale) {
    if (this.scale != scale) {
      clearById('zoom' + this.scale);
      this.scale = scale;
      var ar = this.image2d.getAspectRatio();
      this.nav2d.setRelDimension(1/scale, 1/scale/ar);
      this.image2d.setRelDimension(scale, scale);
      this.nav2d.update();
      this.image2d.update();
      highlightById('zoom' + scale);
    }
  }


  /************************************************************************
   * Misc.
   ************************************************************************/
  this.getYOfImage2d = function() {
    var y = findXY(this.image2d.image).y;

    /* Sanity check */
    if (isNaN(y)) {
      this.error("Failed to infer 'y' of 'image2d': " + y);
    }

    return(y);
  }

  this.getClientHeight = function() {
    var vp = findViewport();
    var dh = vp.height;
    /* Sanity check */
    if (isNaN(dh)) {
      this.error("Failed to infer 'height' of client: " + dh);
    }
    return(dh);
  }



  /************************************************************************
   * Main
   ************************************************************************/
  this.samples = new Array();
  this.sampleAliases = null;
  this.chipTypes = new Array();
  this.colorMaps = new Array();
  this.colorMapAliases = null;
  this.scales = new Array();

  this.loadCount = 0;
  this.scale = 0;
  this.sample = '';
  this.chipType = '';
  this.colorMap = '';

  this.setupEventHandlers = function() {
    var owner = this;

    this.nav2d.onLoad = function() {
      owner.decreaseLoadCount();
      owner.updateInfo();
    }

    this.image2d.onLoad = function() {
      owner.decreaseLoadCount();
      owner.updateInfo();
    }

    this.getRegion = function() {
      var r = this.nav2d.getRegion();
      var w = this.image2d.imageWidth;
      var h = this.image2d.imageHeight;
      var res = new Object();
      res.x0 = Math.round(w*r.x0);
      res.y0 = Math.round(w*r.y0);
      res.x1 = Math.round(w*r.x1)-1;
      res.y1 = Math.round(w*r.y1)-1;
      return(res);
    }

    this.image2d.onmousedown = this.nav2d.onmousedown = function() {
      var info = document.getElementById('image2dInfoTL');
      info.style.visibility = 'visible';
      info = document.getElementById('image2dInfoBR');
      info.style.visibility = 'visible';
    }

    this.image2d.onmouseup = this.nav2d.onmouseup = function() {
      var info = document.getElementById('image2dInfoTL');
      info.style.visibility = 'hidden';
      info = document.getElementById('image2dInfoBR');
      info.style.visibility = 'hidden';
    }

    this.nav2d.onmousemove = function() {
      owner.image2d.setRelXY(this.x, this.y);
      owner.image2d.update();
      owner.updateInfo();
      return(false);
    }

    this.image2d.onmousemove = function() {
      owner.nav2d.setRelXY(this.x, this.y);
      owner.nav2d.update();
      owner.updateInfo();
      return(false);
    }
  }

  this.updateInfo = function() {
    var r = this.getRegion();
    var s = '('+r.x0+','+r.y0+')';
    updateLabel('image2dInfoTL', s);
    var s = '('+r.x1+','+r.y1+')';
    updateLabel('image2dInfoBR', s);
    var infoBR = document.getElementById('image2dInfoBR');
    var lh = infoBR.offsetHeight;
    var lw = infoBR.offsetWidth;
    var xy = findXY(this.image2d.container);
    infoBR.style.left = xy.x+this.image2d.container.clientWidth-lw;
    infoBR.style.top = xy.y+this.image2d.container.clientHeight-lh;
  }

  this.update = function() {
    var y = this.getYOfImage2d();
    var dh = this.getClientHeight();
    var h = dh - y - 16;
    /* Sanity check */
    if (h < 0) {
      this.error("Trying to set 'height' of 'image2d' to a negative value: " + h + " (=dh-y-16=" + dh + "-" + y + "-16)");
    }
    this.image2d.container.style.height = h + 'px';
    var ar = this.image2d.getAspectRatio();
    this.nav2d.setRelDimension(1/this.scale, 1/this.scale/ar);
    this.updateImage();
    this.image2d.update();
    this.nav2d.update();
    this.updateInfo();
  }

  this.onLoad = function() { }

  this.start = function() {
    this.nav2d = new Scrollbar2d("nav2d");
    this.image2d = new ScrollImage2d("image2d");
    this.setupEventHandlers();

    /* Default settings */
    this.setScales(new Array('0.5', '1', '2', '4', '8', '16', '32'));
    this.setColorMaps(new Array('gray'));

    var y = this.getYOfImage2d();
    var dh = this.getClientHeight();
    var h = (dh - y - 12);
    /* Sanity check */
    if (h < 0) {
      this.error("Trying to set 'height' of 'image2d' to a negative value: " + h + " (=dh-y-12=" + dh + "-" + y + "-12)");
    }
    this.image2d.container.style.height = h + "px";

    this.setChipType(this.chipTypes[0]);
    this.setSample(this.samples[0]);
    this.setScale(this.scales[0]);
    this.setColorMap(this.colorMaps[0]);

    this.update();
  } // start()
} /* ArrayExplorer() */

/****************************************************************
 HISTORY:
 2012-10-21
 o Now utilizing new findViewport() to get the height (and
   width) of the browser window.
 2012-10-18
 o ROBUSTNESS: Now ArrayExplorer asserts that inferred height
   of 'image2d' and height of the client are valid.  If not,
   an informative alert() error is reported.
 2012-03-06
 o Now update() also updates the navigator, which may be needed
   if the windows was resized.
 o Now ArrayExplorer v3.4 works with at least Chrome 18, 
   Firefox 10, Internet Explorer 9, and Opera 11.61.
 o BUG FIX: Now update() uses 'window.innerHeight' instead of
   'document.body.clientHeight' to infer the maximum height
   the loaded image should have in order to fill to the bottom.
 o ROBUSTIFICATION: updateImage() no longer tries to load
   non-existing image if getImageUrl() returns 'null'.
 o ROBUSTIFICATION: Now getImageUrl() returns 'null' if one
   of 'sample', 'chipType' and 'colorMap' is not set.
 2007-08-09
 o Now setChipType() calls onChipType() too.
 2007-03-19
 o Now the sample tags are written to their own label.
 2007-02-06
 o Updated to <rootPath>/<dataSet>/<tags>/<chipType>/<set>/.
 2007-01-27
 o Created.
 ****************************************************************/
