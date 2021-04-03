package com.invisibles.minestats.Graphics

class GraphItem(private var time: Long, private var value: Float) {

    fun getTime(): Long { return time }

    fun getValue(): Float { return value }

}