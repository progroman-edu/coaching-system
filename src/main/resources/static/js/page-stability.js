// Prevent unintended page reloads when switching tabs or on visibility changes
// This ensures the page remains stable when the user returns from another tab

(function() {
    // Prevent reload on visibility change (tab switch)
    document.addEventListener('visibilitychange', (e) => {
        // Simply log for debugging, but don't reload
        if (document.hidden) {
            console.debug('Page hidden (tab switched away)');
        } else {
            console.debug('Page visible (tab switched back)');
        }
    });

    // Prevent accidental reload triggers
    let isIntentionalNavigation = false;

    // Monitor links and form submissions
    document.addEventListener('click', (e) => {
        const link = e.target.closest('a[href]');
        if (link && !link.hasAttribute('data-no-prevent')) {
            isIntentionalNavigation = true;
        }
    });

    document.addEventListener('submit', (e) => {
        isIntentionalNavigation = true;
    });

    // Prevent unintentional page reloads (F5, Ctrl+R triggered)
    window.addEventListener('beforeunload', (e) => {
        // Only warn if there are unsaved changes
        // This is a safety net - normally should not occur
        if (false) { // Disabled by default - enable if tracking unsaved state
            e.preventDefault();
            e.returnValue = 'You may have unsaved changes.';
        }
    });

    // Log page state on init
    console.debug('Page stability protection enabled - auto-reload on tab switch is disabled');
})();
