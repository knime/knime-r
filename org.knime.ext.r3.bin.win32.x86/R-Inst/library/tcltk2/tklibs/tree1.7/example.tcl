#!/usr/bin/wish
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
# $Revision: 1.7 $

#################
#
# This is an example of use of the Tree widget.
#

source "./tree.tcl"

. config -bd 3 -relief flat
frame .f -bg white
pack .f -fill both -expand 1
image create photo idir -data {
    R0lGODdhEAAQAPIAAAAAAHh4eLi4uPj4APj4+P///wAAAAAAACwAAAAAEAAQAAADPVi63P4w
    LkKCtTTnUsXwQqBtAfh910UU4ugGAEucpgnLNY3Gop7folwNOBOeiEYQ0acDpp6pGAFArVqt
    hQQAO///
}
image create photo ifile -data {
    R0lGODdhEAAQAPIAAAAAAHh4eLi4uPj4+P///wAAAAAAAAAAACwAAAAAEAAQAAADPkixzPOD
    yADrWE8qC8WN0+BZAmBq1GMOqwigXFXCrGk/cxjjr27fLtout6n9eMIYMTXsFZsogXRKJf6u
    P0kCADv/
}
frame .f.mb -bd 2 -relief raised
pack .f.mb -side top -fill x
menubutton .f.mb.file -text File -menu .f.mb.file.menu
catch {
  menu .f.mb.file.menu
  .f.mb.file.menu add command -label Quit -command exit
}
menubutton .f.mb.edit -text Edit
menubutton .f.mb.view -text View
menubutton .f.mb.help -text Help
pack .f.mb.file .f.mb.edit .f.mb.view .f.mb.help -side left -padx 10
Tree:create .f.w -width 150 -height 400 -yscrollcommand {.f.sb set}
scrollbar .f.sb -orient vertical -command {.f.w yview}
pack .f.w -side left -fill both -expand 1 -padx 5 -pady 5
pack .f.sb -side left -fill y
frame .f.c -height 400 -width 400 -bg white
pack .f.c -side left -fill both -expand 1
label .f.c.l -width 40 -text {} -bg [.f.c cget -bg]
pack .f.c.l -expand 1
foreach z {1 2 3} {
  Tree:newitem .f.w /dir$z -image idir
  foreach x {1 2 3 4 5 6} {
    Tree:newitem .f.w /dir$z/file$x -image ifile
  }
  Tree:newitem .f.w /dir$z/subdir -image idir
  foreach y {1 2} {
    Tree:newitem .f.w /dir$z/subdir/file$y -image ifile
  }
  foreach zz {1 2 3 4} {
    Tree:newitem .f.w /dir$z/subdir/ssdir$zz -image idir
    Tree:newitem .f.w /dir$z/subdir/ssdir$zz/file1  ;# No icon!
    Tree:newitem .f.w /dir$z/subdir/ssdir$zz/file2 -image ifile
  }
}
.f.w bind x <1> {
  set lbl [Tree:labelat %W %x %y]
  Tree:setselection %W $lbl
  .f.c.l config -text $lbl
}
.f.w bind x <Double-1> {
  Tree:open %W [Tree:labelat %W %x %y]
}
update
