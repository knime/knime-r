# a simple directory viewer
# 
# this program uses a multicolumn listbox (mclistbox) to implement
# a simple directory browser

# substitute your favorite method here...
source mclistbox.tcl
package require mclistbox 1.02
catch {namespace import mclistbox::*}

proc showSelection {args} {
    puts "selection has changed: $args"
}

proc main {} {
    wm title . "Simple Directory Viewer"

    # this lets us be reentrant...
    eval destroy [winfo children .]

    # we want the listbox and two scrollbars to be embedded in a 
    frame .container -bd 2 -relief sunken

    # frame so they look like a single widget
    scrollbar .vsb -orient vertical -command [list .listbox yview]
    scrollbar .hsb -orient horizontal -command [list .listbox xview]

    # we will purposefully make the width less than the sum of the
    # columns so that the scrollbars will be functional right off
    # the bat.
    mclistbox .listbox \
	    -bd 0 \
	    -height 10 \
	    -width 60 \
	    -columnrelief flat \
	    -labelanchor w \
	    -columnborderwidth 0 \
	    -selectcommand "showSelection" \
	    -selectmode extended \
	    -labelborderwidth 2 \
	    -fillcolumn name \
	    -xscrollcommand [list .hsb set] \
	    -yscrollcommand [list .vsb set]

    # add the columns we want to see
    .listbox column add name -label "Name"          -width 40
    .listbox column add size -label "Size"          -width 12
    .listbox column add mod  -label "Last Modified" -width 18

    # set up bindings to sort the columns.
    .listbox label bind name <ButtonPress-1> "sort %W name"
    .listbox label bind size <ButtonPress-1> "sort %W size"
    .listbox label bind mod  <ButtonPress-1> "sort %W mod"

    grid .vsb -in .container -row 0 -column 1 -sticky ns
    grid .hsb -in .container -row 1 -column 0 -sticky ew
    grid .listbox -in .container -row 0 -column 0 -sticky nsew -padx 0 -pady 0
    grid columnconfigure .container 0 -weight 1
    grid columnconfigure .container 1 -weight 0
    grid rowconfigure    .container 0 -weight 1
    grid rowconfigure    .container 1 -weight 0

    pack .container -side top -fill both -expand y

    # populate the columns with information about the files in the
    # current directory
    foreach file [lsort [glob *]] {
	if {$file == "." || $file == ".."} continue
	set size [set mtime ""]
	
	catch {set mtime [clock format [file mtime $file] -format "%x %X"]}
	    set size [file size $file]
	    if {$size > 1048576} {
		set size [format "%2.2fMB" [expr $size / 1048576.0]]
	    } elseif {$size > 1024} {
		set size [format "%2.2fKB" [expr $size / 1024.0]]
	    }

	.listbox insert end [list $file $size $mtime]
    }

    # bind the right click to pop up a context-sensitive menu
    # we can use the proc ::mclistbox::convert to convert the 
    # binding substitutions we need. I've included two examples
    # to illustrate. Either method should give identical results. The
    # first method is slightly more efficient since it calls the 
    # conversion routine only once. The second method calls the 
    # procedure once for each of %W, %x and %y. 

#    bind .listbox <ButtonPress-3> \
#	{eval showContextMenu [::mclistbox::convert %W -W -x %x -y %y] %X %Y}

    bind .listbox <ButtonPress-3> \
	{showContextMenu \
	    [::mclistbox::convert %W -W] \
	    [::mclistbox::convert %W -x %x] \
	    [::mclistbox::convert %W -y %y] \
	    %X %Y}

}

# x,y are the coordinates relative to the upper-left corner of the
# listbox; rootx,rooty are screen coordinates (for knowing where 
# to place the menu). w is the name of the mclistbox widget that was
# clicked on.
proc showContextMenu {w x y rootx rooty} {
    catch {destroy .contextMenu}
    menu .contextMenu -tearoff false

    # ask the widget for what column this is
    set column [$w column nearest $x]
    set columnLabel [$w column cget $column -label]

    .contextMenu configure -title "$columnLabel"
    .contextMenu add command \
	    -label "Sort by $columnLabel" \
	    -command [list sort $w $column]
    .contextMenu add command \
	    -label "Hide $columnLabel" \
	    -command [list $w column configure $column -visible false]
    .contextMenu add separator
    .contextMenu add command \
	    -label "Show All Hidden Columns" \
	    -command "showAllColumns $w"
    
    tk_popup .contextMenu $rootx $rooty
}

proc showAllColumns {w} {
    foreach column [$w column names] {
	$w column configure $column -visible true
    }
}

# sort the list based on a particular column
proc sort {w id} {

    set data [$w get 0 end]
    set index [lsearch -exact [$w column names] $id]

    set result [lsort -index $index $data]

    $w delete 0 end

    # ... and add our sorted data in
    eval $w insert end $result

}

main

