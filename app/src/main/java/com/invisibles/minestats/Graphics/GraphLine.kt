package com.invisibles.minestats.Graphics


class GraphLine(
    private var name: String = "",
    private var color: Int = 0,
    private var items: ArrayList<GraphItem> = arrayListOf(),
    private var maxOfGraph: Float = 0f
) {

    fun getName(): String = name

    fun getColor(): Int = color

    fun getItems(): ArrayList<GraphItem> = items

    fun setItems(array: ArrayList<GraphItem>){
        items = array
    }

    fun getMaxOfItems(): Float {
        var maxValue = 0f
        items.forEach { item ->
            val value = item.getValue()
            if (value > maxValue) maxValue = value
        }
        return maxValue
    }

    fun getMaxGraph(): Float {
        return if (maxOfGraph == 0f) getMaxOfItems()
        else maxOfGraph
    }
}