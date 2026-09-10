package io.github.peningtonj.recordcollection.navigation

import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthNavTargetTest {

    private fun authed(token: String = "t") =
        AuthState.Authenticated(AccessToken(token, "Bearer", "scope", 3600))

    @Test
    fun `authenticated on the login screen goes to library`() {
        assertEquals(Screen.Library, authNavTarget(authed(), Screen.Login))
    }

    @Test
    fun `a token refresh while in the app does not move the user`() {
        // The bug: every refresh re-emits Authenticated; this must be a no-op unless
        // the user is sitting on Login.
        assertNull(authNavTarget(authed("new-token"), Screen.Library))
        assertNull(authNavTarget(authed("new-token"), Screen.Album("a", "s")))
        assertNull(authNavTarget(authed("new-token"), Screen.Collection("Faves")))
    }

    @Test
    fun `the transient Authenticating state never navigates`() {
        assertNull(authNavTarget(AuthState.Authenticating, Screen.Library))
        assertNull(authNavTarget(AuthState.Authenticating, Screen.Login))
        assertNull(authNavTarget(AuthState.Authenticating, Screen.Album("a", "s")))
    }

    @Test
    fun `a dead session sends the user to login, once`() {
        assertEquals(Screen.Login, authNavTarget(AuthState.NotAuthenticated, Screen.Library))
        assertEquals(Screen.Login, authNavTarget(AuthState.Error("refresh failed"), Screen.Album("a", "s")))
        assertNull(authNavTarget(AuthState.NotAuthenticated, Screen.Login))
        assertNull(authNavTarget(AuthState.Error("x"), Screen.Login))
    }
}
