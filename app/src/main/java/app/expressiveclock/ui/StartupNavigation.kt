package app.expressiveclock.ui

internal data class StartupNavigation(val tab: Tab?, val handledRequestId: Long?)

/** A restored Activity intent is not a new navigation request after a configuration change. */
internal fun resolveStartupNavigation(
    current: Tab?, requestedKey: String?, requestId: Long, handledRequestId: Long?,
    settingsLoaded: Boolean, defaultTab: Tab,
): StartupNavigation {
    val requested = Tab.entries.firstOrNull { it.key == requestedKey }
    return when {
        requested != null && handledRequestId != requestId -> StartupNavigation(requested, requestId)
        current == null && settingsLoaded -> StartupNavigation(defaultTab, handledRequestId)
        else -> StartupNavigation(current, handledRequestId)
    }
}
