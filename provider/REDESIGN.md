# Provider visual redesign

The provider app now uses the customer app's deep-teal, lime, mint, lavender and peach palette with matching typography. Welcome, splash, sign-in and provider home use this visual direction; the remaining authentication and account-status screens inherit the shared provider theme.

The hero illustration is drawn natively with Compose Canvas. Shaded toolbox faces, beveled tools, floating badges, graphicsLayer perspective tilt, and independent scroll offsets provide the depth and parallax effect. No 3D engine, network image asset or new runtime dependency was added. Decorative motion stops when the activity is not resumed, battery saver is active, Android animator duration is disabled, or the illustration leaves the visible viewport. The hero remains visible in static mode.

The welcome buttons stay outside the scrollable content. Provider home has persistent Home, Requests, Schedule and Profile navigation, an availability panel and existing work-action callbacks. Unknown availability is an explicit setup state rather than an endless loading spinner. Authentication forms use keyboard insets and the activity uses adjustResize.

Validation used the connected Honor NIC-LX2, Android 15, 720 x 1604 pixels / 320 dpi. Four targeted Compose tests passed for large text and fixed welcome actions, dark-mode navigation and unknown availability, light-mode availability/action callbacks, and reduced-motion rendering. Device screenshots were checked in light and dark modes. Dashboard screenshots use a synthetic profile inside instrumentation only; no provider login bypass or persistent test account was added.

This change redesigns presentation and motion. Provider job, schedule, earnings, profile and notification callbacks retain their current integration behaviour. Payment processing and those backend screens are not completed by this visual update.
