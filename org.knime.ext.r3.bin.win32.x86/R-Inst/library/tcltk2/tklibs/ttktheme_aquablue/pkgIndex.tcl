# Package index for tile demo pixmap themes.

if {[file isdirectory [file join $dir aquablue]]} {
    if {[package vsatisfies [package require tile] 0.8.0]} {
        package ifneeded ttk::theme::aquablue 0.1 \
            [list source [file join $dir aquablue8.5.tcl]]
    } else {
        package ifneeded tile::theme::aquablue 0.1 \
            [list source [file join $dir aquablue8.4.tcl]]
    }
}

