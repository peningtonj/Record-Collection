package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.Profile
import io.github.peningtonj.recordcollection.db.ProfileQueries
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.network.spotify.model.ExplicitContent
import io.github.peningtonj.recordcollection.network.spotify.model.Followers
import io.github.peningtonj.recordcollection.network.spotify.model.Image
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileRepositoryTest {
    private val mockDatabase = mockk<RecordCollectionDatabase>()
    private val mockQueries = mockk<ProfileQueries>()
    private lateinit var repository: ProfileRepository

    @Before
    fun setup() {
        coEvery { mockDatabase.profileQueries } returns mockQueries
        repository = ProfileRepository(mockDatabase)
    }

    @After
    fun tearDown() {
        // Clear any stored state if needed
    }

    /**
     * Tests that the repository correctly handles an empty database scenario.
     *
     * Verifies that:
     * - When the database query returns null
     * - The repository should return null
     * - The correct database query method was called
     */
    @Test
    fun `getProfile returns null when database is empty`() = runTest {
        // Given
        coEvery { mockQueries.getProfile().executeAsOneOrNull() } returns null

        // When
        val result = repository.getProfile()

        // Then
        assertNull(result)
        coVerify { mockQueries.getProfile() }
    }

    /**
     * Tests that the repository can successfully retrieve an existing profile.
     *
     * Verifies that:
     * - When a profile exists in the database
     * - The repository returns the exact same profile
     * - The correct database query method was called
     */
    @Test
    fun `getProfile returns profile when exists in database`() = runTest {
        // Given
        val testProfile = TestData.createDbProfile()
        coEvery { mockQueries.getProfile().executeAsOneOrNull() } returns testProfile

        // When
        val result = repository.getProfile()

        // Then
        assertEquals(testProfile, result)
        coVerify { mockQueries.getProfile() }
    }

    /**
     * Tests the complete transformation and storage flow of a Spotify profile.
     *
     * Verifies that:
     * - SpotifyProfile is correctly transformed to database Profile
     * - All fields are mapped correctly
     * - The profile is stored using the correct database method
     */
    @Test
    fun `saveProfile correctly transforms and stores spotify profile`() = runTest {
        // Given
        val slot = slot<Profile>()
        coEvery {
            mockQueries.upsertProfile(capture(slot))
        } returns Unit
        val testProfile = TestData.createSpotifyProfile()

        // When
        repository.saveProfile(testProfile)

        // Then
        with(slot.captured) {
            assertEquals(testProfile.id, id)
            assertEquals(testProfile.displayName, display_name)
            assertEquals(testProfile.email, email)
            assertEquals(testProfile.country, country)
            assertEquals(testProfile.uri, spotify_uri)
            assertEquals(testProfile.externalUrls["spotify"], spotify_url)
            assertEquals(testProfile.images.firstOrNull()?.url, profile_image_url)
            assertEquals(testProfile.followers.total.toLong(), followers_count)
            assertEquals(testProfile.product, product_type)
            assertTrue(updated_at > 0)
        }
    }

    /**
     * Tests handling of database errors during profile save operation.
     *
     * Verifies that:
     * - Database errors are properly propagated
     * - The operation fails with the correct exception type
     */
    @Test
    fun `saveProfile handles database error`() = runTest {
        // Given
        coEvery {
            mockQueries.upsertProfile(any())
        } throws SQLException("Database error")

        // When
        val result = runCatching {
            repository.saveProfile(TestData.createSpotifyProfile())
        }

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SQLException)
    }

    /**
     * Tests handling of SpotifyProfile with null optional fields.
     *
     * Verifies that:
     * - Null fields are handled gracefully
     * - Default values are used where appropriate
     */
    @Test
    fun `saveProfile handles null optional fields`() = runTest {
        // Given
        val slot = slot<Profile>()
        coEvery {
            mockQueries.upsertProfile(capture(slot))
        } returns Unit

        val profileWithNulls = TestData.createSpotifyProfile().copy(
            country = null,
            displayName = null,
            email = null,
            images = emptyList()
        )

        // When
        repository.saveProfile(profileWithNulls)

        // Then
        with(slot.captured) {
            assertNull(country)
            assertNull(display_name)
            assertNull(email)
            assertNull(profile_image_url)
            // Other fields should still be present
            assertEquals(profileWithNulls.id, id)
            assertTrue(updated_at > 0)
        }
    }

    /**
     * Test data factory for creating consistent test objects
     */
    private object TestData {
        fun createDbProfile() = Profile(
            id = "test_id",
            display_name = "Test User",
            email = "test@example.com",
            country = "US",
            spotify_uri = "spotify:user:test",
            spotify_url = "https://open.spotify.com/user/test",
            profile_image_url = "https://example.com/profile.jpg",
            followers_count = 42,
            product_type = "premium",
            updated_at = 1
        )

        fun createSpotifyProfile() = SpotifyProfile(
            country = "US",
            displayName = "Test User",
            email = "test@example.com",
            explicitContent = ExplicitContent(
                filterEnabled = false,
                filterLocked = false
            ),
            externalUrls = mapOf(
                "spotify" to "https://open.spotify.com/user/test"
            ),
            followers = Followers(
                href = null,
                total = 42
            ),
            href = "https://api.spotify.com/v1/users/test",
            id = "test",
            images = listOf(
                Image(
                    url = "https://example.com/profile.jpg",
                    height = 300,
                    width = 300
                )
            ),
            product = "premium",
            type = "user",
            uri = "spotify:user:test"
        )
    }
}