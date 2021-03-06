package pl.droidsonroids.toast.viewmodels.event

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import io.reactivex.subjects.PublishSubject
import pl.droidsonroids.toast.app.utils.managers.AnalyticsEventTracker
import pl.droidsonroids.toast.data.dto.ImageDto
import pl.droidsonroids.toast.data.dto.event.EventTalkDto
import pl.droidsonroids.toast.data.mapper.toViewModel
import pl.droidsonroids.toast.utils.NavigationRequest
import pl.droidsonroids.toast.viewmodels.NavigatingViewModel
import pl.droidsonroids.toast.viewmodels.speaker.SpeakerItemViewModel
import javax.inject.Inject


class EventTalkDetailsViewModel @Inject constructor(
        private val analyticsEventTracker: AnalyticsEventTracker,
        val rotation: ObservableField<Float>
) : ViewModel(), NavigatingViewModel {
    override val navigationSubject: PublishSubject<NavigationRequest> = PublishSubject.create()
    val id: ObservableField<Long> = ObservableField()
    val title: ObservableField<String> = ObservableField()
    val description: ObservableField<String> = ObservableField()
    val speakerItemViewModel: ObservableField<SpeakerItemViewModel> = ObservableField()

    fun init(talkDto: EventTalkDto) {
        talkDto.let {
            id.set(it.id)
            title.set(it.title)
            description.set(it.description)
            speakerItemViewModel.set(it.speaker.toViewModel(::onSpeakerClick))
        }
    }

    private fun onSpeakerClick(speakerId: Long, speakerName: String, avatar: ImageDto?) {
        navigationSubject.onNext(NavigationRequest.SpeakerDetails(speakerId, avatar, id.get()))
        analyticsEventTracker.logEventDetailsShowSpeakerEvent(speakerName)
    }

    fun onReadLess() {
        navigationSubject.onNext(NavigationRequest.Close)
    }
}