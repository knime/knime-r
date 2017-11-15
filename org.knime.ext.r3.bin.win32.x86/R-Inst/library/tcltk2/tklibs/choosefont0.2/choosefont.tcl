###############################
#
# a pure Tcl/Tk font chooser
#
# by ulis, 2002
#
# NOL (No Obligation Licence)
#
#
# modifs by Martin Lemburg, 2002
# Basic Tile'ification and msgcat support
# by schlenk, 2005
#
# Adaptation to R and further enhancements
# by Philippe Grosjean, 2007, GNU GPL 2 or above license
###############################

package require Tcl 8.4
package require Tk 8.4
package require msgcat
#package require tile 0.7.2 ;# The dialog displays tile widgets if package loaded

namespace eval ::choosefont {
  namespace import ::msgcat::mc
  namespace export choosefont

  variable w .choosefont
  variable font
  variable listvar
  variable family
  variable size
  variable bold
  variable italic
  variable underline
  variable overstrike
  variable ok
  variable lock 1
  
  variable defaultopts
  
  variable usetile
  variable locale
  set usetile 0
  set locale [::msgcat::mclocale]
  
  variable mnemonics
  variable mnemopaths
  set mnemonics {}
  set mnemopaths {}

  # Get internationalization string
  ::msgcat::mcload [file join [file dirname [info script]] msgs]
  
  # This is for correct handling of amperstand as mnemonic indicators (Alt-Key)
  proc mca {widget text} {
    variable mnemonics
    variable mnemopaths
    
	foreach {newtext under} [::tk::UnderlineAmpersand [mc $text]] {
      $widget configure -text $newtext -underline $under
    }
    # Add this info to the list of mnemonics
	if {$under > -1} {
      lappend mnemonics [string tolower [string index $newtext $under]]
      lappend mnemopaths $widget 
    }
  }
  
  # This font is inspired from tile
  # Make sure that TkDefaultFont is defined
  if {[lsearch [font names] TkDefaultFont] == -1} {
    catch {font create TkDefaultFont}
    switch -- [tk windowingsystem] {
      win32 {
        if {$tcl_platform(osVersion) >= 5.0} {
          font configure TkDefaultFont -family "Tahoma" -size -11
        } else {
          font configure TkDefaultFont -family "MS Sans Serif" -size -11
        }
      }
      classic -
      aqua {
        font configure TkDefaultFont -family "Lucida Grande" -size 13
      }
      x11 {
        if {![catch {tk::pkgconfig get fontsystem} fs] && $fs eq "xft"} {
          font configure TkDefaultFont -family "sans-serif" -size -12
        } else {
          font configure TkDefaultFont -family "Helvetica" -size -12	    
        }
      }
    }
  }

  # ================
  # choose a font
  # ================
  # args:
  #       -font      an initial (and optional) font
  #       -title     an optional title
  #       -fonttype  'all' (by default), 'fixed' or 'prop'
  #       -style     do we activate additional 'style' options?
  #                   >= 1 => 'bold'
  #                   >= 2 => 'italic'
  #                   >= 3 => 'underline'
  #                   >= 4 => 'overstrike'
  #       -sizetype  'all' (by default), 'point', 'pixel'
  # returns:
  #       "" if the user aborted
  #       or the description of the selected font
  # usage:
  #       namespace import ::choosefont::choosefont
  #       choosefont "Courier 10 italic" "new font"

