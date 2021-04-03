package com.invisibles.minestats.Graphics

class GraphLineData(private var coordinates: ArrayList<GraphLineCoordinates> = arrayListOf(), private var lineParrent: GraphLine? = null) {

    fun getCoordinates() = coordinates

    fun getLineParrent() = lineParrent

    fun setCoordinates(data: ArrayList<GraphLineCoordinates>) { coordinates = data }

}