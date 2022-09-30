package net.codebot.pdfviewer

import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivityViewModel(private val state: SavedStateHandle) : ViewModel() {

    // manage the pages of the PDF, see below
    var pdfRenderer: MutableLiveData<PdfRenderer> = MutableLiveData()
    var parcelFileDescriptor: MutableLiveData<ParcelFileDescriptor> = MutableLiveData()
    var currentPage: MutableLiveData<PdfRenderer.Page> = MutableLiveData()

    // custom ImageView class that captures strokes and draws them over the image
    val currentPageImage: MutableLiveData<PDFimage> = MutableLiveData()

    // captures all the drawing of each PDF page
    var pageIndexToPathListMap = MutableLiveData(HashMap<Int, ArrayList<PDFimage.StrokeInfo>>())

    //current index of page
    var pageIndex = MutableLiveData(0)
    var totalPages = MutableLiveData(0)

    fun showPathsOfCurrentPage() {
        if (pageIndexToPathListMap.value!!.size > 0 && pageIndexToPathListMap.value!!.containsKey(pageIndex.value!!)) {
            val paths = pageIndexToPathListMap.value!![pageIndex.value!!]
            currentPageImage.value!!.paths = paths as ArrayList<PDFimage.StrokeInfo?>
            currentPageImage.value!!.invalidate()
        }
    }

    fun addAndClearPathListToCurrentIndex() {
        val pdfPathList = ArrayList(currentPageImage.value!!.paths)
        if (pdfPathList != null && pdfPathList.size > 0) {
            if (pageIndexToPathListMap.value != null && pageIndexToPathListMap.value!!.containsKey(pageIndex.value!!)) {
                if (pdfPathList != pageIndexToPathListMap.value!![pageIndex.value]!!) {
                    pageIndexToPathListMap.value!![pageIndex.value]!!.addAll(pdfPathList as Collection<PDFimage.StrokeInfo>)
                }
            } else {
                pageIndexToPathListMap.value!![pageIndex.value!!] = pdfPathList as ArrayList<PDFimage.StrokeInfo>
            }
            currentPageImage.value!!.paths = ArrayList()
            currentPageImage.value!!.path = Path()
        }
    }

    fun setCurrentPDFTool(tool : String) {
        if (tool == "undo") {
            currentPageImage.value!!.undo()
        } else if (tool == "redo") {
            currentPageImage.value!!.redo()
        } else {
            currentPageImage.value!!.tool = tool
            currentPageImage.value!!.paint = getToolBrush(tool)
        }
    }

    fun getToolBrush(tool: String): Paint {
        when(tool) {
            "draw" -> {
                val paint = Paint(Color.BLACK)
                paint.isAntiAlias = true
                paint.alpha = 255
                paint.strokeWidth = 5.0F
                paint.style = Paint.Style.STROKE
                paint.strokeJoin = Paint.Join.ROUND
                paint.strokeCap = Paint.Cap.ROUND
                return paint
            }
            "highlight" -> {
                val paint = Paint()
                paint.isAntiAlias = true
                paint.color = Color.YELLOW
                paint.alpha = 100
                paint.strokeWidth = 50.0F
                paint.style = Paint.Style.STROKE
                paint.strokeJoin = Paint.Join.ROUND
                paint.strokeCap = Paint.Cap.ROUND
                return paint
            }
            "erase" -> {
                val paint = Paint()
                paint.isAntiAlias = true
                paint.alpha = 0
                paint.color = Color.TRANSPARENT
                paint.strokeWidth = 50.0F
                paint.style = Paint.Style.STROKE
                paint.strokeJoin = Paint.Join.ROUND
                paint.strokeCap = Paint.Cap.ROUND
                return paint
            }
        }
        return Paint()
    }

    fun setPDFImage(pdfPage: PDFimage) {
        pdfPage.minimumWidth = 1000
        pdfPage.minimumHeight = 2000

        currentPageImage.value = pdfPage
    }

    fun setPDFRenderer(pdfRenderer: PdfRenderer) {
        this.pdfRenderer.value = pdfRenderer
    }

    fun setCurrentPDFPage(page : PdfRenderer.Page?) {
        currentPage?.value = page
    }

    fun setPDFPageCount(count : Int) {
        totalPages.value = count
    }

    fun setPDFParcelFileDescriptor(parcelFileDescriptor : ParcelFileDescriptor) {
        this.parcelFileDescriptor.value = parcelFileDescriptor
    }

    fun setPageIndex(index: Int) {
        pageIndex.value = index
    }
}