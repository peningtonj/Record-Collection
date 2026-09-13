package io.github.peningtonj.recordcollection.viewmodel

import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import io.github.peningtonj.recordcollection.repository.SearchRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val searchRepository = mockk<SearchRepository>()
    private val albumRepository = mockk<AlbumRepository>()
    private val profileRepository = mockk<ProfileRepository>()

    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { albumRepository.fetchAllNewReleases(any()) } returns emptyList()
        every { albumRepository.getAlbumsByIds(any()) } returns flowOf(emptyList())
        viewModel = SearchViewModel(searchRepository, albumRepository, profileRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `clearing the search term shows new releases, not a stuck loading screen`() {
        viewModel.search("radiohead")
        // clearSearch() is what the search bar's X button calls.
        viewModel.clearSearch()

        assertIs<SearchScreenUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun `backspacing to an empty query also settles on Idle`() {
        // search("") is the other way a query becomes empty (typing, not the X button) —
        // it already worked; locking it alongside clearSearch() so they can't drift apart.
        viewModel.search("")

        assertIs<SearchScreenUiState.Idle>(viewModel.uiState.value)
    }
}
