package com.invisibles.minestats.Graphics

class GraphLineCoordinates(private var x: Float, private var y: Float) {

    fun getX() = x

    fun getY() = y

    override fun toString(): String {
        return "$x $y"
    }

}