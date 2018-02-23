package pl.droidsonroids.toast.test

import android.support.test.espresso.Espresso
import android.support.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import pl.droidsonroids.toast.R
import pl.droidsonroids.toast.app.home.MainActivity
import pl.droidsonroids.toast.function.getString
import pl.droidsonroids.toast.robot.SpeakersRobot

class SpeakersScreenTest {
    @JvmField
    @Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, true)

    private fun goToSpeakersScreen() {
        with(SpeakersRobot()) {
            performClickOnElementWithId(R.id.actionSpeakers)
        }
    }

    private fun goToSearchScreen() {
        goToSpeakersScreen()
        with(SpeakersRobot()) {
            performClickOnElementWithId(R.id.searchImageButton)
        }
    }

    private fun goToSpeakerDetailsScreen() {
        with(SpeakersRobot()) {
            goToSpeakersScreen()
            performClickOnRecyclerViewElement(R.id.speakersRecyclerView, 0)
        }
    }

    @Test
    fun isToolbarDisplayed() {
        val toolbarTitle = getString(R.string.speakers_title)
        goToSpeakersScreen()
        with(SpeakersRobot()) {
            checkIfToolbarWithTitleIsDisplayed(toolbarTitle, R.id.toolbar)
        }
    }

    @Test
    fun isSearchIconDisplayed() {
        goToSpeakersScreen()
        with(SpeakersRobot()) {
            checkIfElementWithIdIsDisplayed(R.id.searchImageButton)
        }
    }

    @Test
    fun isSpeakerSelectedOnSpeakersScreen() {
        goToSpeakersScreen()
        with(SpeakersRobot()) {
            performClickOnRecyclerViewElement(R.id.speakersRecyclerView, 0)
        }
    }

    @Test
    fun isSpeakerSelectedOnSearchScreen() {
        goToSearchScreen()
        with(SpeakersRobot()) {
            performTyping("a", R.id.searchBox)
            checkIfSearchIsPerformed()
            performClickOnRecyclerViewElement(R.id.speakersSearchRecyclerView, 0)
        }
    }

    @Test
    fun isEveryElementOnSearchScreenDisplayed() {
        goToSearchScreen()
        with(SpeakersRobot()) {
            checkIfElementWithIdIsDisplayed(R.id.searchBox)
            checkIfHomeButtonIsDisplayed()
            checkIfHintOnEditTextIsCorrect(R.id.searchBox, getString(R.string.search_hint))
        }
    }

    @Test
    fun isSpeakersScreenDisplayedAfterClickingOnBackButton() {
        goToSearchScreen()
        with(SpeakersRobot()) {
            performNavigateUp()
            checkIfElementWithIdIsDisplayed(R.id.searchImageButton)
        }
    }

    @Test
    fun isSortingBarDisplayed() {
        goToSpeakersScreen()
        with(SpeakersRobot()) {
            checkIfElementWithIdIsDisplayed(R.id.sortingBarLayout)
            checkIfElementWithIdIsDisplayed(R.id.arrowDownImage)
        }
    }

    @Test
    fun isSortingBarExpanded() {
        goToSpeakersScreen()
        with(SpeakersRobot()) {
            performClickOnElementWithId(R.id.titleSortingLayout)
            checkIfElementWithIdIsDisplayed(R.id.arrowUpImage)
            checkIfElementWithIdIsDisplayed(R.id.alphabeticalDivider)
            checkIfElementWithIdIsDisplayed(R.id.alphabeticalSortImage)
            checkIfTextIsCorrect(getString(R.string.alphabetical), R.id.alphabeticalText)
            checkIfElementWithIdIsDisplayed(R.id.dateDivider)
            checkIfElementWithIdIsDisplayed(R.id.dateSortImage)
            checkIfTextIsCorrect(getString(R.string.date), R.id.dateText)
        }
    }

    /**
     * Tests for Speaker Details
     */

    @Test
    fun isHeaderDisplayedWithProperElements() {
        goToSpeakerDetailsScreen()
        with(SpeakersRobot()) {
            checkIfElementWithIdIsDisplayed(R.id.collapsingToolbar)
            checkIfElementWithIdIsDisplayed(R.id.avatarImage)
            checkIfElementWithIdIsDisplayed(R.id.avatarBorderSmall)
            checkIfElementWithIdIsDisplayed(R.id.avatarBorderBig)
            checkIfElementWithIdIsDisplayed(R.id.speakerName)
            checkIfElementWithIdIsDisplayed(R.id.speakerJob)
        }
    }

    @Test
    fun isSpeakersListDisplayedAfterClickingBackFromSpeakerDetails() {
        goToSpeakerDetailsScreen()
        Espresso.pressBack()
        with(SpeakersRobot()) {
            checkIfElementWithIdIsDisplayed(R.id.searchImageButton)
        }
    }
}
