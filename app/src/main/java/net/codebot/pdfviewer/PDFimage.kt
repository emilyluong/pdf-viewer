package net.codebot.pdfviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.widget.ImageView


@SuppressLint("AppCompatCustomView")
class PDFimage(context: Context?) : ImageView(context) {

    class StrokeInfo(path: Path, paint: Paint) {
        var path = path
        var paint = paint
    }

    // tool used
    var tool = "draw"

    // drawing path
    var path = Path()
    var paths = ArrayList<StrokeInfo?>()

    // image to display
    var bitmap: Bitmap? = null
    var paint = Paint()

    var pathRedoList = ArrayList<StrokeInfo?>()

    var x1 = 0f
    var x2 = 0f
    var y1 = 0f
    var y2 = 0f
    var old_x1 = 0f
    var old_y1 = 0f
    var old_x2 = 0f
    var old_y2 = 0f
    var mid_x = -1f
    var mid_y = -1f
    var old_mid_x = -1f
    var old_mid_y = -1f
    var p1_id = 0
    var p1_index = 0
    var p2_id = 0
    var p2_index = 0

    // store cumulative transformations
    // the inverse matrix is used to align points with the transformations - see below
    var currentMatrix = Matrix()
    var inverse = Matrix()

    init {
        setBrush()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var inverted: FloatArray
        when (event.pointerCount) {
            1 -> {
                p1_id = event.getPointerId(0)
                p1_index = event.findPointerIndex(p1_id)

                // invert using the current matrix to account for pan/scale
                // inverts in-place and returns boolean
                inverse = Matrix()
                currentMatrix.invert(inverse)

                // mapPoints returns values in-place
                inverted = floatArrayOf(event.getX(p1_index), event.getY(p1_index))
                inverse.mapPoints(inverted)
                x1 = inverted[0]
                y1 = inverted[1]
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (tool == "erase") {
                            paint.color = Color.TRANSPARENT
                            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        }
                        path = Path()
                        paths.add(StrokeInfo(path, paint))
                        path!!.moveTo(x1, y1)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (tool == "erase") {
                            paint.color = Color.TRANSPARENT
                            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        }
                        path!!.lineTo(x1, y1)
                    }
                }
            }
            2 -> {
                // point 1
                p1_id = event.getPointerId(0)
                p1_index = event.findPointerIndex(p1_id)

                // mapPoints returns values in-place
                inverted = floatArrayOf(event.getX(p1_index), event.getY(p1_index))
                inverse.mapPoints(inverted)

                // first pass, initialize the old == current value
                if (old_x1 < 0 || old_y1 < 0) {
                    x1 = inverted.get(0)
                    old_x1 = x1
                    y1 = inverted.get(1)
                    old_y1 = y1
                } else {
                    old_x1 = x1
                    old_y1 = y1
                    x1 = inverted.get(0)
                    y1 = inverted.get(1)
                }

                // point 2
                p2_id = event.getPointerId(1)
                p2_index = event.findPointerIndex(p2_id)

                // mapPoints returns values in-place
                inverted = floatArrayOf(event.getX(p2_index), event.getY(p2_index))
                inverse.mapPoints(inverted)

                // first pass, initialize the old == current value
                if (old_x2 < 0 || old_y2 < 0) {
                    x2 = inverted.get(0)
                    old_x2 = x2
                    y2 = inverted.get(1)
                    old_y2 = y2
                } else {
                    old_x2 = x2
                    old_y2 = y2
                    x2 = inverted.get(0)
                    y2 = inverted.get(1)
                }

                // midpoint
                mid_x = (x1 + x2) / 2
                mid_y = (y1 + y2) / 2
                old_mid_x = (old_x1 + old_x2) / 2
                old_mid_y = (old_y1 + old_y2) / 2

                // distance
                val d_old =
                    Math.sqrt(Math.pow((old_x1 - old_x2).toDouble(), 2.0) + Math.pow((old_y1 - old_y2).toDouble(), 2.0))
                        .toFloat()
                val d = Math.sqrt(Math.pow((x1 - x2).toDouble(), 2.0) + Math.pow((y1 - y2).toDouble(), 2.0))
                    .toFloat()

                // pan and zoom during MOVE event
                if (event.action == MotionEvent.ACTION_MOVE) {
                    // pan == translate of midpoint
                    val dx = mid_x - old_mid_x
                    val dy = mid_y - old_mid_y
                    currentMatrix.preTranslate(dx, dy)

                    // zoom == change of spread between p1 and p2
                    var scale = d / d_old
                    scale = Math.max(0f, scale)
                    currentMatrix.preScale(scale, scale, mid_x, mid_y)

                    // reset on up
                } else if (event.action == MotionEvent.ACTION_UP) {
                    old_x1 = -1f
                    old_y1 = -1f
                    old_x2 = -1f
                    old_y2 = -1f
                    old_mid_x = -1f
                    old_mid_y = -1f
                }
            }
            else -> {
            }
        }
        return true
    }

    // set image as background
    fun setImage(bitmap: Bitmap?) {
        this.bitmap = bitmap
    }

    // undo last path
    fun undo() {
        if (paths.size > 0 && pathRedoList.size < 5) {
            val undoPath = paths.removeLast()
            pathRedoList.add(undoPath)
            invalidate()
        }
    }

    // redo
    fun redo() {
        if (pathRedoList.size > 0) {
            val redoPath = pathRedoList.removeLast()
            paths.add(redoPath)
            invalidate()
        }
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    fun setBrush() {
        paint.color = Color.BLACK
        paint.isAntiAlias = true
        paint.alpha = 255
        paint.strokeWidth = 5.0F
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        // apply transformations from the event handler above
        canvas.concat(currentMatrix)

        // draw background
        if (bitmap != null) {
            setImageBitmap(bitmap)
        }

        // draw lines over it
        for (i in paths.indices) {
            val strokePath = paths[i]!!.path
            val strokePaint = paths[i]!!.paint

            canvas.drawPath(strokePath, strokePaint)
        }

        super.onDraw(canvas)
    }
}