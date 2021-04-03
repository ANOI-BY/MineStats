package com.invisibles.minestats.Graphics


class GraphData(
    private var keys: ArrayList<String>,
    private var names: ArrayList<String>,
    private var colors: ArrayList<Int>,
    private var items: ArrayList<GraphItem>
    ) {


    fun getKeys(): ArrayList<String> { return keys }

    fun getNames(): ArrayList<String> { return names }

    fun getColors(): ArrayList<Int> { return colors }

    fun getItems(): ArrayList<GraphItem> { return items }

}
