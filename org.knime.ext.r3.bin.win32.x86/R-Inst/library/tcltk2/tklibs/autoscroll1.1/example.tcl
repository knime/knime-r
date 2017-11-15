source ./autoscroll.tcl
text .t -highlightthickness 0 -yscrollcommand ".scrolly set"
scrollbar .scrolly -orient v -command ".t yview"
pack .scrolly -side right -fill y
pack .t -side left -fill both -expand 1
::autoscroll::autoscroll .scrolly