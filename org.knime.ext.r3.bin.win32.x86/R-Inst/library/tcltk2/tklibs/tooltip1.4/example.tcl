# Demonstrate widget tooltip
#package require tooltip
source ./tooltip.tcl
pack [label .l -text "label"]
tooltip::tooltip .l "This is a label widget"

# Demonstrate menu tooltip
#package require tooltip
. configure -menu [menu .menu]
.menu add cascade -label Test -menu [menu .menu.test -tearoff 0]
.menu.test add command -label Tooltip
tooltip::tooltip .menu.test -index 0 "This is a menu tooltip"

# Demonstrate canvas item tooltip
#package require tooltip
pack [canvas .c]
set item [.c create rectangle 10 10 80 80]
tooltip::tooltip .c -item $item "Canvas item tooltip"

# Demonstrate listbox item tooltip
#package require tooltip
pack [listbox .lb]
.lb insert 0 "item one"
tooltip::tooltip .lb -item 0 "Listbox item tooltip"

# Demonstrate text tag tooltip
#package require tooltip
pack [text .txt]
.txt tag configure TIP-1 -underline 1
tooltip::tooltip .txt -tag TIP-1 "tooltip one text"
.txt insert end "An example of a " {} "tooltip" TIP-1 " tag.\n" {}