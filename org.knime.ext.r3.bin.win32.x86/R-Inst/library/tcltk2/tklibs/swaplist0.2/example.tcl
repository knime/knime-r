#package require swaplist
source ./swaplist.tcl
namespace import swaplist::*

if {[swaplist .slist opts "1 2 3 4 5 6 7 8 9" "1 3 5"]} {
    puts "user chose numbers: $opts"
}