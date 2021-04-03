package com.invisibles.minestats.Graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.invisibles.minestats.Utils
import kotlin.collections.ArrayList

class GraphicHash : View {

    private var selectedSectors = ArrayList<Path>()
    private var statsRect = Path()

    private var width: Float = 0f
    private var height: Float = 0f

    private var RECT_A = 50f
    private var RECT_B = 100f

    private var data: ArrayList<GraphLine>? = null
    private var lineCoordinates = ArrayList<GraphLineData>()
    private var fullDate = ArrayList<String>()


    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr){}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = measuredWidth.toFloat()
        height = measuredHeight.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data != null){
            drawBackgroundGrid(canvas)
            drawLines(canvas)
            drawSelectedSectors(canvas)

        }

    }

    private fun drawSelectedSectors(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.RED
        }

        if (selectedSectors.size != 0) {
            selectedSectors.forEach {
                canvas.drawPath(it, paint)
            }
            canvas.drawPath(statsRect, Paint().apply { Color.GRAY; style = Paint.Style.STROKE; strokeWidth = 10f })
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        val action = event.action
        val pointX = event.x


        when (action){
            MotionEvent.ACTION_DOWN  -> touchEventDown(pointX)
            MotionEvent.ACTION_UP -> touchEventUp()
            MotionEvent.ACTION_MOVE -> touchEventDown(pointX)
        }

        return true
    }

    private fun touchEventDown(pointX: Float){
        val sectorSize = width / 25f

        if (lineCoordinates.size == data?.size && pointX < width && pointX >= 0){
            selectedSectors = arrayListOf()
            val sector = Math.floor((pointX / sectorSize).toDouble()).toInt()
            //Log.i("GRAPH", "$pointX, $sector")
            statsRect = Path()
            lineCoordinates.forEach {
                val line = it.getLineParrent()
                val lineSector = it.getCoordinates()[sector]
                val lineX = lineSector.getX()
                val lineY = lineSector.getY()

                val path = Path()
                path.addCircle(lineX, lineY, 10f, Path.Direction.CCW)
                selectedSectors.add(path)

                statsRect = getStatusMenu(lineX)
            }
            visibility = GONE
            visibility = VISIBLE
        }
    }

    private fun touchEventUp(){
        selectedSectors = arrayListOf()
        visibility = GONE
        visibility = VISIBLE
    }

    private fun getStatusMenu(lineX: Float): Path {

        val path = Path()
        var pointX1 = 0f
        var pointX2 = 0f
        var height1 = 0f
        var height2 = 0f

        if (lineX > 500 || lineX + 500 > width) {
            pointX1 = lineX - 500
            pointX2 = lineX - 50
            height1 = height/2 - 150
            height2 = height/2 + 150
        }
        else if (lineX < 500 && lineX < width) {
            pointX2 = lineX + 500
            pointX1 = lineX + 50
            height2 = height/2 + 150
            height1 = height/2 - 150

        }

        //Log.i("GRAPH", "$pointX1, $height1, $pointX2, $height2")

        path.addRect(RectF(pointX1, height1, pointX2, height2), Path.Direction.CCW)
        return path
    }

    private fun drawLines(canvas: Canvas){
        val paddingX = width / 24

        lineCoordinates = arrayListOf()

        data?.forEach { line ->
            val lineArray = arrayListOf<GraphLineCoordinates>()
            val items = line.getItems()
            var paddingNowX = 0f
            val paint = Paint().apply {
                color = line.getColor()
                strokeWidth = 8f
                style = Paint.Style.STROKE
            }

            val path = Path()

            for (i in 0 until items.size){
                val item = items.get(i)
                val value = item.getValue()
                val cof = ((height-135) / line.getMaxGraph())
                val heightGraph = (height-68) - cof * value

                if (i == 0) path.moveTo(paddingNowX, heightGraph)
                else path.lineTo(paddingNowX, heightGraph)

                lineArray.add(GraphLineCoordinates(paddingNowX, heightGraph))

                paddingNowX += paddingX

            }

            canvas.drawPath(path, paint)
            lineCoordinates.add(GraphLineData(lineArray, line))
        }
    }

    private fun drawBackgroundGrid(canvas: Canvas){
        val paddingYHash = height / 6
        val paddingScreenTime = width / 8

        lateinit var lineWithHashrate: GraphLine
        data?.forEach { line ->
            if (line.getName() == "hashrate") lineWithHashrate = line
        }

        val maxHashrate = lineWithHashrate.getMaxOfItems()
        val cof = maxHashrate / 5
        var hashNum = 0f
        var paddingNowY = height-68
        var paddingNowX = 0f

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
        }

        var perNum = 0
        var perPadding = 15

        //Vertical grid
        for (i in 0 until 6){
            var hashNumStr = hashNum.toString()
            val hashNumsUntilDot = Utils.getLenNumUntilDot(hashNum.toDouble())
            //Log.i("API", "$hashNum hashnum \t $hashNumsUntilDot")
            if (hashNumStr == "0.0") hashNumStr = "0.00"
            else hashNumStr = hashNumStr.substring(0, hashNumsUntilDot+3)

            canvas.drawLine(0f, paddingNowY, width, paddingNowY, paint)
            canvas.drawText(hashNumStr, 0f, paddingNowY-4, paint)
            canvas.drawText("$perNum", width-perPadding, paddingNowY-4, paint)
            paddingNowY -= paddingYHash
            perNum += 20
            hashNum += cof
            if (perNum.toString().length == 2) perPadding = 30
            else if (perNum.toString().length == 3) perPadding = 50
        }

        //Horizontal grid
        lineWithHashrate.getItems().forEach { item ->
            val time = Utils.getTime(item.getTime())
            fullDate.add(time)
        }


        for (i in 1 until fullDate.size step 3){
            val hours = fullDate.get(i).split(" ").get(1).substring(0, 5)
            canvas.drawText(hours, paddingNowX+30, height-40, paint)
            paddingNowX += paddingScreenTime
        }

    }

    fun setData(data: ArrayList<GraphLine>){
        this.data = data
    }

}