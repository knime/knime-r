source ./tk_getString.tcl
#package require getstring
namespace import getstring::*

if {[tk_getString .gs text "Feed me a string please:"]} {
    puts "user entered: $text"
}