package pl.droidsonrioids.toast.app.events

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pl.droidsonrioids.toast.app.base.BaseFragment
import pl.droidsonrioids.toast.databinding.FragmentEventsBinding
import pl.droidsonrioids.toast.viewmodels.EventsViewModel
import android.view.animation.DecelerateInterpolator
import kotlinx.android.synthetic.main.fragment_events.*
import pl.droidsonrioids.toast.R

const val TOP_BAR_TRANSLATION_FACTOR = 2f
class EventsFragment : BaseFragment() {

    private lateinit var eventsViewModel: EventsViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        eventsViewModel = ViewModelProviders.of(this, viewModelFactory)[EventsViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentEventsBinding.inflate(inflater, container, false)
        binding.eventsViewModel = eventsViewModel
        return binding.root
    }
    private val maxToolbarElevation by lazy {
        resources.getDimensionPixelSize(R.dimen.home_toolbar_elevation).toFloat()
    }
    private val topBarHeight by lazy {
        resources.getDimensionPixelSize(R.dimen.events_top_bar_height).toFloat()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupAppBarShadow()
    }

    private fun setupRecyclerView() {
        with(previousEventsRecyclerView) {
            adapter = PreviousEventsAdapter()
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupAppBarShadow() {
        val shadowInterpolator = DecelerateInterpolator(TOP_BAR_TRANSLATION_FACTOR)

        eventsScrollContainer.setOnScrollChangeListener { _: NestedScrollView?, _, scrollY, _, _ ->
            topBar.translationY = -scrollY * TOP_BAR_TRANSLATION_FACTOR

            val shadow = scrollY.takeIf { it < topBarHeight }
                    ?.let { shadowInterpolator.getInterpolation(it / topBarHeight) * maxToolbarElevation }
                    ?: maxToolbarElevation

            shadowCreator.translationZ = shadow
        }
    }

}