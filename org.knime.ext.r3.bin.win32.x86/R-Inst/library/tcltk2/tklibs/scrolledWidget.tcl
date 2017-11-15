# scrolledWidget: add scrollbars to an existing widget
# create a standard widget with scrollbars around
# Inspired from http://wiki.tcl.tk/1146, and modified to use ttk::scrollbar and
# autoscroll features
#
# wigdet  -> name of the widget to be created
# parent  -> path to the ttk::frame, in which the widget and the scrollbars
#            should be created
# scroll  -> x, y, or both, use scrollbars on X or/and Y
# auto    -> x, y, or both, use autoscrollbars on X or/and Y
#
# returns: the path to the created widget (frame)
#
proc scrolledWidget {widget parent scroll auto} {
    # Create widget attached to scrollbars
    if {$scroll == "x" || $scroll == "both"} {
        ttk::scrollbar $parent.sx -orient horizontal \
            -command [list $widget xview]
        grid $parent.sx         -column 0 -row 1 -sticky ew
        $widget configure -xscrollcommand [list $parent.sx set]
        # Do we use autoscroll features?
        if {$auto == "x" | $auto == "both"} {
            package require autoscroll
            ::autoscroll::autoscroll $parent.sx
        }
    }
    if {$scroll == "y" || $scroll == "both"} {
        ttk::scrollbar $parent.sy -orient vertical \
            -command [list $widget yview]
        grid $parent.sy         -column 1 -row 0 -sticky ns
        $widget configure -yscrollcommand [list $parent.sy set]
        # Do we use autoscroll features?
        if {$auto == "y" | $auto == "both"} {
            package require autoscroll
            ::autoscroll::autoscroll $parent.sy
        }
    }
    # Arrange them in the parent frame
    grid $widget  -column 0 -row 0 -sticky ewsn
    grid columnconfigure $parent 0 -weight 1
    grid rowconfigure $parent 0 -weight 1
    # hide the original widget command from the interpreter:
    interp hide {} $parent
    # Install the alias:
    interp alias {} $parent {} scrolledWidgetCmd $widget
    # fix the bindtags:
    bindtags $widget [lreplace [bindtags $widget] 0 0 $parent]
    return $parent
}
proc scrolledWidgetCmd {self cmd args} {
    return [uplevel 1 [list $self $cmd] $args]
}
