#!/usr/bin/wish
#
# Adapted from tree.tcl example file by Ph. Grosjean, 2005
#
# I am D. Richard Hipp, the author of this code.  I hereby
# disavow all claims to copyright on this program and release
# it into the public domain.
#
#                     D. Richard Hipp
#                     January 31, 2001
#
# As an historical record, the original copyright notice is
# reproduced below:
#
# Copyright (C) 1997,1998 D. Richard Hipp
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Library General Public
# License as published by the Free Software Foundation; either
# version 2 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Library General Public License for more details.
#
# You should have received a copy of the GNU Library General Public
# License along with this library; if not, write to the
# Free Software Foundation, Inc., 59 Temple Place - Suite 330,
# Boston, MA  02111-1307, USA.
#
# Author contact information:
#   drh@acm.org
#   http://www.hwaci.com/drh/
#
# $Revision: 1.3 $

#################
#
# The following code implements an example of using the
# notebook widget.
#

source "./notebook.tcl"

Notebook:create .n -pages {One Two Three Four Five} -pad 3 
pack .n -fill both -expand 1
set w [Notebook:frame .n One]
label $w.l -text "Hello.\nThis is page one"
pack $w.l -side top -padx 10 -pady 50
set w [Notebook:frame .n Two]
text $w.t -font fixed -yscrollcommand "$w.sb set" -width 40
$w.t insert end "This is a text widget.  Type in it, if you want\n"
pack $w.t -side left -fill both -expand 1
scrollbar $w.sb -orient vertical -command "$w.t yview"
pack $w.sb -side left -fill y
set w [Notebook:frame .n Three]
set p3 red
frame $w.f
pack $w.f -padx 30 -pady 30
foreach c {red orange yellow green blue violet} {
  radiobutton $w.f.$c -fg $c -text $c -variable p3 -value $c -anchor w
  pack $w.f.$c -side top -fill x
}
set w [Notebook:frame .n Four]
frame $w.f
pack $w.f -padx 30 -pady 30
button $w.f.b -text {Goto} -command [format {
  set i [%s cursel]
  if {[string length $i]>0} {
    Notebook:raise .n [%s get $i]
  }
} $w.f.lb $w.f.lb]
pack $w.f.b -side bottom -expand 1 -pady 5
listbox $w.f.lb -yscrollcommand "$w.f.sb set"
scrollbar $w.f.sb -orient vertical -command "$w.f.lb yview"
pack $w.f.lb -side left -expand 1 -fill both
pack $w.f.sb -side left -fill y
$w.f.lb insert end One Two Three Four Five
set w [Notebook:frame .n Five]
button $w.b -text Exit -command exit
pack $w.b -side top -expand 1
