package pl.droidsonrioids.toast.viewmodels

import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.internal.operators.maybe.MaybeJust
import junit.framework.Assert.assertNotNull
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pl.droidsonrioids.toast.data.State
import pl.droidsonrioids.toast.repositories.EventsRepository
import pl.droidsonrioids.toast.testEventDetails
import pl.droidsonrioids.toast.testPreviousEvents
import pl.droidsonrioids.toast.testSplitEvents

@RunWith(MockitoJUnitRunner::class)
class EventsViewModelTest {
    @Mock
    lateinit var eventsRepository: EventsRepository

    @Test
    fun shouldReturnFeaturedEvent() {
        whenever(eventsRepository.getEvents()).thenReturn(MaybeJust.just(testSplitEvents))
        val eventsViewModel = EventsViewModel(eventsRepository)

        val upcomingEventViewModel = eventsViewModel.featuredEvent.get()

        assertNotNull(upcomingEventViewModel)
        assertThat(upcomingEventViewModel.id, equalTo(testEventDetails.id))
        assertThat(upcomingEventViewModel.title, equalTo(testEventDetails.title))
    }

    @Test
    fun shouldReturnSingletonPreviousEventsList() {
        whenever(eventsRepository.getEvents()).thenReturn(MaybeJust.just(testSplitEvents))
        val eventsViewModel = EventsViewModel(eventsRepository)

        val previousEvents = eventsViewModel.previousEventsSubject.value

        assertThat(previousEvents.size, equalTo(1))
        val previousEventViewModel = (previousEvents.first() as? State.Item)?.item
        val testPreviousApiEvent = testPreviousEvents.first()
        assertThat(previousEventViewModel?.id, equalTo(testPreviousApiEvent.id))
        assertThat(previousEventViewModel?.title, equalTo(testPreviousApiEvent.title))
    }

}