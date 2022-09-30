package net.codebot.pdfviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Code references:
// Used the starter code given in CS349
// Used the pan and zoom code from CS349

class MainActivity : AppCompatActivity() {

    // create view model using delegation
    private val viewModel: MainActivityViewModel by viewModels()

    val LOGNAME = "pdf_viewer"
    val FILENAME = "shannon1948.pdf"
    val FILERESID = R.raw.shannon1948

    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val layout = findViewById<LinearLayout>(R.id.pdfLayout)
        layout.isEnabled = true

        viewModel.setPDFImage(PDFimage(this))
        layout.addView(viewModel.currentPageImage.value)

        title = FILENAME

        setOnClickToNavigationArrows()
        setOnClickTools()

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this)
            viewModel.setPDFPageCount(viewModel.pdfRenderer.value!!.pageCount)
            showPage(0)
            closeRenderer()
        } catch (exception: IOException) {
            Log.d(LOGNAME, "Error opening PDF")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            closeRenderer()
        } catch (ex: IOException) {
            Log.d(LOGNAME, "Unable to close PDF renderer")
        }
    }

    @Throws(IOException::class)
    private fun openRenderer(context: Context) {
        // In this sample, we read a PDF from the assets directory.
        val file = File(context.cacheDir, FILENAME)
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            val asset = this.resources.openRawResource(FILERESID)
            val output = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var size: Int
            while (asset.read(buffer).also { size = it } != -1) {
                output.write(buffer, 0, size)
            }
            asset.close()
            output.close()
        }
        viewModel.setPDFParcelFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (viewModel.parcelFileDescriptor != null) {
            viewModel.setPDFRenderer(PdfRenderer(viewModel.parcelFileDescriptor.value!!))
        }
    }

    // do this before you quit!
    @Throws(IOException::class)
    private fun closeRenderer() {
        viewModel.currentPage?.value?.close()
        viewModel.pdfRenderer.value!!.close()
        viewModel.parcelFileDescriptor.value!!.close()
    }

    private fun showPage(index: Int) {
        if (viewModel.pdfRenderer.value!!.pageCount <= index) {
            return
        }

        // Use `openPage` to open a specific page in PDF.
        viewModel.setCurrentPDFPage(viewModel.pdfRenderer.value!!.openPage(index))

        if (viewModel.currentPage != null) {
            // Important: the destination bitmap must be ARGB (not RGB).
            val bitmap = Bitmap.createBitmap(viewModel.currentPage!!.value!!.getWidth(), viewModel.currentPage!!.value!!.getHeight(), Bitmap.Config.ARGB_8888)

            // Here, we render the page onto the Bitmap.
            // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
            // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
            viewModel.currentPage!!.value!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            // Display the page
            viewModel.currentPageImage.value!!.setImage(bitmap)

            //set current page num
            viewModel.setPageIndex(index)

            updateUI()
        }
    }

    private fun updateUI() {
        viewModel.showPathsOfCurrentPage()

        val pdfNum = findViewById<TextView>(R.id.pageNumber)
        pdfNum.text = "Page ${getActualPageNum()}/${viewModel.pdfRenderer.value!!.pageCount}"
    }

    private fun getActualPageNum(): Number {
        return viewModel.pageIndex.value!!+1
    }

    private fun setOnClickToNavigationArrows() {
        val leftArrow = findViewById<ImageView>(R.id.prevPage)
        leftArrow.isClickable = true
        leftArrow.setOnClickListener {
            val index = viewModel.pageIndex.value!!
            if (index > 0) {
                try {
                    openRenderer(this)
                    viewModel.addAndClearPathListToCurrentIndex()
                    showPage(index-1)
                    closeRenderer()
                } catch (exception: IOException) {
                    Log.d(LOGNAME, "Error opening PDF")
                }
            }
        }

        val rightArrow = findViewById<ImageView>(R.id.nextPage)
        rightArrow.isClickable = true
        rightArrow.setOnClickListener {
            val index = viewModel.pageIndex.value!!
            if (index < viewModel.totalPages.value!!-1) {
                try {
                    openRenderer(this)
                    viewModel.addAndClearPathListToCurrentIndex()
                    showPage(index+1)
                    closeRenderer()
                } catch (exception: IOException) {
                    Log.d(LOGNAME, "Error opening PDF")
                }
            }
        }
    }

    private fun setOnClickTools() {
        val undo = findViewById<ImageView>(R.id.undo)
        undo.isClickable = true
        undo.setOnClickListener {
            viewModel.setCurrentPDFTool("undo")
        }

        val redo = findViewById<ImageView>(R.id.redo)
        redo.isClickable = true
        redo.setOnClickListener {
            viewModel.setCurrentPDFTool("redo")
        }

        val draw = findViewById<ImageView>(R.id.draw)
        draw.isClickable = true
        draw.setOnClickListener {
            viewModel.setCurrentPDFTool("draw")
        }

        val highlight = findViewById<ImageView>(R.id.highlight)
        highlight.isClickable = true
        highlight.setOnClickListener {
            viewModel.setCurrentPDFTool("highlight")
        }

        val erase = findViewById<ImageView>(R.id.erase)
        erase.isClickable = true
        erase.setOnClickListener {
            viewModel.setCurrentPDFTool("erase")
        }
    }
}