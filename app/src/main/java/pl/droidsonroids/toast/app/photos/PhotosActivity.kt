package pl.droidsonroids.toast.app.photos

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.databinding.Observable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.GridLayoutManager
import android.view.MenuItem
import android.view.View
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.alexvasilkov.gestures.transition.tracker.SimpleTracker
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_photos.*
import pl.droidsonroids.toast.R
import pl.droidsonroids.toast.app.Navigator
import pl.droidsonroids.toast.app.base.BaseActivity
import pl.droidsonroids.toast.app.utils.binding.setVisible
import pl.droidsonroids.toast.data.dto.ImageDto
import pl.droidsonroids.toast.utils.NavigationRequest
import pl.droidsonroids.toast.utils.addOnPropertyChangedCallback
import pl.droidsonroids.toast.utils.consume
import pl.droidsonroids.toast.viewmodels.photos.PhotosViewModel
import java.util.*
import javax.inject.Inject

class PhotosActivity : BaseActivity() {
    companion object {
        private const val PHOTOS_KEY = "photos_key"
        private const val PHOTOS_GRID_SIZE = 2
        private const val SYSTEM_UI_FLAG_SHOW_NAVIGATION = 6

        private const val IMMERSIVE_MODE =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        private const val NORMAL_MODE = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        fun createIntent(context: Context, navigationRequest: NavigationRequest.Photos): Intent {
            return Intent(context, PhotosActivity::class.java)
                    .putParcelableArrayListExtra(PHOTOS_KEY, ArrayList(navigationRequest.photos))
        }
    }

    @Inject
    lateinit var navigator: Navigator

