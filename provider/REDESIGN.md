# Provider visual redesign

The provider app now uses the customer app's deep-teal, lime, mint, lavender and peach palette with matching typography. Welcome, splash, sign-in and provider home use this visual direction; the remaining authentication and account-status screens inherit the shared provider theme.

The hero illustration is drawn natively with Compose Canvas. Shaded toolbox faces, beveled tools, floating badges, graphicsLayer perspective tilt, and independent scroll offsets provide the depth and parallax effect. No 3D engine, network image asset or new runtime dependency was added. Decorative motion stops when the activity is not resumed, battery saver is active, Android animator duration is disabled, or the illustration leaves the visible viewport. The hero remains visible in static mode.

The welcome buttons stay outside the scrollable content. Provider home has persistent Home, Requests, Schedule and Profile navigation, an availability panel and existing work-action callbacks. Unknown availability is an explicit setup state rather than an endless loading spinner. Authentication forms use keyboard insets and the activity uses adjustResize.

Validation used the connected Honor NIC-LX2, Android 15, 720 x 1604 pixels / 320 dpi. Four targeted Compose tests passed for large text and fixed welcome actions, dark-mode navigation and unknown availability, light-mode availability/action callbacks, and reduced-motion rendering. Device screenshots were checked in light and dark modes. Dashboard screenshots use a synthetic profile inside instrumentation only; no provider login bypass or persistent test account was added.

## Connected workflow update — October 15, 2026

The visual redesign above is now connected to shared Supabase repositories. Home availability, requests, active jobs, schedule, professional profile, activity and direct-payment receipts open live screens through `ProviderWorkspaceScreen`. Pending providers can complete their professional profile before approval. Booking details include status actions, history and participant messages. Screens refresh while visible, with manual refresh available.

The original 3D/parallax dashboard remains the provider home. Eight connected phone checks passed across both apps, including the actual provider MainActivity home-to-receipts route. Temporary fixture accounts were removed afterward. Customers pay providers directly after service; recording receipt does not process a payment. Background push and public release preparation remain outstanding. See `../docs/CONNECTED_WORKFLOW.md` for the complete tested journey.
