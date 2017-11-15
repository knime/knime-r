# some (very!) rudimentary test code for mclistbox.tcl

source mclistbox.tcl
package require mclistbox 1.00
catch {namespace import mclistbox::*}

proc test {{addcolumns 1}} {
    source mclistbox.tcl

    destroy .vsb .container .hsb

    frame .container -bd 2 -relief sunken
    pack .container -side top -fill both -expand y

    puts "creating mclistbox"
    set ::foo [::mclistbox::mclistbox .container.foo \
	    -yscrollcommand [list .container.vsb set] \
	    -xscrollcommand [list .container.hsb set] \
	    -selectmode extended \
	    -borderwidth 0 \
	    -width 60 \
	    -height 20 ]

    scrollbar .container.vsb \
	    -orient vertical \
	    -command [list .container.foo yview]
    scrollbar .container.hsb \
	    -orient horizontal \
	    -command [list .container.foo xview]

    grid .container.vsb -row 0 -column 1 -sticky ns
    grid .container.hsb -row 1 -column 0 -sticky ew
    grid .container.foo -row 0 -column 0 -sticky nsew -padx 0 -pady 0
    grid columnconfigure .container 0 -weight 1
    grid columnconfigure .container 1 -weight 0
    grid rowconfigure .container 0 -weight 1
    grid rowconfigure .container 1 -weight 0

    set commands [lsort [info commands]]
    
    puts "adding column number"
    .container.foo column add number -label "Num" -width 5
    puts "adding column command"
    .container.foo column add command -label "Command" -width 30
    puts "adding column size"
    .container.foo column add size -label "String Length" -width 13

    puts "inserting data"
    set count [llength $commands]
    set row 1
    foreach command $commands {
	set stuff [list $row $command "[string length $command] chars"]
	.container.foo insert end $stuff
	incr row
    }


    puts "$row rows added..."

    .container.foo configure -width 49

    
#    bind .container.foo <ButtonPress-3> {findColumn %X}
    bind .container.foo <ButtonPress-3> {testConvert %W %x %y %X %Y}

}

proc testIndicies {w} {
    puts "deleting all columns..."
    foreach column [$w column names] {
	$w column delete $column
    }
    puts "adding a single column"
    $w column add col0 -label "Column 0"

    puts "deleting everything from 0 to end"
    $w delete 0 end
    puts "inserting at 0..."
    $w insert 0 A0
    puts "inserting at @0,0..."
    $w insert @0,0 A1
    puts "inserting at end..."
    $w insert end A2
    puts "contents should be A1,A0,A2: [$w get 0 end]"
    
}

proc testConvert {w x y rx ry} {
    # let's calculate the coordinates to see how they compare
    # to the magic conversion:
    set rootx [winfo rootx .container.foo]
    set rooty [winfo rooty .container.foo]
    puts "calculated: [expr $rx - $rootx],[expr $ry - $rooty]"
    set result [::mclistbox::convert $w -W -x $x -y $y]
    foreach {w x y} $result {}
    puts "%W=$w %x=$x %y=$y"
}

proc findColumn {x} {
    puts "findColumn..."
    # sigh. We are passed in a root x which we have to convert
    # to something relative to the widget. Easy enough to do...
    set x [expr $x - [winfo rootx .container.foo]]
    puts "findColumn: [.container.foo column nearest $x]"
}

proc testConfig {} {
    set listbox .container.foo
    puts "testing global options"
    if {[catch {
	foreach config [lsort [array names ::mclistbox::globalOptions]] {
	    puts "$config..."
	    puts -nonewline "    result of cget: "
	    puts "'[set result(cget) [$listbox cget $config]]'"
	    puts -nonewline "    result of configure: "
	    puts "'[set result(configure) [$listbox configure $config]]'"
	    set oname [lindex $result(configure) 1]
	    set oclass [lindex $result(configure) 2]
	    puts -nonewline "    from the option database: "
	    puts "'[option get $listbox $oname $oclass]'"
	    if {$result(cget) != [lindex $result(configure) 4]} {
		puts stderr "    the values are not the same: "
		puts stderr "            cget=$result(cget)"
		puts stderr "       configure=[lindex $result(configure) 4]"
	    }
	}
    } error]} {
	puts stderr "error with the config foo: $error"
    }
}

proc testColumnConfig {} {
    set listbox .container.foo
    foreach column {number command size} {
	foreach config [lsort [array names ::mclistbox::columnOptions]] {

	}
    }
    puts "testing commands with a bogus column:"
    $listbox column configure bogus -width 10
}


proc addrows {w n} {
    for {set i 0} {$i < $n} {incr i} {
	$w insert end $i
    }
}

proc addrowsfast {w n} {
    set biglist {}
    for {set i 0} {$i < $n} {incr i} {
	lappend biglist $i
	update idletasks
    }
    update idletasks
    eval $w insert end $biglist
}


test
testConfig
testColumnConfig
