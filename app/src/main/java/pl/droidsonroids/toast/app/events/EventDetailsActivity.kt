package pl.droidsonroids.toast.app.events

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.support.v4.util.Pair
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_event_details.*
import pl.droidsonroids.toast.R
import pl.droidsonroids.toast.app.Navigator
import pl.droidsonroids.toast.app.base.BaseActivity
import pl.droidsonroids.toast.app.utils.extensions.addInsetAppBehaviorToLoadingLayout
import pl.droidsonroids.toast.data.dto.ImageDto
import pl.droidsonroids.toast.data.dto.event.EventTalkDto
import pl.droidsonroids.toast.databinding.ActivityEventDetailsBinding
import pl.droidsonroids.toast.di.LoginCallbackManager
import pl.droidsonroids.toast.utils.NavigationRequest
import pl.droidsonroids.toast.utils.consume
import pl.droidsonroids.toast.viewmodels.event.EventDetailsViewModel
import javax.inject.Inject

class EventDetailsActivity : BaseActivity() {

    companion object {
        private const val ALPHA_MAX_VALUE = 255
        private const val ADD_ANIMATION_DURATION_MS = 600L
        private const val EVENT_ID = "event_id"
        private const val COVER_IMAGE = "cover_image"

        fun createIntent(context: Context, eventDetailsRequest: NavigationRequest.EventDetails): Intent {
            return Intent(context, EventDetailsActivity::class.java)
                    .putExtra(EVENT_ID, eventDetailsRequest.id)
                    .putExtra(COVER_IMAGE, eventDetailsRequest.coverImage)
        }
    }

    private val eventId by lazy {
        intent.getLongExtra(EVENT_ID, 0)
    }

    private val coverImage by lazy {
        intent.getParcelableExtra<ImageDto?>(COVER_IMAGE)
    }

    private val compositeDisposable = CompositeDisposable()


    @Inject
    lateinit var loginCallbackManager: LoginCallbackManager

    @Inject
    lateinit var navigator: Navigator

    private var isTransitionPostponed = false

    private val eventDetailsViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)
                .get(eventId.toString(), EventDetailsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeSharedTransition()
        val eventDetailsBinding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(eventDetailsBinding.root)

        setupAppBar()
        setupViewModel(eventDetailsBinding)
        setupGradientSwitcher()
        setupRecyclerView()
        addInsetAppBehaviorToLoadingLayout()
    }

    private fun postponeSharedTransition() {
        postponeEnterTransition()
        isTransitionPostponed = true
    }

    private fun setupAppBar() {
        val contentScrimColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val statusBarScrimColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            setToolbarScrim(verticalOffset, appBarLayout, contentScrimColor, statusBarScrimColor)
        }
        collapsingToolbar.scrimVisibleHeightTrigger = Int.MAX_VALUE
    }

    private fun setToolbarScrim(verticalOffset: Int, appBarLayout: AppBarLayout, contentScrimColor: Int, statusBarScrimColor: Int) {
        val offsetFraction = -verticalOffset / appBarLayout.totalScrollRange.toFloat()
        val alphaValue = (offsetFraction * ALPHA_MAX_VALUE).toInt()
        val contentScrimWithAlpha = ColorUtils.setAlphaComponent(contentScrimColor, alphaValue)
        val statusBarScrimWithAlpha = ColorUtils.setAlphaComponent(statusBarScrimColor, alphaValue)

        toolbarTitle.alpha = offsetFraction
        collapsingToolbar.setContentScrimColor(contentScrimWithAlpha)
        collapsingToolbar.setStatusBarScrimColor(statusBarScrimWithAlpha)
    }

    private fun setupViewModel(eventDetailsBinding: ActivityEventDetailsBinding) {
        eventDetailsViewModel.init(eventId, coverImage)
        eventDetailsBinding.eventDetailsViewModel = eventDetailsViewModel
        compositeDisposable += eventDetailsViewModel.navigationSubject
                .subscribe(::handleNavigationRequest)
        compositeDisposable += eventDetailsViewModel.coverImageLoadingFinishedSubject
                .filter { isTransitionPostponed }
                .subscribe { resumeSharedTransition() }
    }

    private fun resumeSharedTransition() {
        startPostponedEnterTransition()
        isTransitionPostponed = false
        eventDetailsViewModel.onTransitionEnd()
    }

    private fun handleNavigationRequest(it: NavigationRequest) {
        if (it is NavigationRequest.EventTalkDetails) {
            navigator.showActivityWithSharedAnimation(this, it, getSharedViews(it.eventTalkDto))
        } else {
            navigator.dispatch(this, it)
        }
    }

    private fun getSharedViews(it: EventTalkDto): Array<Pair<View, String>> {
        return eventSpeakersRecyclerView.findViewHolderForItemId(it.id)
                ?.itemView
                ?.run {
                    val talkCard = findViewById<View>(R.id.talkCard)
                    arrayOf(Pair(talkCard, talkCard.transitionName))
                } ?: emptyArray()
    }

    private fun setupGradientSwitcher() {
        gradientSwitcher.setFactory {
            ImageView(applicationContext).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
        }
    }

    private fun setupRecyclerView() {
        with(eventSpeakersRecyclerView) {
            val eventSpeakersAdapter = EventSpeakersAdapter()
            adapter = eventSpeakersAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator.addDuration = ADD_ANIMATION_DURATION_MS

            subscribeToSpeakersChange(eventSpeakersAdapter)
        }
    }

    private fun subscribeToSpeakersChange(eventSpeakersAdapter: EventSpeakersAdapter) {
        compositeDisposable += eventDetailsViewModel.eventSpeakersSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(eventSpeakersAdapter::setData)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> consume { onBackPressed() }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        eventDetailsViewModel.invalidateAttendState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        loginCallbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (appBar.isCollapsed) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }
}

private val AppBarLayout.isCollapsed: Boolean
    get() {
        val layoutParams = layoutParams as? CoordinatorLayout.LayoutParams
        return (layoutParams?.behavior as? AppBarLayout.Behavior)?.topAndBottomOffset != 0
    }

