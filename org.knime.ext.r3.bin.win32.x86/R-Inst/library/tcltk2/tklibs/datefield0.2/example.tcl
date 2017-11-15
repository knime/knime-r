source ./datefield.tcl
# package require datefield

 wm title . "Datefield example"
 proc DayOfWeek {args} {
     set now [clock scan $::myDate]
     set ::myDate2 [clock format $now -format %A]
 }
 trace variable myDate w DayOfWeek

 ::datefield::datefield .df -textvariable myDate
 label .l1 -text "Enter a date:"   -anchor e
 label .l2 -text "That date is a:" -anchor e
 label .l3 -textvariable myDate2 -relief sunken -width 12

 grid .l1 .df -sticky ew
 grid .l2 .l3 -sticky ew
 focus .df