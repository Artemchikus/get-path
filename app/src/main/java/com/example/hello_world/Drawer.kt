package com.example.hello_world

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

data class RelVector(var x: Float, var y: Float, var z: Float, var len: Float)
class Drawer: AppCompatImageView, View.OnTouchListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private var mContext: Context? = null
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    var mMatrix: Matrix? = null
    private var mMatrixValues: FloatArray? = null
    var mode = NONE
    var mSaveScale = 1f
    var mMinScale = 1f
    var mMaxScale = 4f
    var origWidth = 0f
    var origHeight = 0f
    var viewWidth = 0
    var viewHeight = 0
    private var mLast = PointF()
    private var mStart = PointF()
    private lateinit var bitmap: Bitmap
    private lateinit var baseBitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var painter: Paint
    private lateinit var eraser: Paint
    private var chosenPoint = PointF(
        2000f, 660f
    )
    private var isDrawing = false
    private val myCoroutineScope = CoroutineScope(Dispatchers.Main)
    private var relVectorChannel: Channel<RelVector>? = null
    private var drawPointStart = PointF()

    constructor(context: Context) : super(context) {
        sharedConstructing(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        sharedConstructing(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)

    private fun sharedConstructing(context: Context) {
        super.setClickable(true)
        mContext = context
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mMatrix = Matrix()
        mMatrixValues = FloatArray(9)
        imageMatrix = mMatrix
        scaleType = ScaleType.MATRIX
        mGestureDetector = GestureDetector(context, this)
        setOnTouchListener(this)

        bitmap = BitmapFactory.decodeResource(resources, R.drawable.image1flor).copy(Bitmap.Config.ARGB_8888, true)
        canvas = Canvas(bitmap)
        baseBitmap = BitmapFactory.decodeResource(resources, R.drawable.image1flor).copy(Bitmap.Config.ARGB_8888, true)

        painter = Paint()
        painter.color = Color.RED
        painter.strokeWidth = 10F

        eraser = Paint()
        eraser.color = Color.TRANSPARENT
        eraser.strokeWidth = 10F
        eraser.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))

        setImageBitmap(bitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.concat(imageMatrix)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val prevScale = mSaveScale
            mSaveScale *= mScaleFactor
            if (mSaveScale > mMaxScale) {
                mSaveScale = mMaxScale
                mScaleFactor = mMaxScale / prevScale
            } else if (mSaveScale < mMinScale) {
                mSaveScale = mMinScale
                mScaleFactor = mMinScale / prevScale
            }
            if (origWidth * mSaveScale <= viewWidth
                || origHeight * mSaveScale <= viewHeight) {
                mMatrix!!.postScale(mScaleFactor, mScaleFactor, viewWidth / 2.toFloat(),
                    viewHeight / 2.toFloat())
            } else {
                mMatrix!!.postScale(mScaleFactor, mScaleFactor,
                    detector.focusX, detector.focusY)
            }
            fixTranslation()
            return true
        }
    }

    private fun fitToScreen() {
        mSaveScale = 1f
        val scale: Float
        val drawable = drawable
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }

        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight
        val scaleX = viewWidth.toFloat() / imageWidth.toFloat()
        val scaleY = viewHeight.toFloat() / imageHeight.toFloat()
        scale = scaleX.coerceAtMost(scaleY)
        mMatrix!!.setScale(scale, scale)

        var redundantYSpace = (viewHeight.toFloat() - scale * imageHeight.toFloat())
        var redundantXSpace = (viewWidth.toFloat() - scale * imageWidth.toFloat())
        redundantYSpace /= 2.toFloat()
        redundantXSpace /= 2.toFloat()
        mMatrix!!.postTranslate(redundantXSpace, redundantYSpace)
        origWidth = viewWidth - 2 * redundantXSpace
        origHeight = viewHeight - 2 * redundantYSpace
        imageMatrix = mMatrix
    }

    fun fixTranslation() {
        mMatrix!!.getValues(mMatrixValues)
        val transX = mMatrixValues!![Matrix.MTRANS_X]
        val transY = mMatrixValues!![Matrix.MTRANS_Y]
        val fixTransX = getFixTranslation(transX, viewWidth.toFloat(), origWidth * mSaveScale)
        val fixTransY = getFixTranslation(transY, viewHeight.toFloat(), origHeight * mSaveScale)
        if (fixTransX != 0f || fixTransY != 0f) mMatrix!!.postTranslate(fixTransX, fixTransY)
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) {
            return -trans + minTrans
        }
        if (trans > maxTrans) {
            return -trans + maxTrans
        }
        return 0F
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0F
        } else delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (mSaveScale == 1f) {
            fitToScreen()
        }
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        mScaleDetector!!.onTouchEvent(event)
        mGestureDetector!!.onTouchEvent(event)
        val currentPoint = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mLast.set(currentPoint)
                mStart.set(mLast)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                val dx = currentPoint.x - mLast.x
                val dy = currentPoint.y - mLast.y
                val fixTransX = getFixDragTrans(dx, viewWidth.toFloat(), origWidth * mSaveScale)
                val fixTransY = getFixDragTrans(dy, viewHeight.toFloat(), origHeight * mSaveScale)
                mMatrix!!.postTranslate(fixTransX, fixTransY)
                fixTranslation()
                mLast[currentPoint.x] = currentPoint.y
            }
            MotionEvent.ACTION_POINTER_UP -> mode = NONE
        }
        imageMatrix = mMatrix
        return false
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(motionEvent: MotionEvent) {}
    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, motionEvent: MotionEvent, v: Float, v1: Float): Boolean {
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, motionEvent: MotionEvent, v: Float, v1: Float): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
        if (isDrawing) {
            return true
        }

        val values = FloatArray(9)
        imageMatrix.getValues(values)

        val newX = (motionEvent.x - values[Matrix.MTRANS_X]) / values[Matrix.MSCALE_X]
        val newY = (motionEvent.y -  values[Matrix.MTRANS_Y]) / values[Matrix.MSCALE_Y]
        println("chosenPoint: $chosenPoint")

        if (chosenPoint.x != 0f) {
            canvas.drawPoint(chosenPoint.x, chosenPoint.y, eraser)
        }

        chosenPoint = PointF(newX, newY)

        canvas.drawPoint(newX, newY, painter)

        invalidate()
        return true
    }

    override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
        fitToScreen()
        return false
    }

    override fun onDoubleTapEvent(motionEvent: MotionEvent): Boolean {
        return false
    }

    fun StartDrawing(dataChan: Channel<RelVector>): Boolean {
        isDrawing = true
        relVectorChannel = dataChan

//        if (chosenPoint.x == 0f) {
//            return false
//        }

        drawPointStart.x = chosenPoint.x
        drawPointStart.y = chosenPoint.y

        myCoroutineScope.launch {
            for (vec in dataChan)  {
                val vectorLength = vec.len * 5// Length of the vector for display purposes
                val newX = drawPointStart.x + (vec.x * vectorLength)
                val newY = drawPointStart.y - (vec.y * vectorLength)

                println("newX: $newX")
                println("newY: $newY")

                canvas.drawLine(drawPointStart.x,  drawPointStart.y, newX, newY, painter)
                invalidate()

                drawPointStart.x = newX
                drawPointStart.y = newY
            }
        }

        return true
    }

    fun EndDrawing(): Boolean {
        isDrawing = false
        relVectorChannel?.close()

        canvas.drawBitmap(baseBitmap, 0f, 0f, null)
        invalidate()
        return true
    }

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
    }
}