  proc choosefont {args} { 
    if {[llength $args] & 1} {
      return -code error "invalid number of arguments given to choosefont (uneven number) : $args"
    }
  
    global tcl_platform
    global tile_use
    
    # ------------------
    # get choosefont env
    # ------------------
    variable ::choosefont::w
    variable ::choosefont::font
    variable ::choosefont::listvarall
    variable ::choosefont::listvarfixed
    variable ::choosefont::listvarprop
    variable ::choosefont::listvar
    variable ::choosefont::family
    variable ::choosefont::size
    variable ::choosefont::bold
    variable ::choosefont::italic
    variable ::choosefont::underline
    variable ::choosefont::overstrike
    variable ::choosefont::ok
    variable ::choosefont::usetile
    variable ::choosefont::locale
    variable ::choosefont::mnemonics
    variable ::choosefont::mnempaths
    variable ::choosefont::lock

    # ------------------
    # handling of optional arguments
    # ------------------
    variable ::choosefont::defaultopts
    variable opts
    # Initialize de fault fonts (done only once)
    if {![info exists defaultopts]} {
      set defaultopts {-font "" -title "" -fonttype all -style 4 -sizetype all}    
    }
    # Create an array (easier to work with)
    array set opts $defaultopts
    # Override options provided as arguments
    array set opts $args
    
    # ------------------
    # current font
    # ------------------
	if {$opts(-font) != ""} { set font $opts(-font) }
    if {![info exists font]} {
      # We try to get the default text or fixed font, depending on fonttype
      if {$opts(-fonttype) == "fixed"} {
        catch { set font [font actual TkFixedFont] }
      } else { ;# 'all' or 'prop'
        catch { set font [font actual TkTextFont] }
      }
    }
    # Make sure that the default one is correct regarding 'fixed' or 'prop'
    # Otherwise, select default fonts instead
    if {$opts(-fonttype) == "fixed" & [font metrics $font -fixed]  == 0 } {
      catch { set font "courier" }
    }
    if {$opts(-fonttype) == "prop" & [font metrics $font -fixed]  == 1 } {
      catch { set font "helvetica" }
    }
 
    # ------------------
    # dialog
    # ------------------
    set notile [catch { package present tile }]
    catch {if {[winfo exists $tile_use] && $tile_use == 0} {set notile 1}}
    # If it is not the first time the dialog is displayed
    # and tile presence, or locale has changed
    # then, we destroy the dialog box and rebuild it
    if {[winfo exists $w]} {
	  if {$notile != $usetile || [::msgcat::mclocale] != $locale} {
	    destroy $w
	    set mnemonics {}
        set mnemopaths {}
	  }
	}
	set usetile $notile
	set locale [::msgcat::mclocale]
	 
    if {[winfo exists $w]} {
      # show the dialog
      wm deiconify $w
      
      # Switch to the corresponding list of fonts ('all', 'prop' or 'fixed')
      switch -exact -- [string tolower $opts(-fonttype)] {
        fixed    { set listvar $listvarfixed }
        prop     { set listvar $listvarprop }
        default  { set listvar $listvarall }
      }
      
      # Possibly reconfigure the size selector
      if {$notile} {
        switch $opts(-sizetype) {
          point {
            set minsize 1; set maxsize 100
          }
          pixel {
            set minsize -100; set maxsize -1
          }
          default {
            set minsize -100; set maxsize 100
          }
        }
        $w.fa.f.esize configure -from $minsize -to $maxsize
      } else {
        switch $opts(-sizetype) {
          point {
            $w.fa.f.esize configure -values [list 7 8 9 10 11 12 13 14 15 \
              20 25 30 40]
          }
          pixel {
            $w.fa.f.esize configure -values [list -20 -15 -14 -13 -12 -11 \
              -10 -9 -8]              
          }
          default {
            $w.fa.f.esize configure -values [list -20 -15 -14 -13 -12 -11 \
              -10 -9 -8 7 8 9 10 11 12 13 14 15 20 25 30 40]
          }
        }
      }
    } else {	;# Create the dialog box
      if {[info exists listvarall] == 0} {
	    set listvarall [lsort -dictionary [font families]]
        #PhG: allow to filter out fixed and/or proportional fonts
        set listvarfixed {}
        set listvarprop {}
        foreach family $listvarall {
          if {[font metrics "{$family}" -fixed]  == 1 } {
            lappend listvarfixed $family
          } else {
            lappend listvarprop $family
          }
        }
      }
      switch -exact -- [string tolower $opts(-fonttype)] {
        fixed    { set listvar $listvarfixed }
        prop     { set listvar $listvarprop }
        default  { set listvar $listvarall }
      }

	  # create the dialog
      toplevel $w -class Dialog
      wm resizable $w 0 0
      wm title $w [mc "Choose a font"]
      wm iconname $w Dialog
      wm protocol $w WM_DELETE_WINDOW { }
      
      # PhG: under Windows, make it topmost, so that it is always visible
      if { [regexp topmost [wm attributes $w]] == 1 } {
        wm attributes $w -topmost 1
      }
      # Dialog boxes should be transient with respect to their parent,
      # so that they will always stay on top of their parent window.  However,
      # some window managers will create the window as withdrawn if the parent
      # window is withdrawn or iconified.  Combined with the grab we put on the
      # window, this can hang the entire application.  Therefore we only make
      # the dialog transient if the parent is viewable.
      if {[winfo viewable [winfo toplevel [winfo parent $w]]] } {
        wm transient $w [winfo toplevel [winfo parent $w]]
      }    

      if {[string equal $tcl_platform(platform) "macintosh"]
        || [string equal [tk windowingsystem] "aqua"]} {
        ::tk::unsupported::MacWindowStyle style $w dBoxProc
      }

      # create widgets
      if {$notile} {
        frame $w.f -bd 1 -relief sunken
      } else {
        ttk::labelframe $w.f
      }
        # We never use tile for these ones
        label $w.f.h -height 4
        label $w.f.l -textvariable ::choosefont::family

      if {$notile} {
        frame $w.fl
          label $w.fl.la -font TkDefaultFont
          listbox $w.fl.lb -listvar ::choosefont::listvar -width 30 \
            -font TkDefaultFont -yscrollcommand [list $w.fl.sb set] \
            -selectmode single -exportselection 0
          scrollbar $w.fl.sb -command [list $w.fl.lb yview]
      } else {
        ttk::frame $w.fl
          ttk::label $w.fl.la
          listbox $w.fl.lb -listvar ::choosefont::listvar -width 30 -bd 0 \
            -font TkDefaultFont -yscrollcommand [list $w.fl.sb set] \
            -selectmode single -exportselection 0
          ttk::scrollbar $w.fl.sb -orient vertical -command [list $w.fl.lb yview]            
      }
      mca $w.fl.la &Family:

      if {$notile} {
        frame $w.fa -bd 2 -relief groove
          frame $w.fa.f
            label $w.fa.f.lsize -font TkDefaultFont
            switch $opts(-sizetype) {
              point {
                set minsize 1; set maxsize 100
              }
              pixel {
                set minsize -100; set maxsize -1
              }
              default {
                set minsize -100; set maxsize 100
              }
            }
			spinbox $w.fa.f.esize -textvariable ::choosefont::size -width 3 \
              -validate focusout -vcmd {string is integer -strict %P} \
                  -from $minsize -to $maxsize -font TkDefaultFont
            checkbutton $w.fa.f.bold -variable ::choosefont::bold \
               -font TkDefaultFont
            checkbutton $w.fa.f.italic -variable ::choosefont::italic \
               -font TkDefaultFont
            checkbutton $w.fa.f.under -variable ::choosefont::underline \
               -font TkDefaultFont
            checkbutton $w.fa.f.over -variable ::choosefont::overstrike \
               -font TkDefaultFont
      } else {
        ttk::labelframe $w.fa
          ttk::frame $w.fa.f
            ttk::label $w.fa.f.lsize
            ttk::combobox $w.fa.f.esize -textvariable ::choosefont::size \
              -width 3 -exportselection 0
            switch $opts(-sizetype) {
              point {
                $w.fa.f.esize configure -values [list 7 8 9 10 11 12 13 14 15 \
				  20 25 30 40]
              }
              pixel {
                $w.fa.f.esize configure -values [list -20 -15 -14 -13 -12 -11 \
			      -10 -9 -8]              
              }
              default {
                $w.fa.f.esize configure -values [list -20 -15 -14 -13 -12 -11 \
			      -10 -9 -8 7 8 9 10 11 12 13 14 15 20 25 30 40]
			  }
			}
            ttk::checkbutton $w.fa.f.bold -variable ::choosefont::bold
            ttk::checkbutton $w.fa.f.italic -variable ::choosefont::italic
            ttk::checkbutton $w.fa.f.under -variable ::choosefont::underline
            ttk::checkbutton $w.fa.f.over -variable ::choosefont::overstrike
      }
      mca $w.fa.f.lsize &Size:
      mca $w.fa.f.bold &Bold
      mca $w.fa.f.italic &Italic
	  mca $w.fa.f.under &Underline
	  mca $w.fa.f.over &Overstrike
      
      if {$notile} {
        frame $w.fb
          button $w.fb.ok -text [mc OK] -width 10 \
            -command { set ::choosefont::ok 1 } -font TkDefaultFont
          button $w.fb.cancel -text [mc Cancel] -width 10 \
            -command { set ::choosefont::ok 0 } -font TkDefaultFont
      } else {
        ttk::frame $w.fb
          ttk::button $w.fb.ok -text [mc OK] -width 10 \
            -command { set ::choosefont::ok 1 }
          ttk::button $w.fb.cancel -text [mc Cancel] -width 10 \
            -command { set ::choosefont::ok 0 }
      }
      wm protocol $w WM_DELETE_WINDOW { $::choosefont::w.fb.cancel invoke }
      
      # bind events
	  bind $w.fl.lb <ButtonRelease-1> {
	    set ::choosefont::family [%W get [%W cursel]]
        focus %W
	  }

      # listbox handling
      bind $w <Home> { ::choosefont::selectfont %W First }
      bind $w <End> { ::choosefont::selectfont %W Last }
      bind $w <Control-Home> { ::choosefont::selectfont %W First }
      bind $w <Control-End> { ::choosefont::selectfont %W Last }
      bind $w <Key-Next> { ::choosefont::selectfont %W PgDown }
      bind $w <Key-Prior> { ::choosefont::selectfont %W PgUp }
      bind $w <KeyPress> { ::choosefont::selectfont %W %K }

      # buttons handling
	  bind $w <Escape> [list $w.fb.cancel invoke]
      bind $w <Return> [list $w.fb.ok invoke]
      
      # Alt-key navigation
      if {[llength $mnemonics] > 0} {
        bind $w <Alt-Key> {
		  set w [winfo toplevel %W]
          set key [string tolower %K]
          set pos [lsearch $::choosefont::mnemonics $key]
          if {$pos > -1} {
            set target [lindex $::choosefont::mnemopaths $pos]
		    event generate $target <<AltUnderlined>>
          }
        }
      }
      bind $w.fl.la <<AltUnderlined>> [list focus $w.fl.lb]
      bind $w.fa.f.lsize <<AltUnderlined>> { focus $w.fa.f.esize }
      bind $w.fa.f.bold <<AltUnderlined>> {
        $w.fa.f.bold invoke; focus $w.fa.f.bold }
      bind $w.fa.f.italic <<AltUnderlined>> {
        $w.fa.f.italic invoke; focus $w.fa.f.italic }
      bind $w.fa.f.under <<AltUnderlined>> {
        $w.fa.f.under invoke; focus $w.fa.f.under }
      bind $w.fa.f.over <<AltUnderlined>> {
        $w.fa.f.over invoke; focus $w.fa.f.over }

      set lock 1

      trace variable ::choosefont::family     w ::choosefont::createfont
      trace variable ::choosefont::size       w ::choosefont::createfont
      trace variable ::choosefont::bold       w ::choosefont::createfont
      trace variable ::choosefont::italic     w ::choosefont::createfont
      trace variable ::choosefont::underline  w ::choosefont::createfont
      trace variable ::choosefont::overstrike w ::choosefont::createfont

      # place widgets
      grid $w.f           -row 0 -column 0 -columnspan 2 -sticky nsew -pady {2 5}
      grid $w.fl          -row 1 -column 0 -padx 5 -pady 5
      grid $w.fa          -row 1 -column 1 -sticky nsew -padx 5 -pady 5
      grid $w.fb          -row 2 -column 0 -columnspan 2 -sticky ew -pady 7
      grid $w.f.h         -row 0 -column 0
      grid $w.f.l         -row 0 -column 1 -sticky nsew -pady 3
      grid $w.fl.la       -row 0 -column 0 -sticky nw -pady 3
      grid $w.fl.lb       -row 1 -column 0
      grid $w.fl.sb       -row 1 -column 1 -sticky ns
      grid $w.fa.f        -padx 5 -pady 5
      grid $w.fa.f.lsize  -row 0 -column 0 -padx 5 -pady 10 -sticky w
      grid $w.fa.f.esize  -row 0 -column 1 -sticky w
      grid $w.fa.f.bold   -row 1 -column 0 -columnspan 2 -sticky w
      grid $w.fa.f.italic -row 2 -column 0 -columnspan 2 -sticky w
      grid $w.fa.f.under  -row 3 -column 0 -columnspan 2 -sticky w
      grid $w.fa.f.over   -row 4 -column 0 -columnspan 2 -sticky w
      grid $w.fb.ok $w.fb.cancel -padx 20
      # Center the Window on screen the first time it is used
      ::tk::PlaceWindow $w
    }

    # Reconfigure the dialog box with current font
    set family      [font actual $font -family]
    set size        [font actual $font -size]
    set bold        [expr {[font actual $font -weight] == "bold"}] 
	if {$opts(-style) > 0} { # Allow bold
	  $w.fa.f.bold configure -state normal
    } else {
      $w.fa.f.bold configure -state disabled
    }
    set italic      [expr {[font actual $font -slant] == "italic"}]
	if {$opts(-style) > 1} { # Allow italic  
	  $w.fa.f.italic configure -state normal
    } else {
	  $w.fa.f.italic configure -state disabled
    }
    set underline   [font actual $font -underline]
	if {$opts(-style) > 2} {	# Allow underline
      $w.fa.f.under configure -state normal	  
    } else {
	  $w.fa.f.under configure -state disabled
    }
    set overstrike  [font actual $font -overstrike]
	if {$opts(-style) > 3} {	# Allow overstrike  
      $w.fa.f.over configure -state normal      
    } else {
	  $w.fa.f.over configure -state disabled
    }

    set lock        0
    ::choosefont::createfont

    # ------------------
    # end of dialog
    # ------------------

    if {$opts(-title) != ""} {
	  wm title $w $opts(-title)
	} else {
	  wm title $w [mc "Choose a font"]
	}
    set newIndex  [lsearch -exact $listvar $family]
    # PhG: clear the selection list first!
    $w.fl.lb selection clear 0 end
    # PhG: this is needed by R, otherwise, there is a bug with the list
    update
    $w.fl.lb selection set $newIndex
    $w.fl.lb activate $newIndex
    $w.fl.lb see $newIndex
    # PhG: focus on the list
    focus $w.fl.lb

    # Grab the focus, wait for user action
	::tk::SetFocusGrab $w $w.fl.lb
    vwait ::choosefont::ok
    # Restore the focus and hide the font chooser dialog box
    ::tk::RestoreFocusGrab $w $w.fl.lb withdraw

    if {$ok} {
      return [::choosefont::createfont]
    } else {
      return ""
    }
  }

