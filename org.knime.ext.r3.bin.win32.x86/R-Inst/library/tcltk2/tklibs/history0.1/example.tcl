source ./history.tcl
entry .e
bind .e <Return> [list ProcessEntry %W]
::history::init .e
pack .e

proc ProcessEntry {w} {
    set text [$w get]
    if {$text == ""} { return }
    ::history::add $w $text
    puts $text
    $w delete 0 end
}