    private val photosViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[PhotosViewModel::class.java]
    }

    private val photos by lazy { intent.getParcelableArrayListExtra<ImageDto>(PHOTOS_KEY) }

    private val compositeDisposable = CompositeDisposable()
    private val isImmersiveMode
        get() = window.decorView.systemUiVisibility and PhotosActivity.IMMERSIVE_MODE == PhotosActivity.IMMERSIVE_MODE

    private var pagerAnimator: ViewsTransitionAnimator<Int>? = null
    private var defaultStatusBarColor = 0

    private var rotationCallback: Observable.OnPropertyChangedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos)
        setupWindow()
        setupMainToolbar()
        setupFullPhotoToolbar()
        setupViewModel()
        setupRecyclerView()
        setupViewPager()
        setupPagerAnimator()
        setHighGlideMemoryCache()
    }

    private fun setupWindow() {
        window.decorView.systemUiVisibility = PhotosActivity.NORMAL_MODE
        defaultStatusBarColor = window.statusBarColor
        ViewCompat.setOnApplyWindowInsetsListener(fullPhotoViewPager) { _, insets ->
            for (i in 0 until fullPhotoViewPager.childCount) {
                ViewCompat.dispatchApplyWindowInsets(fullPhotoViewPager.getChildAt(i), insets)
            }
            insets
        }
        val photoItemPadding = resources.getDimensionPixelSize(R.dimen.photo_item_pading)
        ViewCompat.setOnApplyWindowInsetsListener(photosRecyclerView) { view, insets ->
            val bottomPadding = insets.systemWindowInsetBottom + insets.systemWindowInsetTop + photoItemPadding
            view.setPadding(photoItemPadding, photoItemPadding, photoItemPadding, bottomPadding)
            insets
        }
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility == SYSTEM_UI_FLAG_SHOW_NAVIGATION) {
                toggleImmersiveMode()
            }
        }
    }

    private fun setupMainToolbar() {
        setSupportActionBar(photosToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    private fun setupFullPhotoToolbar() {
        fullPhotoToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupViewModel() {
        photosViewModel.init(photos)
        compositeDisposable += photosViewModel.navigationSubject
                .subscribe(::handleNavigationRequest)
        rotationCallback = photosViewModel.rotation
                .apply { photosContainer.rotation = get() }
                .addOnPropertyChangedCallback {
                    photosContainer.rotation = it
                }
    }

    private fun setupRecyclerView() {
        with(photosRecyclerView) {
            val photosAdapter = PhotosAdapter()
            adapter = photosAdapter
            layoutManager = GridLayoutManager(context, PHOTOS_GRID_SIZE)

            subscribeToPhotosChange(photosAdapter)
        }
    }

    private fun subscribeToPhotosChange(photosAdapter: PhotosAdapter) {
        compositeDisposable += photosViewModel.photosSubject
                .subscribe { photosAdapter.setData(it) }
    }

    private fun setupViewPager() {
        val photosViewPagerAdapter = PhotosViewPagerAdapter()
        fullPhotoViewPager.adapter = photosViewPagerAdapter
        fullPhotoViewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_large)

        subscribeToFullscreenPhotosChange()
    }

    private fun subscribeToFullscreenPhotosChange() {
        val pagerAdapter = fullPhotoViewPager.adapter as PhotosViewPagerAdapter
        compositeDisposable += photosViewModel.fullscreenPhotosSubject
                .subscribe(pagerAdapter::setData)
    }

    private fun setupPagerAnimator() {
        pagerAnimator = GestureTransitions.from<Int>(photosRecyclerView, getRecyclerViewTracker())
                .into(fullPhotoViewPager, getViewPagerTracker())
                .apply { addPositionUpdateListener(::applyFullPhotoPagerState) }
    }

    private fun getRecyclerViewTracker(): SimpleTracker {
        return object : SimpleTracker() {
            public override fun getViewAt(position: Int): View? {
                return photosRecyclerView.findViewHolderForLayoutPosition(position).itemView
            }
        }
    }

    private fun getViewPagerTracker(): SimpleTracker {
        val pagerAdapter = fullPhotoViewPager.adapter as PhotosViewPagerAdapter
        return object : SimpleTracker() {
            public override fun getViewAt(position: Int): View? {
                val holder = pagerAdapter.getViewHolder(position)
                return holder?.let(pagerAdapter::getPhotoView)
            }
        }
    }

    private fun applyFullPhotoPagerState(fullPhotoVisibilityOffset: Float, isLeaving: Boolean) {
        val isFullPhotoVisible = fullPhotoVisibilityOffset != 0f

        fullPhotoBackground.setVisible(isFullPhotoVisible)
        fullPhotoBackground.alpha = fullPhotoVisibilityOffset

        fullPhotoToolbar.setVisible(isFullPhotoVisible)
        fullPhotoToolbar.alpha = fullPhotoVisibilityOffset

        setStatusBarColor(fullPhotoVisibilityOffset)

        if (isImmersiveMode && !isFullPhotoVisible && isImmersiveMode) {
            toggleImmersiveMode()
        }
    }

    private fun setStatusBarColor(offset: Float) {
        window.statusBarColor = if (offset == 0f) {
            defaultStatusBarColor
        } else {
            ContextCompat.getColor(this, R.color.blackAlpha40)
        }
    }


    private fun toggleImmersiveMode() {
        window.decorView.systemUiVisibility = if (isImmersiveMode) PhotosActivity.NORMAL_MODE else PhotosActivity.IMMERSIVE_MODE
        val appBarTranslation = if (isImmersiveMode) -fullPhotoToolbar.bottom.toFloat() else 0.0f
        fullPhotoToolbar.animate().translationY(appBarTranslation).start()
    }

    private fun handleNavigationRequest(navigationRequest: NavigationRequest) {
        when (navigationRequest) {
            is NavigationRequest.FullscreenPhoto -> onPhotoItemClicked(navigationRequest.index)
            NavigationRequest.ToggleImmersive -> toggleImmersiveMode()
            else -> navigator.dispatch(this, navigationRequest)
        }
    }

    private fun onPhotoItemClicked(index: Int) {
        openFullPhoto(index)
    }

    private fun setHighGlideMemoryCache() {
        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)
    }

    private fun setDefaultGlideMemoryCache() {
        Glide.get(this).setMemoryCategory(MemoryCategory.NORMAL)
    }

    private fun isFullPhotoOpened() = pagerAnimator?.isLeaving == false

    private fun openFullPhoto(index: Int) {
        pagerAnimator?.enter(index, true)
    }

    private fun closeFullPhoto() {
        pagerAnimator?.exit(true)
    }

    override fun onBackPressed() {
        if (isFullPhotoOpened()) {
            closeFullPhoto()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> consume { onBackPressed() }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        photosViewModel.rotation.removeOnPropertyChangedCallback(rotationCallback)
        setDefaultGlideMemoryCache()
        compositeDisposable.dispose()
        super.onDestroy()
    }
}