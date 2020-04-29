package org.wordpress.android.ui.posts.prepublishing.visibility

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.prepublishing_visibility_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PostSettingsInputDialogFragment
import org.wordpress.android.ui.posts.PrepublishingScreenClosedListener
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class PrepublishingVisibilityFragment : Fragment() {
    private lateinit var closeButton: ImageView
    private lateinit var backButton: ImageView
    private lateinit var toolbarTitle: TextView
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: PrepublishingVisibilityViewModel
    private var closeListener: PrepublishingScreenClosedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.prepublishing_visibility_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        visibility_recycler_view.layoutManager = LinearLayoutManager(requireActivity())
        visibility_recycler_view.adapter = PrepublishingVisibilityAdapter(requireActivity())
        toolbarTitle = view.findViewById(R.id.toolbar_title)
        closeButton = view.findViewById(R.id.close_button)
        backButton = view.findViewById(R.id.back_button)

        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingVisibilityViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            (visibility_recycler_view.adapter as PrepublishingVisibilityAdapter).update(uiState)
        })

        viewModel.showPasswordDialog.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                showPostPasswordDialog()
            }
        })

        viewModel.dismissBottomSheet.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onCloseClicked()
            }
        })

        viewModel.navigateToHomeScreen.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onBackClicked()
            }
        })

        viewModel.updateToolbarUiState.observe(this, Observer { uiString ->
            toolbarTitle.text = uiHelpers.getTextOfUiString(
                    requireContext(),
                    uiString
            )
        })

        closeButton.setOnClickListener { viewModel.onCloseButtonClicked() }
        backButton.setOnClickListener { viewModel.onBackButtonClicked() }

        viewModel.start(getEditPostRepository())
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) {
            "This is possibly null because it's " +
                    "called during config changes."
        }

        return editPostActivityHook.editPostRepository
    }

    private fun showPostPasswordDialog() {
        val dialog = PostSettingsInputDialogFragment.newInstance(
                getEditPostRepository().password, getString(string.password),
                getString(string.post_settings_password_dialog_hint), false
        )
        dialog.setPostSettingsInputDialogListener { input -> viewModel.onPostPasswordChanged(input) }

        fragmentManager?.let {
            dialog.show(it, null)
        }
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null
        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    companion object {
        const val TAG = "prepublishing_visibility_fragment_tag"
        fun newInstance() = PrepublishingVisibilityFragment()
    }
}