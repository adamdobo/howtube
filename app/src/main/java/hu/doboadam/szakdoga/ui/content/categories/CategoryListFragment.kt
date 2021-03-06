package hu.doboadam.szakdoga.ui.content.categories

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.doboadam.szakdoga.R
import hu.doboadam.szakdoga.extensions.createDialog
import hu.doboadam.szakdoga.extensions.isYoutubeVideo
import hu.doboadam.szakdoga.model.Category
import hu.doboadam.szakdoga.model.Result
import hu.doboadam.szakdoga.ui.BaseViewModelFragment
import kotlinx.android.synthetic.main.dialog_add_new_video.view.*
import kotlinx.android.synthetic.main.fragment_category_list.*

class CategoryListFragment : BaseViewModelFragment() {

    override val TAG: String = "CategoryListFragment"
    private lateinit var adapter: CategoryAdapter
    private lateinit var viewModel: CategoryListViewModel
    private lateinit var listener: OnCategoryClickedListener
    private var dialog: AlertDialog? = null
    private lateinit var dialogView: View

    companion object {
        fun newInstance(): BaseViewModelFragment = CategoryListFragment()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_category_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CategoryListViewModel::class.java)
        observeViewModel()
        setUpRecyclerView()
        addVideo.setOnClickListener {
            dialog?.show()
            setDialogClickListeners(dialog)
        }
    }

    private fun setDialogClickListeners(dialog: AlertDialog?) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            with(dialogView) {
                val category = categorySpinner.selectedItem as Category
                when {
                    videoUrlText.text.toString().isYoutubeVideo() -> {
                        videoUrlLayout.error = null
                        dialog.dismiss()
                        viewModel.uploadVideo(videoUrlText.text.toString(), category.id)
                    }
                    else -> videoUrlLayout.error = context.getString(R.string.invalid_url)
                }

            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnCategoryClickedListener) {
            listener = context
        } else {
            throw RuntimeException("${context.toString()} must implement interface OnCategoryClickedListener")
        }
    }


    private fun setUpRecyclerView() {
        categoryList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                when {
                    dy > 0 -> addVideo.hide()
                    else -> addVideo.show()
                }
            }
        })
        adapter = CategoryAdapter(emptyList<Category>().toMutableList()) {
            listener.onCategoryClicked(it.id)
        }
        categoryList.layoutManager = GridLayoutManager(context, 2)
        categoryList.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.categoryLiveData.observe(this, Observer<List<Category>> { value ->
            value?.let {
                if (dialog == null) {
                    initDialog(it)
                }
                adapter.setItems(it)
            }
        })
        viewModel.uploadSucceeded.observe(this, Observer<Result> { result ->
            result?.also {
                val message = when (it) {
                    Result.Success -> getString(R.string.upload_success)
                    Result.Failure -> getString(R.string.upload_failed)

                }
                Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun initDialog(it: List<Category>) {
        dialogView = layoutInflater.inflate(R.layout.dialog_add_new_video, null)
        dialog = createDialog {
            setTitle(getString(R.string.add_new_video))
            setUpSpinner(it, dialogView)
            setNegativeButton(getString(R.string.cancel), null)
            setPositiveButton(getString(R.string.ok), null)
            setView(dialogView)
        }
    }

    private fun setUpSpinner(it: List<Category>, dialogView: View) = with(dialogView) {
        val adapter = SpinnerAdapter(context, android.R.layout.simple_spinner_item, it.toTypedArray())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    override fun startListeningToDb() {
        viewModel.startListeningToDbChanges()
    }

    override fun stopListeningToDb() {
        viewModel.stopListeningToDbChanges()
    }

    interface OnCategoryClickedListener {
        fun onCategoryClicked(id: Int)
    }
}