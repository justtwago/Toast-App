package pl.droidsonroids.toast.app.speakers

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.Observable
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import kotlinx.android.synthetic.main.fragment_speakers.*
import kotlinx.android.synthetic.main.layout_speakers_sorting_bar.*
import pl.droidsonroids.toast.app.Navigator
import pl.droidsonroids.toast.app.base.BaseFragment
import pl.droidsonroids.toast.app.home.MainActivity
import pl.droidsonroids.toast.app.utils.callbacks.LazyLoadingScrollListener
import pl.droidsonroids.toast.databinding.FragmentSpeakersBinding
import pl.droidsonroids.toast.utils.Constants
import pl.droidsonroids.toast.utils.addOnPropertyChangedCallback
import pl.droidsonroids.toast.viewmodels.speaker.SpeakersViewModel
import javax.inject.Inject

class SpeakersFragment : BaseFragment() {

    @Inject
    lateinit var navigator: Navigator

    private lateinit var speakersViewModel: SpeakersViewModel

    private var navigationDisposable: Disposable = Disposables.disposed()

    private var speakersDisposable: Disposable = Disposables.disposed()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setupViewModel()
    }


    private var sortingDetailsVisibilityCallback: Observable.OnPropertyChangedCallback? = null

    private fun setupViewModel() {
        speakersViewModel = ViewModelProviders.of(this, viewModelFactory)[SpeakersViewModel::class.java]
        navigationDisposable = speakersViewModel.navigationSubject
                .subscribe { request ->
                    activity?.let { navigator.dispatch(it, request) }
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSpeakersBinding.inflate(inflater, container, false)
        binding.speakersViewModel = speakersViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        showSearchMenuItemWithAnimation()
        setupRecyclerView()
        showSearchMenuItemWithAnimation()
        speakersViewModel.isSortingDetailsVisible.run {
            sortingDetailsVisibilityCallback = addOnPropertyChangedCallback {
                val isSortingVisible = get()
                sortingDetailsLayout.animate()
                        .translationY(if (isSortingVisible) sortingDetailsLayout.height.toFloat() else 0f)
                        .start()
                arrowDownImage.animate().rotation(if (isSortingVisible) 180f else 0f).start()
            }
        }
    }

    private fun showSearchMenuItemWithAnimation() {
        animateViewByY(Constants.SearchMenuItem.SHOWN_OFFSET)
    }

    private fun hideSearchMenuItemWithAnimation() {
        animateViewByY(Constants.SearchMenuItem.HIDDEN_OFFSET)
    }

    private fun animateViewByY(offset: Float) {
        (activity as MainActivity).animateSearchButton(offset)
    }

    private fun setupRecyclerView() {
        with(speakersRecyclerView) {
            val speakersAdapter = SpeakersAdapter()
            adapter = speakersAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(SpeakerItemDecoration(context.applicationContext))
            addOnScrollListener(LazyLoadingScrollListener {
                speakersViewModel.loadNextPage()
            })

            subscribeToSpeakersChange(speakersAdapter)
        }
    }

    private fun subscribeToSpeakersChange(speakersAdapter: SpeakersAdapter) {
        speakersDisposable = speakersViewModel.speakersSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(speakersAdapter::setData)
    }

    override fun onDestroyView() {
        speakersViewModel.isSortingDetailsVisible.removeOnPropertyChangedCallback(sortingDetailsVisibilityCallback)
        hideSearchMenuItemWithAnimation()
        speakersDisposable.dispose()
        super.onDestroyView()
    }

    override fun onDetach() {
        navigationDisposable.dispose()
        super.onDetach()
    }
}