  # ================
  # ancillary procs
  # ================
  proc selectfont {w mode} {
    if {[winfo class $w] != "Listbox"} { return }

    set oldIndex [$w curselection]

    if {[string length $mode] > 1} {
      switch -exact -- $mode {
        Down    {set newIndex [expr {$oldIndex+1}]}
        Up      {set newIndex [expr {$oldIndex-1}]}
        PgDown  {set newIndex [expr {$oldIndex+10}]}
        PgUp    {set newIndex [expr {$oldIndex-10}]}
        First   {set newIndex 0}
        Last    {set newIndex end}
        default { return }
      }

      if {($newIndex ne "end") && $newIndex} {
        if {$newIndex < 0} {
          set newIndex 0
        } elseif {$newIndex > [$w size] - 1} {
          set newIndex end
        }
      }
      focus $w
    } else {
      set m [string tolower $mode]
      set oldFamily  [string tolower [lindex $::choosefont::listvar $oldIndex]]
      set families [string tolower $::choosefont::listvar]
      if {[string match $m* $oldFamily]} {  
        set newIndex  [expr {$oldIndex + 1}]
        set newFamily [lindex $::choosefont::listvar $newIndex]
        if {![string match $m* [string tolower $newFamily]]} {
          set newIndex [lsearch -glob $families $m* ]
        }
      } else {
        set newIndex [lsearch -glob $families $m* ]
      }
      if {$newIndex < 0} { return }
    }

    set ::choosefont::family  [$w get $newIndex]

    $w selection clear $oldIndex
    $w selection set $newIndex
    $w activate $newIndex
    $w see $newIndex
    focus $w
    
    return
  }

  proc createfont {args} {
    if {$::choosefont::lock} {return ""}

    variable ::choosefont::w
    variable ::choosefont::font
    variable ::choosefont::family
    variable ::choosefont::size
    variable ::choosefont::bold
    variable ::choosefont::italic
    variable ::choosefont::underline
    variable ::choosefont::overstrike

    catch { font delete TkChooseFont }

    if {$::choosefont::size != "" \
      & [catch {expr {int($::choosefont::size)}}] == "0"} {
      set f [list -family $family -size $size]
    } else {
      set f [list -family $family]
    }
    foreach {var option value} {
      bold        -weight     bold
      italic      -slant      italic
      underline   -underline  1
      overstrike  -overstrike 1
    } { if {[set $var]} { lappend f $option $value } }

	eval [linsert $f 0 font create TkChooseFont]
	$w.f.l config -font TkChooseFont
    
    return $f
  }
}

package provide choosefont 0.2
