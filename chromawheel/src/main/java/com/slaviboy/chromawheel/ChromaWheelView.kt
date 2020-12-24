package com.slaviboy.chromawheel

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.animation.doOnEnd
import com.slaviboy.colorpicker.main.ColorConverter
import java.util.*

/**
 * View that is representing chroma colors wheel. It includes the those are colors that are brighter or darker then the
 * selected color)
 */
class ChromaWheelView : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setAttributes(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setAttributes(context, attrs, defStyleAttr)
    }

    var updateShapes: Boolean = false                   // if the shapes should be updated, that include the paths for the color blocks and the overlay shadow
    var updateColors: Boolean = true                    // if colors should ne updated, that include the colors for the fill and stroke colors for the color block

    var numberOfChromaColors: Int                       // the number of chrome colors for the wheel
        set(value) {
            field = value
            updateShapes = true
            updateColors = true
            invalidate()
        }

    var spaceBetweenColorBlocks: Float                  // space between the chroma colors as percentage of the chroma block 1f=100%,0.5f=50%,0.1f=10%...
        set(value) {
            field = value
            updateShapes = true
            updateColors = true
            invalidate()
        }

    var bigCircleRadius: Float                          // big circle radius as percentage of half the biggest view side (height or width) 1f=100%,0.5f=50%,0.1f=10%...
        set(value) {
            field = value
            updateShapes = true
            updateColors = true
            invalidate()
        }

    var smallCircleRadius: Float                        // small circle radius as percentage of half the biggest view side (height or width) 1f=100%,0.5f=50%,0.1f=10%...
        set(value) {
            field = value
            updateShapes = true
            updateColors = true
            invalidate()
        }

    var colorBlockStrokeColor: Int                      // stroke color for all color blocks, in case the opacity is not 0
        set(value) {
            field = value
            invalidate()
        }

    var strokeColorDifference: Int                      // value showing how much the stroke color for each color block should differ from the fill color
        set(value) {
            field = value
            invalidate()
        }

    var selectedColor: Int                              // selected color, that is used to generate the other chroma colors
        set(value) {
            field = value
            updateColors = true
            invalidate()
        }

    var overlayShadowColor: Int                         // color for the overlay shadow, that is put on top of all color blocks expect the first block
        set(value) {
            field = value
            invalidate()
        }


    var useStrokeForFirstBlockOnly: Boolean             // if the stroke color should be applied only to the first color block
        set(value) {
            field = value
            invalidate()
        }

    val chromaFillColors: ArrayList<Int>                // array list with generated chroma fill block colors (1 integer values per color block)
    val chromaStrokeColors: ArrayList<Int>              // array list with generated chrome stroke block colors (1 integer values per color block)
    val rotationalMatrix: Matrix                        // matrix that is used to rotate the color block around the device center, to generate all color blocks from the first color block

    var centerX: Float                                  // device center x coordinate
    var centerY: Float                                  // device center y coordinate

    var degreesPerColorPack: Float                      // how many degrees is the span for each color pack (pack is made out of color block and empty space)
    var degreesPerColorBlock: Float                     // how many degrees is the span for each color block
    var degreesPerColorEmptySpace: Float                // how many degrees is the span for each empty space between the color blocks

    val overlayShadowPath: Path                         // path for the overlay shadow
    val colorBlockPath: Path                            // path for the color block

    var bigCircleActualRadius: Float                    // the actual big circle radius not as percentage of the width but as raw calculated pixels
    var smallCircleActualRadius: Float                  // the actual small circle radius not as percentage of the width but as raw calculated pixels

    lateinit var onColorSelected:
            ((v: View, clickedBlockIndex: Int) -> Unit) // listener called when the user selects new chroma color from the wheel
    lateinit var animator: ValueAnimator                // animator that is used to rotate the wheel
    var startAngle: Float                               // the angle from which to start drawing the color blocks
    var topColorBlockIndex: Int                         // the index of the color block that should be drawn on top of the overlay shadow
    var animationDuration: Long                         // the duration of the rotation animation in ms
    var fingerDownTime: Long                            // the time when the finger was pressed down, to detect click
    var isMoved: Boolean                                // if finger is moved after it was pressed down

    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {

        numberOfChromaColors = 9
        spaceBetweenColorBlocks = 0.09f

        bigCircleRadius = 1f
        smallCircleRadius = 0.5f

        colorBlockStrokeColor = Color.TRANSPARENT
        strokeColorDifference = -30

        useStrokeForFirstBlockOnly = true

        chromaFillColors = arrayListOf()
        chromaStrokeColors = arrayListOf()
        selectedColor = Color.parseColor("#34573d")

        overlayShadowColor = Color.parseColor("#80000000")

        rotationalMatrix = Matrix()

        centerX = 0f
        centerY = 0f

        startAngle = 0f
        topColorBlockIndex = 0

        degreesPerColorPack = 0f
        degreesPerColorBlock = 0f
        degreesPerColorEmptySpace = 0f

        bigCircleActualRadius = 0f
        smallCircleActualRadius = 0f
        animationDuration = 1000L
        fingerDownTime = 0L
        isMoved = false

        overlayShadowPath = Path()
        colorBlockPath = Path()
    }

    /**
     * Method called to get the xml attributes and then used them, as properties
     * for the class. Called only when the View is created using xml.
     * @param context context for the view
     * @param attrs attribute set when properties are set using the xml
     */
    fun setAttributes(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) {

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ChromaWheelView, defStyleAttr, 0)

        numberOfChromaColors = attributes.getInteger(R.styleable.ChromaWheelView_numberOfChromaColors, numberOfChromaColors)
        spaceBetweenColorBlocks = attributes.getFloat(R.styleable.ChromaWheelView_spaceBetweenColorBlocks, spaceBetweenColorBlocks)
        bigCircleRadius = attributes.getFloat(R.styleable.ChromaWheelView_bigCircleRadius, bigCircleRadius)
        smallCircleRadius = attributes.getFloat(R.styleable.ChromaWheelView_smallCircleRadius, smallCircleRadius)
        colorBlockStrokeColor = attributes.getColor(R.styleable.ChromaWheelView_colorBlockStrokeColor, colorBlockStrokeColor)
        strokeColorDifference = attributes.getInteger(R.styleable.ChromaWheelView_strokeColorDifference, strokeColorDifference)
        selectedColor = attributes.getColor(R.styleable.ChromaWheelView_selectedColor, selectedColor)
        overlayShadowColor = attributes.getColor(R.styleable.ChromaWheelView_overlayShadowColor, overlayShadowColor)
        animationDuration = attributes.getInteger(R.styleable.ChromaWheelView_animationDuration, animationDuration.toInt()).toLong()

        attributes.recycle()
    }

    /**
     * Generate new chroma color, with different tones darker and lighter using the
     * ColorConverter that has a way to convert the RGB to HSL and vise versa.
     */
    fun generateChromaColors() {

        // set current color to the converter
        val colorConverter = ColorConverter(selectedColor)
        val l = colorConverter.hsl.l

        // get how many dark and light colors should be created
        val numberOfDarkerShades = (numberOfChromaColors - 1) / 2
        val numberOfLighterShades = (numberOfChromaColors - 1) - numberOfDarkerShades

        // clear previous colors
        chromaFillColors.clear()
        addChromaColor(colorConverter, 0)

        // set the L range to [10,90], but if the selected color has color outside the range set it to [0,100]
        val startRange = if (colorConverter.hsl.l < 10) 0 else 10
        val endRange = if (colorConverter.hsl.l > 90) 0 else 100

        // generate darker shades
        var step = (l - startRange) / numberOfDarkerShades
        for (i in 0 until numberOfDarkerShades) {
            addChromaColor(colorConverter, step)
        }

        // generate lighter shades
        colorConverter.hsl.l = endRange
        step = (endRange - l) / (numberOfLighterShades + 1)
        for (i in 0 until numberOfLighterShades) {
            addChromaColor(colorConverter, step)
        }
    }

    /**
     * Add chroma fill and stroke colors
     * @param colorConverter color converter that converts RGB to HSL and vise versa
     * @param fillColorDifference difference of the L value for the HSL color model from the last L value
     */
    fun addChromaColor(colorConverter: ColorConverter, fillColorDifference: Int) {

        // set chroma fill color
        val previousL = checkLRange(colorConverter.hsl.l - fillColorDifference)
        colorConverter.hsl.l = previousL
        chromaFillColors.add(colorConverter.rgba.getInt())

        // set chroma stroke color
        colorConverter.hsl.l = checkLRange(colorConverter.hsl.l + strokeColorDifference)
        chromaStrokeColors.add(colorConverter.rgba.getInt())
        colorConverter.hsl.l = previousL
    }

    /**
     * Check the L value for the HSL color model and make sure its
     * in the range [0,100]
     */
    fun checkLRange(value: Int): Int {
        return when {
            value > 100 -> 100
            value < 0 -> 0
            else -> value
        }
    }

    /**
     * Create all paths needed to create the shapes for the color block and the
     * overlay shadow
     */
    fun createPaths() {

        // set center of the device
        centerX = width / 2f
        centerY = height / 2f

        val radius = Math.min(centerX, centerY)

        val bigCirclePath = Path()
        bigCircleActualRadius = radius * bigCircleRadius
        bigCirclePath.addCircle(centerX, centerY, bigCircleActualRadius, Path.Direction.CW)

        val smallCirclePath = Path()
        smallCircleActualRadius = radius * smallCircleRadius
        smallCirclePath.addCircle(centerX, centerY, smallCircleActualRadius, Path.Direction.CW)

        // reset previous paths
        colorBlockPath.reset()
        overlayShadowPath.reset()
        overlayShadowPath.op(bigCirclePath, smallCirclePath, Path.Op.XOR)

        // get the degrees per pak, block and empty space
        degreesPerColorPack = 360f / numberOfChromaColors
        degreesPerColorBlock = degreesPerColorPack / (1 + spaceBetweenColorBlocks)
        degreesPerColorEmptySpace = degreesPerColorPack - degreesPerColorBlock
        startAngle = -degreesPerColorBlock / 2f

        // get the (centerX, -radius)
        val x = centerX
        val y = centerY - 2 * bigCircleActualRadius

        // get the rotated point
        val rotatedPoint = rotate(centerX, centerY, x, y, degreesPerColorBlock)

        val linePath = Path()
        linePath.moveTo(centerX, centerY)
        linePath.lineTo(x, y)
        linePath.lineTo(rotatedPoint.x, rotatedPoint.y)

        colorBlockPath.op(overlayShadowPath, linePath, Path.Op.INTERSECT)
    }

    /**
     * Draw a color block at given index, at index 0 is the first color block
     * @param canvas canvas where the path for the color block will be drawn
     * @param i index of the color block, it determine the fill and stroke color for the color block
     */
    fun drawColorBlock(canvas: Canvas, i: Int) {

        // draw fill block
        paint.apply {
            color = chromaFillColors[i]
            style = Paint.Style.FILL
        }
        canvas.drawPath(colorBlockPath, paint)

        // draw stroke block
        if (useStrokeForFirstBlockOnly && i != 0) {
            return
        }
        val strokeColor = when {

            // first try to use the chroma strokes
            strokeColorDifference != 0 -> {
                chromaStrokeColors[i]
            }

            // try to use the stoke for all blocks
            Color.alpha(colorBlockStrokeColor) > 0 -> {
                colorBlockStrokeColor
            }

            // set the color to transparent-0
            else -> {
                Color.TRANSPARENT
            }
        }
        if (strokeColor != Color.TRANSPARENT) {
            paint.apply {
                color = strokeColor
                style = Paint.Style.STROKE
            }
            canvas.drawPath(colorBlockPath, paint)
        }
    }

    override fun onDraw(canvas: Canvas) {

        if (updateShapes) {
            createPaths()
            updateShapes = false
        }

        if (updateColors) {
            generateChromaColors()
            updateColors = false
        }

        // rotate opposite direction half the degree per color block
        rotationalMatrix.setRotate(startAngle, centerX, centerY)

        // draw all color blocks, except the first one, that is drawn on top of the overlay shadow
        for (i in 0 until numberOfChromaColors) {

            // apply rotation to the matrix
            if (i > 0) rotationalMatrix.postRotate(degreesPerColorPack, centerX, centerY)

            // skip the element that will be on top of the overlay shadow
            if (i == topColorBlockIndex) {
                continue
            }

            canvas.setMatrix(rotationalMatrix)
            drawColorBlock(canvas, i)
        }

        // draw the overlay shadow
        if (Color.alpha(overlayShadowColor) > 0) {
            paint.apply {
                color = overlayShadowColor
                style = Paint.Style.FILL
            }
            canvas.drawPath(overlayShadowPath, paint)
        }

        // draw the color block that is on top of the overlay shadow
        rotationalMatrix.setRotate(startAngle + topColorBlockIndex * degreesPerColorPack, centerX, centerY)
        canvas.setMatrix(rotationalMatrix)
        drawColorBlock(canvas, topColorBlockIndex)

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                // get the time when the finger is pressed down, for detecting click event
                fingerDownTime = System.currentTimeMillis()
            }

            MotionEvent.ACTION_UP -> {

                // get the time when the finger is pressed down, for detecting click event
                val passedTime: Long = System.currentTimeMillis() - fingerDownTime

                if (!isMoved && passedTime < 500) {


                    val halfWidth = width / 2f
                    val halfHeight = height / 2f
                    val r = distancePointToPoint(halfWidth, halfHeight, event.x, event.y)

                    if (r >= smallCircleActualRadius && r <= bigCircleActualRadius) {

                        val halfDegreesPerColorBlock = (degreesPerColorBlock / 2f)
                        val a = 90f + halfDegreesPerColorBlock
                        val b = 90f - halfDegreesPerColorBlock
                        val c = angleBetweenTwoPoints(width / 2f, height / 2f, event.x, event.y)

                        // range for the color block, if the finger is inside this range
                        // then it is inside that color block
                        val rangeMin = 0
                        val rangeMax = a - b

                        val angle = if (c < b) 360 - b + c else (c - b)
                        val rangeValue = angle % degreesPerColorPack
                        if (rangeValue >= rangeMin && rangeValue <= rangeMax) {

                            // index of the color block that was clicked
                            var clickedBlockIndex = (angle / degreesPerColorPack).toInt()
                            if (clickedBlockIndex > 0) clickedBlockIndex = chromaFillColors.size - clickedBlockIndex

                            // get the rotation distance if the value is negative the list is rotate anti-clockwise
                            // otherwise it is rotated clockwise, value indicate how many time it should rotate
                            val rotateDistance = when {
                                clickedBlockIndex == 0 -> {
                                    0
                                }
                                clickedBlockIndex <= (chromaFillColors.size - 1) / 2 -> {
                                    -clickedBlockIndex
                                }
                                else -> {
                                    chromaFillColors.size - clickedBlockIndex
                                }
                            }

                            if ((!::animator.isInitialized) || !animator.isRunning) {

                                topColorBlockIndex = clickedBlockIndex

                                // make sure the duration of the animation is bigger than zero, before creating it
                                if (animationDuration <= 0L) {
                                    onAnimationEnd(rotateDistance, clickedBlockIndex)
                                } else {

                                    // store previous stroke color, and remove it while the animation is running and the wheel is spinning
                                    val previousColorBlockStrokeColor = colorBlockStrokeColor
                                    val previousStrokeColorDifference = strokeColorDifference
                                    if (clickedBlockIndex != 0) {
                                        colorBlockStrokeColor = Color.TRANSPARENT
                                        strokeColorDifference = 0
                                    }

                                    animator = ValueAnimator.ofFloat(startAngle, startAngle + rotateDistance * degreesPerColorPack)
                                    animator.addUpdateListener { animation ->

                                        // update the start angle and force redrawing of the view
                                        // that way the wheel is rotated
                                        startAngle = animation.animatedValue as Float
                                        invalidate()
                                    }
                                    animator.doOnEnd {

                                        // restore values
                                        colorBlockStrokeColor = previousColorBlockStrokeColor
                                        strokeColorDifference = previousStrokeColorDifference

                                        onAnimationEnd(rotateDistance, clickedBlockIndex)
                                    }
                                    animator.duration = animationDuration
                                    animator.start()
                                }
                            }
                        }
                    }
                }

                isMoved = false
            }

            MotionEvent.ACTION_MOVE -> {
                isMoved = true
            }
        }

        return true
    }

    fun onAnimationEnd(rotateDistance: Int, clickedBlockIndex: Int) {

        topColorBlockIndex = 0
        startAngle = -degreesPerColorBlock / 2f

        // rotate the element of the list to match the new first element
        Collections.rotate(chromaFillColors, rotateDistance)
        invalidate()

        // call the listener indicating that new color is selected from the wheel
        if (::onColorSelected.isInitialized) {
            onColorSelected.invoke(this, clickedBlockIndex)
        }
    }

    fun setOnColorSelectedListener(method: (v: View, clickedBlockIndex: Int) -> Unit) {
        onColorSelected = method
    }

    companion object {

        /**
         * Rotate a point around a center with given angle
         * @param cx rotary center point x coordinate
         * @param cy rotary center point y coordinate
         * @param x x coordinate of the point that will be rotated
         * @param y y coordinate of the point that will be rotated
         * @param angle angle of rotation in degrees
         * @param anticlockWise rotate clockwise or anti-clockwise
         * @param resultPoint object where the result rotational point will be stored
         */
        fun rotate(cx: Float, cy: Float, x: Float, y: Float, angle: Float, anticlockWise: Boolean = false, resultPoint: PointF = PointF()): PointF {

            if (angle == 0f) {
                resultPoint.x = x
                resultPoint.y = y
                return resultPoint
            }

            val radians = if (anticlockWise) {
                (Math.PI / 180) * angle
            } else {
                (Math.PI / -180) * angle
            }

            val cos = Math.cos(radians)
            val sin = Math.sin(radians)
            val nx = (cos * (x - cx)) + (sin * (y - cy)) + cx
            val ny = (cos * (y - cy)) - (sin * (x - cx)) + cy

            resultPoint.x = nx.toFloat()
            resultPoint.y = ny.toFloat()
            return resultPoint
        }

        /**
         * Find the angle between two points
         * @param cx x coordinate of the center point
         * @param cy y coordinate of the center point
         * @param x x coordinate of the point that is rotating
         * @param y y coordinate of the point that is rotating
         */
        fun angleBetweenTwoPoints(cx: Float, cy: Float, x: Float, y: Float): Float {

            val dy = y - cy
            val dx = x - cx
            var angle = Math.atan2(dy.toDouble(), dx.toDouble())      // range (-PI, PI]
            angle *= 180 / Math.PI                                    // radians to degrees, range (-180, 180]
            angle = if (angle < 0) Math.abs(angle) else 360 - angle   // range [0, 360)

            return angle.toFloat()
        }

        /**
         * Distance between two points
         * @param x1 x coordinate of first point
         * @param y1 y coordinate of first point
         * @param x2 x coordinate of second point
         * @param y2 y coordinate of second point
         */
        fun distancePointToPoint(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = (x1 - x2)
            val dy = (y1 - y2)
            return Math.sqrt(dx.toDouble() * dx + dy * dy).toFloat()
        }
    }
}