#!/usr/bin/wish

# substitute your favorite method here...
source choosefont.tcl
package require choosefont 0.1
catch {namespace import choosefont::*}

# To test with tile
package require tile 0.7.2

# To test with different fonts
#font configure TkDefaultFont -family Georgia -size 12 -weight bold
#font configure TkTextFont -family Georgia -size 12 -slant italic

#::msgcat::mclocale en
#::msgcat::mclocale de
#::msgcat::mclocale fr

#variable fontExample
#set fontExample fontChooser
catch { font delete fontChooser }
font create fontChooser -family Courier -size 10 -weight bold
#toplevel .cf
label .l -text "Click on the button\nto change my font" -font fontChooser
if {[catch { package present tile }]} {
  button .b -text "  Choose font  " -font TkDefaultFont
} else {
	ttk::button .b -text "Choose font"
}
.b configure -command {
  set font [choosefont -font [font actual fontChooser]]
  if {$font != "" } { 
    catch { font delete fontChooser }
    eval [linsert $font 0 font create fontChooser]
  }
}
grid .l .b -padx 5 -pady 5

 button .btile -text "Use tile" -command {package require tile 0.7.2} -font TkDefaultFont
 button .beng -text "English" -command {::msgcat::mclocale en} -font TkDefaultFont
 grid .btile .beng -padx 5 -pady 5
#choosefont -font "Courier 10 italic"
#choosefont -font "{Lucida console} 9 bold italic underline" \
#  -title "Choose a fixed font" -fonttype "fixed" -style 3 -sizetype "point"
