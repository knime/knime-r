# Package index for tile demo pixmap themes.

if {[file isdirectory [file join $dir radiance]]} {
    if {[package vsatisfies [package require tile] 0.8.0]} {
        package ifneeded ttk::theme::radiance 0.1 \
            [list source [file join $dir radiance8.5.tcl]]
    } else {
        package ifneeded tile::theme::radiance 0.1 \
            [list source [file join $dir radiance8.4.tcl]]
    }
}

