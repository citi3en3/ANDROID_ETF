package com.iurie.etfwatch.ui.common

enum class SortMode(val label: String) {
    Sector("Sector"),
    MonthReturn("1M %"),
    TwoMonthReturn("2M %"),
    Week1Return("1W %"),
    Week2Return("2W %"),
    Week3Return("3W %"),
    Week5Return("5W %"),
    Ticker("A→Z"),
    Inverse("Inverse"),
    NonInverse("Non-Inverse"),
    Lev1x("1x"),
    Lev2x("2x"),
    Lev3x("3x"),
    Price("Price"),
    ChangePct("Day %"),
    Yield("Yield");

    /** Short label for inline use, e.g. "1M" rather than "1M %". */
    val shortLabel: String get() = label.removeSuffix(" %")
}
