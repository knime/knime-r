// alert("ChromosomeExplorer...");
/****************************************************************
 * ChromosomeExplorer()
 *
 * Author: Henrik Bengtsson, henrik.bengtsson@aroma-project.org
 ****************************************************************/
function ChromosomeExplorer() {
  /************************************************************************
   * Generic methods
   ************************************************************************/
  this.version = "3.5"

  this.error = function(msg) {
      console.log("ChromosomeExplorer v" + this.version + " ERROR: " + msg);
  }


  var chromosomes = new Array('01','02','03','04','05','06','07','08','09','10','11','12','13','14','15','16','17','18','19','20','21','22','X');
  var chipTypes = null;

  var nav = null;
  var navArea = null;
  var navAreaWidth = 0;
  var navAreaX = 0;
  var navImage = null;
  var navImageOffsetX = 0;
  var navImageWidth = 0;
  var navigatorZoom = -1;

  var panel = null;
  var panelX = 0;
  var panelImage = null;
  var panelImageOnLoad = function() {};
  var panelImageWidth = 0;
  var panelImageOffsetX = 0;
  var panelLocator = null;
  var panelLocatorTag = null;
  var panelWidth = 0;
  var panelMaxWidth = 0;

  var playSamples = false;
  var playDelay = 2000;

//  alert("ChromosomeExplorer()...");
  /************************************************************************
   * Methods for setting up chip types, samples, zooms, and sets
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
        s = s + '[<span id="sample' + sample + '"><a href="javascript:explorer.setSample(' + kk + ');">' + name + '</a></span>]<span style="font-size:1%"> </span>';
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
    this.setSamples(this.samples);
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
      s = s + '[<span id="zoom' + scale + '"><a href="javascript:explorer.setScale(' + kk + ');">x' + padWidthZeros(scale, zWidth) + '</a></span>]'; 
    }
    updateLabel('zoomLabel', s);

    var scale = this.scale;
    this.scale = null;
    this.setScale(scale);
  }


  this.setSets = function(sets) {
    var s = 'Sets: ';
    for (var kk=0; kk < sets.length; kk++) {
      var set = sets[kk];
      s = s + '[<span id="set' + set + '"><a href="javascript:explorer.setSet(' + kk + ');">' + set + '</a></span>]'; 
    }
    updateLabel('setLabel', s);

    var set = this.set;
    this.sets = sets;
    this.set = null;
    this.setSet(set);
  }


  this.setChromosomes = function(chromosomes) {
    this.chromosomes = chromosomes;

    if (chromosomes.length > 1) {
      var s = '';
      for (var kk=0; kk < chromosomes.length; kk++) {
        var chr = chromosomes[kk];
        var chrLabel = chr;
        if (chr == 'X') {
          chrLabel = "23(X)";
        } else if (chr == 'Y') {
          chrLabel = "24(Y)";
        }
        if (chrLabel != '') {
            s = s + '[<span id="chromosome' + chr + '"><a href="javascript:explorer.setChromosome(' + kk + ');">' + chrLabel + '</a></span>]';
        }
      }
      s = s + '<br>';
      updateLabel('chromosomes', s);
    }
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
    navImage = document.getElementById('navigatorImage');
    panelImage = document.getElementById('panelImage');
    if (state == "") {
      this.showIndicator(false);
      navImage.style.filter = "alpha(opacity=50)";
      navImage.style.opacity = 0.50;
      panelImage.style.filter = "alpha(opacity=100)";
      panelImage.style.opacity = 1.0;
      this.updateInfo();
    } else if (state == "wait") {
      this.showIndicator(true);
      navImage.style.filter = "alpha(opacity=20)";
      navImage.style.opacity = 0.20;
      panelImage.style.filter = "alpha(opacity=50)";
      panelImage.style.opacity = 0.50;
    }
  }

  this.updateInfo = function() {
    updateLabel('chromosomeLabel', this.chromosomes[this.chromosomeIdx]);

    var label = this.samples[this.sampleIdx];
    var tags = null;
    if (this.sampleAliases != null) {
      if (this.sampleAliases[this.sampleIdx] != label) {
        tags = label;
        label = this.sampleAliases[this.sampleIdx];
      }
    }
    updateLabel('sampleLabel', label);

    if (tags != null) {
      updateLabel('sampleTags', '(' + tags + ')');
    }
  }


  /************************************************************************
   * Methods for changing chip type, sample, set & zoom
   ************************************************************************/ 
  this.setChipType = function(idx) {
    if (idx == null)
      idx = 0;
    if (this.chipTypeIdx != idx) {
      clearById('chipType' + this.chipType);
      this.loadCount = 2;
      this.setStatus('wait');
      this.chipTypeIdx = idx;
      this.chipType = this.chipTypes[this.chipTypeIdx];
      highlightById('chipType' + this.chipType);
      this.updatePanel();
      this.updateNavigator();
    }
  }

  this.setSet = function(idx) {
    if (idx == null)
      idx = 0;
    if (this.setIdx != idx) {
      clearById('set' + this.set);
      this.loadCount = 2;
      this.setStatus('wait');
      this.setIdx = idx;
      this.set = this.sets[this.setIdx];
      highlightById('set' + this.set);
      this.updatePanel();
      this.updateNavigator();
    }
  }

  this.setChromosome = function(idx) {
    if (idx == null)
      idx = 0;
    if (this.chromosomeIdx != idx) {
      clearById('chromosome' + this.chromosome);
      this.loadCount = 2;
      this.setStatus('wait');
      this.chromosomeIdx = idx;
      this.chromosome = this.chromosomes[this.chromosomeIdx];
      highlightById('chromosome' + this.chromosome);
      this.updatePanel();
      this.updateNavigator();
    }
  }

  this.setScale = function(idx) {
    if (idx == null)
      idx = 0;

    this.scaleIdx = idx;
    s = this.scales[idx];
    if (this.scale != s) {
      clearById('zoom' + this.scale);
      this.loadCount = 1;
      this.setStatus('wait');
      this.scale = s;
      highlightById('zoom' + this.scale);
      this.updatePanel();
    }
  }

  this.setSample = function(idx) {
    if (idx == null)
      idx = 0;
    if (this.sampleIdx != idx) {
      clearById('sample' + this.sample);
      this.loadCount = 2;
      this.setStatus('wait');
      this.sampleIdx = idx;
      this.sample = this.samples[this.sampleIdx];
      highlightById('sample' + this.sample);
      this.updatePanel();
      this.updateNavigator();
    }
  }
  
  this.getImagePathname = function(chipType, sample, chromosome, zoom, set) {
    imgName = sample + ",chr" + padWidthZeros(chromosome, 2) + ",x" + padWidthZeros(zoom, 4) + ".png";
    var pathname = chipType + '/' + set + '/' + imgName;
    return(pathname);
  }

  this.resetPositions = function() {
    navAreaX = 0;
    panel.scrollLeft = 0;
    this.updateGlobals();
  }

  this.update = function() {
    this.updateGlobals();
		this.updatePanel();
  	this.updateNavigator();
		this.updateNavigatorWidth();
  }

  this.updateGlobals = function() {
    panelWidth = panel.clientWidth;
    panelMaxWidth = panel.scrollWidth;
    navImageOffsetX = findXY(navImage).x;
    panelImageWidth = panelImage.clientWidth;
    panelImageOffsetX = findXY(panelImage).x;
    navImageWidth = navImage.clientWidth;
    navAreaWidth = navImageWidth * (panelWidth / panelMaxWidth);
  }

  this.locatorUpdated = function() {
    /* Update locator tag */
    var pixelsPerMb = 3; /* /1.0014; */
    var xPx = findXY(panelLocator).x - findXY(panelImage).x + parseFloat(panel.scrollLeft);
    var xMb = (xPx-50)/(this.scale*pixelsPerMb);
    var tag = Math.round(100*xMb)/100 + 'Mb';
    updateText(panelLocatorTag, tag);
  
    var url;
  
    /* Update shortcut link */
    if (this.bookmarkUrl != null) {
      var args = "'" + this.chipType + "', '" + this.sample + "', '" + this.chromosome + "', " + panel.scrollLeft + ", " + this.scale + ", " + this.set;
      url = 'javascript:explorer.jumpTo(' + args + ');';
      url = 'x:"' + args + '",';
    	/* url = 'javascript:addToFavorites("' + url + '", "sss")';	*/
      this.bookmarkUrl.href = url;
      updateText(this.bookmarkUrl, url);
    }
  
    /* Update CNR link */
    if (this.cnrUrl != null) {
      url = this.chipType + '/' + this.set + '/' + 'regions.xls';
      this.cnrUrl.href = url;
      updateText(this.cnrUrl, url);
    }
  }

  this.jumpTo = function(newChipType, newSample, newChromosome, newPanelOffset, newZoom, set) {
    /* Chip type */
    if (this.chipType != '') {
      clearById('chipType' + this.chipType);
      this.chipTypeIdx = this.chipTypes.indexOf(newChipType);
      this.chipType = this.chipTypes[this.chipTypeIdx];
      highlightById('chipType' + this.chipType);
    }
  
    /* Sample */
    clearById('sample' + this.sample);
    this.sampleIdx = this.samples.indexOf(newSample);
    this.sample = this.samples[this.sampleIdx];
    highlightById('sample' + this.sample);
  
    /* Chromosome */
    clearById('chromosome' + this.chromosome);
    this.chromosomeIdx = this.chromosomes.indexOf(newChromosome);
    this.chromosome = this.chromosomes[this.chromosomeIdx];
    highlightById('chromosome' + this.chromosome);

    /* Set */
    clearById('set' + this.set);
    this.setIdx = this.sets.indexOf(newSet);
    this.set = this.sets[this.setIdx];
    highlightById('set' + this.set);
  
    /* Zoom */
    clearById('zoom' + this.scale);
    this.scaleIdx = -1;
    var kk = 0;
    while (scaleIdx == -1 && kk < this.scales.length) {
      if (this.scales[kk] == newZoom)
        this.scaleIdx = kk;
      kk = kk + 1;
    }
    this.scale = this.scales[this.scaleIdx];
    highlightById('zoom' + this.scale);
  
    /* When image is loaded... */
    panelImageOnLoad = function() {
      panel.scrollLeft = newPanelOffset;
      this.panelUpdated();
    }
  
    this.updatePanel();
    this.updateNavigator();
  }

  this.setGlobalCursor = function(status) {
    panel.style.cursor = status;
    panelImage.style.cursor = status;
    nav.style.cursor = status;
    navImage.style.cursor = status;
    navArea.style.cursor = status;
  }
  

  /************************************************************************
   * Main
   ************************************************************************/ 
  this.loadCount = 0;
  this.imageUrl = null;
  this.bookmarkUrl = null;
  this.cnrUrl = null;

  this.scale = -1;

  this.chromosomes = new Array('01','02','03','04','05','06','07','08','09','10','11','12','13','14','15','16','17','18','19','20','21','22','X','Y');
  this.samples = new Array();
  this.sampleAliases = null;
  this.chipTypes = null;
  this.scales = new Array(1);
  this.sets = new Array();

  this.chromosome = null;
  this.chromosomeIdx = 22;
  this.sample = null;
  this.sampleIdx = 0;
  this.chipType = null;
  this.chipTypeIdx = 0;
  this.set = null;
  this.setIdx = 0;
  this.scaleIdx = 0;


  this.setupEventHandlers = function() {
    var owner = this;

    /*******************************************************
     * chromosomePanel
     *******************************************************/
    panel = document.getElementById('panel');
    panelLocator = document.getElementById('panelLocator');
    panelLocatorTag = document.getElementById('panelLocatorTag');
  
    panelOnScroll = function() {
      relOffset = panel.scrollLeft / panelMaxWidth;
      navAreaX = relOffset * navImageWidth;
      owner.navAreaUpdate();
    }
  
    panel.onscroll = panelOnScroll;
  
    panelImage = document.getElementById('panelImage');
  
    var panelLocatorIsLocked = true;
  
    panelImage.ondblclick = function() {
      panelLocatorIsLocked = false;
      panelImage.onmousedown = null;
      var e = arguments[0] || event;
      mouseX = e.clientX;
      panelLocator.style.left = (mouseX-2) + "px";
      owner.locatorUpdated();
    }
    panelLocator.ondblclick = panelImage.ondblclick;
  
    panelImage.onclick = function() {
      panelLocatorIsLocked = true;
      panelImage.onmousedown = panelImageOnMouseDown;
    }
    panelLocator.onclick = panelImage.onclick;
  
    panelImageOnMouseMove = function() {
      if (!panelLocatorIsLocked) {
        var e = arguments[0] || event;
        mouseX = e.clientX;
        panelLocator.style.left = (mouseX-2) + "px";
        owner.locatorUpdated();
      }
      return false;
    }
  
    panelImage.onmousemove = panelImageOnMouseMove;
  
    panelImageOnMouseDown = function() {
      var e = arguments[0] || event;
      var x = panel.scrollLeft + e.clientX;
      owner.setGlobalCursor("move");
      panel.onscroll = null;
      panelImage.onmousemove = null;
  
      document.onmousemove = function() {
        var e = arguments[0] || event;
        isMoving = true;
        panel.scrollLeft = x - e.clientX;
        owner.panelUpdated();
        return false;
      }
  
      document.onmouseup = function() {
        document.onmousemove = null;
        panel.onscroll = panelOnScroll;
        panelImage.onmousemove = panelImageOnMouseMove;
  //      panelLocatorIsLocked = false;
        owner.setGlobalCursor("default");
        return false;
      }
  
      return false;
    }
  
    panelImage.onmousedown = panelImageOnMouseDown;

    /*******************************************************
     * chromosomeNavigator
     *******************************************************/
    nav = document.getElementById('navigator');
    navImage = document.getElementById('navigatorImage');
    navArea = document.getElementById('navigatorArea');
    var mouseX = 0;
    var mouseDown = false;

    /* Immitate onmousepress, which does not exists */
    navImage.onmousepress = function() {
      if (mouseDown) {
        if (mouseX < navAreaX) {
          owner.navAreaMove(navAreaX - 0.47*navAreaWidth);
        } else if (mouseX > navAreaX + 1*navAreaWidth) {
          owner.navAreaMove(navAreaX + 1.47*navAreaWidth);
        } else {
          owner.navAreaMove(mouseX);
        }
        setTimeout('navImage.onmousepress();', 100);
      }
      return false;
    }
  
    navImage.onmousedown = function() {
      var e = arguments[0] || event;
      mouseDown = true;
      owner.setGlobalCursor("move");
      owner.updateGlobals();
      mouseX = (e.clientX - navImageOffsetX);
      if (mouseX < navAreaX) {
        owner.navAreaMove(navAreaX - 0.47*navAreaWidth);
        } else if (mouseX > navAreaX + 1*navAreaWidth) {
        owner.navAreaMove(navAreaX + 1.47*navAreaWidth);
      }
  
      setTimeout('navImage.onmousepress();', 500);
  
      document.onmouseup = function() {
        navImage.onmousemove = null;
        mouseDown = false;
        owner.setGlobalCursor("default");
        return false;
      }
  
      navImage.onmousemove = function() {
        var e = arguments[0] || event;
        mouseX = (e.clientX - navImageOffsetX);
        owner.setGlobalCursor("default");
        return false;
      }
  
      return false;
    }
  
    navArea.onmousedown = function() {
      var e = arguments[0] || event;
      owner.setGlobalCursor("move");
      panel.onscroll = null;
      owner.updateGlobals();
      mouseX = (e.clientX - navImageOffsetX);
      var dx = navAreaWidth/2 + (navAreaX - mouseX);
      owner.navAreaMove(mouseX + dx);
  
      document.onmousemove = function() {
        var e = arguments[0] || event;
        mouseX = (e.clientX - navImageOffsetX);
        owner.navAreaMove(mouseX + dx);
        return false;
      }
  
      document.onmouseup = function() {
        document.onmousemove = null;
        panel.onscroll = panelOnScroll;
        owner.setGlobalCursor("default");
        return false;
      }
      return false;
    }
  }

  this.start = function() {
    /*******************************************************
     * Set the current sample
     *******************************************************/
    this.sample = this.samples[this.sampleIdx];
    highlightById('sample' + this.sample);
  
    this.chipType = this.chipTypes[this.chipTypeIdx];
    highlightById('chipType' + this.chipType);
  
    this.set = this.sets[this.setIdx];
    highlightById('set' + this.set);
    if (this.chromosome == null)
      this.chromosome = this.chromosomes[this.chromosomeIdx];
    highlightById('chromosome' + this.chromosome);

    this.scale = this.scales[this.scaleIdx];
    highlightById('zoom' + this.scale);
  
    if (navigatorZoom == -1) {
      navigatorZoom = this.scale;
    }

    this.imageUrl = document.getElementById('imageUrl');
    this.bookmarkUrl = document.getElementById('bookmarkUrl');
    this.cnrUrl = document.getElementById('cnrUrl');

    this.setupEventHandlers();
  
    this.updateNavigator();
    this.updatePanel();
    this.setStatus('');
    webcutsOptions['numberLinks'] = false;
    setTimeout('explorer.navAreaMoveRel(0.5);', 1000);
  }
    
  this.navAreaUpdate = function() {
    if (navAreaX < 0) {
      navAreaX = 0;
    } else if (navAreaX + navAreaWidth > navImageWidth) {
      navAreaX = navImageWidth - navAreaWidth;
    }
    navArea.style.width = navAreaWidth + "px";
    navArea.style.left = (navAreaX + navImageOffsetX) + "px";
    this.locatorUpdated();
  }

  this.navAreaMove = function(midX) {
    navAreaX = midX - navAreaWidth/2;
    this.navAreaUpdate();
    this.panelMove(navAreaX/navImageWidth);
  }

  this.navAreaMoveRel = function(relX) {
    this.navAreaMove(relX * navImageWidth);
  }

  this.panelMove = function(relOffset) {
    panelX = relOffset*panelMaxWidth;
    if (panelX < 0)
      panelX = 0;
    panel.scrollLeft = panelX;
  }

  this.panelUpdated = function() {
    this.updateGlobals();
    relOffset = panel.scrollLeft / panelMaxWidth;
    navAreaX = relOffset * navImageWidth;
    this.navAreaUpdate();
    this.locatorUpdated();
  }

  this.updatePanel = function() {
    var owner = this;

    var navAreaRelMidX = (navAreaX + navAreaWidth/2) / navImageWidth;
      owner.updateNavigatorWidth();

    var pathname = owner.getImagePathname(this.chipType, this.sample, this.chromosomeIdx+1, this.scale, this.set);

    panelImage = document.getElementById('panelImage');
    panelImage.onload = function() {
      owner.updateNavigatorWidth();
      owner.updateGlobals();
      owner.navAreaMoveRel(navAreaRelMidX);
      owner.loadCount = owner.loadCount - 1;
      if (owner.loadCount <= 0) {
        owner.loadCount = 0;
        owner.setStatus("");
      }
      panelImageOnLoad();
      panelImageOnLoad = function() {};
    }
    panelImage.src = pathname;
    this.imageUrl.href = pathname;
    updateText(this.imageUrl, pathname);
  
    /* Update the title of the page */
    var title = location.href;
    title = title.substring(0, title.lastIndexOf('\/'));
    title = title.substring(title.lastIndexOf('\/')+1);
    title = title + '/' + pathname;
    document.title = title;
  }

  this.updateNavigator = function() {
    var owner = this;

    var pathname = this.getImagePathname(this.chipType, this.sample, this.chromosomeIdx+1, navigatorZoom, this.set);
    navImage = document.getElementById("navigatorImage");
    navImage.onload = function() {
      owner.loadCount = owner.loadCount - 1;
      if (owner.loadCount <= 0) {
        owner.loadCount = 0;
        owner.setStatus("");
      }
    }
    navImage.src = pathname;
  } // updateNavigator()


  this.updateNavigatorWidth = function() {
    /* Update the width of the navigator */
    var chromosomeLength = new Array(3840, 3798, 3119, 2993, 2826, 2673, 2482, 2288, 2165, 2117, 2104, 2071, 1785, 1664, 1568, 1387, 1230, 1191, 998, 976, 733, 774, 2417);
    var relWidth = chromosomeLength[this.chromosomeIdx] / chromosomeLength[0];
    navImageWidth = Math.round(relWidth * nav.clientWidth);
    navAreaWidth = Math.round(relWidth * nav.clientWidth);
    navImage.style.width = "" + navImageWidth  + "px";
  }
  
  this.getMouseMb = function(x, chromosome, zoom) {
    return(-1);
  }
         
  this.onLoad = function() { }

  this.gotoNextSample = function(step) {
    var samples = this.samples;
    var nextSampleIdx = this.sampleIdx + step;
    if (nextSampleIdx >= samples.length) {
      nextSampleIdx = 0;
    } else if (nextSampleIdx < 0) {
      nextSampleIdx = samples.length-1;
    }
    this.setSample(nextSampleIdx);
    if (playSamples) {
      cmd = "explorer.gotoNextSample(" + step + ");";
      setTimeout(cmd, playDelay);
    }
  }

  this.playAlongSamples = function(cmd) {
    if (cmd == "start") {
      playSamples = true;
      this.gotoNextSample(+1);
    } else if (cmd == "stop") {
      playSamples = false;
    }
  }

  this.playSlower = function() {
    playDelay = playDelay * 2;
  }

  this.playFaster = function() {
    playDelay = playDelay / 2;
  }

//  alert("ChromosomeExplorer()...ok");
} /* ChromosomeExplorer */


/****************************************************************
 HISTORY:
 2012-02-04
 o Added playSlower() and playFaster().
 o Moved gotoNextSample() and playAlongSamples() to 
   ChromosomeExplorer.
 o Now declaring private variables.
 2012-02-01
 o Added setChromosomes().
 2008-07-30
 o Seems to work with Firefox 3.01.  Don't know about Firefox 2.
 2007-09-04
 o Added support for (model) "sets", e.g. 'glad', 'cbs'.
 2007-03-06
 o BUG FIX: Missing update() method.
 2007-02-20
 o Updated to <rootPath>/<dataSet>/<tags>/<chipType>/<set>/.
 o Created from old ChromosomeExplorer.js making it more of the
   style of class ArrayExplorer.
 ****************************************************************/

// alert("ChromosomeExplorer...done");
