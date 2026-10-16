# Provider visual redesign

The provider app now uses the customer app's deep-teal, lime, mint, lavender and peach palette with matching typography. Welcome, splash, sign-in and provider home use this visual direction; the remaining authentication and account-status screens inherit the shared provider theme.

The hero illustration is drawn natively with Compose Canvas. Shaded toolbox faces, beveled tools, floating badges, graphicsLayer perspective tilt, and independent scroll offsets provide the depth and parallax effect. No 3D engine, network image asset or new runtime dependency was added. Decorative motion stops when the activity is not resumed, battery saver is active, Android animator duration is disabled, or the illustration leaves the visible viewport. The hero remains visible in static mode.

The welcome buttons stay outside the scrollable content. Provider home has persistent Home, Requests, Schedule and Profile navigation, an availability panel and existing work-action callbacks. Unknown availability is an explicit setup state rather than an endless loading spinner. Authentication forms use keyboard insets and the activity uses adjustResize.

Validation used the connected Honor NIC-LX2, Android 15, 720 x 1604 pixels / 320 dpi. Four targeted Compose tests passed for large text and fixed welcome actions, dark-mode navigation and unknown availability, light-mode availability/action callbacks, and reduced-motion rendering. Device screenshots were checked in light and dark modes. Dashboard screenshots use a synthetic profile inside instrumentation only; no provider login bypass or persistent test account was added.

## Connected workflow update — October 15, 2026

The visual redesign above is now connected to shared Supabase repositories. Home availability, requests, active jobs, schedule, professional profile, activity and direct-payment receipts open live screens through `ProviderWorkspaceScreen`. Pending providers can complete their professional profile before approval. Booking details include status actions, history and participant messages. Screens refresh while visible, with manual refresh available.

The original 3D/parallax dashboard remains the provider home. Eight connected phone checks passed across both apps, including the actual provider MainActivity home-to-receipts route. Temporary fixture accounts were removed afterward. Customers pay providers directly after service; recording receipt does not process a payment. Background push and public release preparation remain outstanding. See `../docs/CONNECTED_WORKFLOW.md` for the complete tested journey.

## Light-only provider appearance — October 16, 2026

The provider app always uses its light color scheme, independent of the device theme. Splash and activity launch backgrounds use the light surface palette; edge-to-edge system bars use dark icons. Android automatic force-dark rendering is disabled for this app. The existing teal/lime branding and 3D illustrations remain. The customer app's separate theme preference is unchanged.

Validation: the provider debug APK and test APK built successfully, all four provider unit tests and four existing Compose screen checks passed. The updated app was installed with data preserved on the Honor NIC-LX2 while Android night mode was enabled; live app and isolated dashboard screenshots were captured for visual review.

## Provider profile photos — October 16, 2026

Professional profile now has Add/Change photo, an explicit preview and Save photo, plus Remove photo. Android Photo Picker grants access to only the selected image. Input is limited to 15 MB, orientation is corrected, and the centered image is encoded as a JPEG up to 512 × 512 with EXIF/location metadata removed. Storage uploads use unique paths and the existing signed-in session. A validated backend RPC publishes the owned photo; replacements clean up the old object after publication. Interrupted/ambiguous saves require refresh before retrying and never fake success.

Photos appear in the provider dashboard, customer directory/profile and administrator review. Missing photos use initials. The bucket is private: owners/admins can review pending photos; customers and guests can read only the current photo of an approved provider with a complete active service profile.

Photo verification passed 48 unit tests, six provider Compose checks, five rollback-only SQL suites and the admin browser suite. The installed provider photo picker was opened and dismissed without selecting any personal media. Existing account/session data was preserved.

## Photo interaction refinement — October 16, 2026

Profile uses a centered, tappable circular avatar with a camera badge. Edit photo opens a native bottom sheet with gallery, camera, view and remove actions; removal requires confirmation. Gallery and camera images open a circular framing preview with drag/pinch, an accessible zoom slider and rotation. Use photo returns the framed preview to the profile; Save photo uploads through the existing secure backend. Discard, cancelled pickers and cancelled camera capture preserve the saved image.

Camera capture uses a non-exported FileProvider limited to the app's dedicated profile-camera cache folder; no broad gallery or camera permission is added. Captured temporary files are removed after preparation/cancellation. Image decoding remains bounded, orientation-aware and free of EXIF/location metadata in the uploaded JPEG.

Validation for the refinement: both APKs built and were installed on the Honor phone with account data retained. All 48 unit tests and ten provider screen checks passed; the six photo checks were repeated after the final crop status-bar adjustment. Tests cover sheet cancellation, crop bounds, zoom/rotation, JPEG output and the restricted camera FileProvider. Profile, action-sheet and isolated crop screenshots were reviewed in light mode on the phone.

Crop refinement: the preview now includes Adjust crop, which reopens the original selected image for another framing pass before upload. Both gallery and camera selections require explicit crop confirmation; the action sheet fully expands and scrolls so actions remain reachable.

Validation: both normal debug APKs build and all 48 unit tests pass. All eight photo instrumentation checks passed on the Honor in a temporary separate package that preserved the regular app's unsaved preview. The gallery crop/re-adjust/save flow was repeated for screenshot review and verifies adjusted output pixels; the camera cancellation check verifies no save callback. Test media and save callbacks remain isolated from the shared backend. The temporary test apps were removed.

## Profile view and edit separation

My profile presents the saved provider photo, service area, description, availability, account email and phone as read-only details. The accessible toolbar edit icon opens a separate Edit profile screen. Photo selection and crop controls are available there. Successful profile save plus data refresh returns to the updated overview; validation/backend errors stay in the editor. Back exits editing without submitting form changes. Pending-provider approval checking stays on the overview.

Validation: both standard APKs built; all 48 unit tests and all 16 provider device checks passed, including read-only display, edit navigation, successful-save return, invalid/failed saves, back without submitting, pending approval and large text. Fixture screenshots of both screens were reviewed. Device checks used a temporary separate package to preserve the regular app's existing unsaved photo preview, and the temporary apps were removed.

## Modern profile with depth

The reference's portrait and section hierarchy is adapted to Fixoom's light theme with a teal identity header, lime account status, soft colour accents and compact expandable rows. Layered rings, a raised portrait and decorative service/location tiles move at different scroll speeds; the portrait also tilts slightly in perspective. Entrance, expand/collapse and view/edit transitions use short Compose animations. Decorative layers do not intercept scrolling or accessibility focus. The header reads scroll state in graphics layers to avoid recomposing the profile every frame.

Profile essentials is calculated from four groups of saved data: portrait, contact details, service name/category/area and introduction. It reports field presence, never verification or approval. No new backend, demo data, statistics or fake ratings are introduced. Shared motion preference handling stops decorative movement when the activity is backgrounded, system animations are disabled or battery saver is enabled.

Verification: both debug APKs built and were installed on the Honor with account data retained. All 48 unit tests, five profile device checks, eight photo checks and four dashboard checks passed; the reduced-motion case advances only the frames needed to apply input, with no timed animation. The profile tests cover accurate missing-data completion, expand/collapse, view/edit/back, successful and failed saves, approval and large text. Real saved-photo rendering and manual scrolling/section/edit/back interactions were verified, and normal/scrolled screenshots were reviewed. A short debug timing sample reported 13 janky frames out of 232 (5.6%); this is device QA, not a release performance benchmark. No fixture media was uploaded or production profile fields changed by the agent.

## Your details refinement

The details section uses one clean grouped panel with inset dividers, custom monoline contact/briefcase/introduction vectors, softly layered icon tiles and a single expand control per row. Saved values open in a quiet inset panel and support text selection/copy. Section expansion, motion preferences, view-first navigation and the shared backend remain connected. The larger heading and reduced icon clutter improve the hierarchy without adding dependencies or operational data.

Both APKs built and were installed on the Honor with account data retained. All five existing profile checks passed, including large text, reduced motion, navigation, validation and save outcomes. The real grouped section and expanded saved values were inspected on the phone; screenshots were reviewed and no profile data was changed during QA.

## Saved field cards with motion

Expanded full name, email, phone, category, area, availability and introduction now use individual raised cards with dedicated monoline icons, a soft coloured backplate and readable, selectable values. Cards enter with a short staggered lift and perspective tilt when the section opens. This is a finite reveal; the text rests still for reading. System motion preferences bypass the reveal, and values wrap naturally without fixed heights.

Validation: both APKs built and were installed on the Honor without clearing account data. All six profile device checks passed, including a new long-email/large-text case. The paused-clock test checks offscreen field presence without waiting for a scroll frame; the large-text check exercises scrolling and visibility. Screenshots of normal saved values and large-text fixtures were reviewed. Real full-name, email and phone rendering remains read-only and no provider data was changed during testing.


## Whole-workspace depth and real skill requests

The light provider design now continues into registration, login/recovery/status, Skills & services, job lists, schedule/activity/conversations, booking details and receipts. Compact layered headers use raised lime service icons with finite perspective entrance and restrained scroll parallax. Workspace navigation has short directional fades/slides, bypassed by reduced-motion settings. Jobs expose the actual service and reserved duration. Profile remains read-only first; skills open a dedicated request/status screen and editing offers only approved primary skills. No new rendering dependencies or fictional operational data were introduced.

Registration submits selected service categories as untrusted pending requests. Service descriptions and account approval are separately reviewed by the admin before customer discovery. All 21 provider UI checks and 50 unit tests passed; APKs were installed on the Honor without clearing the main apps' data. See the shared connected workflow and versioned SQL policy tests for the full approval contract.

## Connected alerts and service location

The light depth-style Notifications screen now reads the signed-in recipient's durable inbox and offers real Android phone-alert permission/registration. Edit profile adds explicit service-base/radius controls for nearby discovery. Active booking details expose voluntary live location sharing with a visible background notification and Stop action; completed/closed work exposes no live sharing control. The service base and live job position are separate choices, with no automatic personal location publication.

## Provider home work shortcuts

Keep your work moving now has a dark teal Job requests feature card with an original code-drawn 3D inbox, lime action cue and a two-column grid for Active jobs, Schedule, Request history and Earnings. Small raised tiles use original monoline icons, matching round arrow controls, quiet borders and readable descriptions. The five cards retain their actual navigation callbacks and introduce no operational counts, fictional statuses, new media assets or rendering dependencies. The provider app stays in its light theme.

The section reveals once as it enters the viewport, with a short staggered lift and perspective settle. Cards and icon tiles react to press/release with a restrained spring. Lifecycle, system motion and battery settings use the existing motion policy; reduced-motion content renders immediately. Narrow content below 300 dp and text scaling of 1.3 or more switch the grid to full-width cards. Text wraps and cards grow instead of clipping.

Provider debug/app-test builds passed, and eight checks passed on Honor NIC-LX2: all five shortcut callbacks, large-text stacking, reduced motion, existing dashboard navigation/availability actions and welcome/hero accessibility checks. The updated provider APK was installed preserving account data. The running home section was visually inspected on the Honor; local evidence is `Documentation/connected-workflow/work-hub-real-device.png`. Backend data, customer app and admin code were not changed.

### Floating 3D motion

The requests illustration now separates the paper, tray and check badge into independently moving graphics layers. A six-second loop adds vertical float and perspective tilt; the four shortcut icon tiles float, tilt and subtly change depth with staggered phases. Scrolling adds a bounded perspective offset. Card text remains stationary after the entrance animation, and every shortcut remains immediately tappable.

One shared animation clock runs only while the section is visible and the existing motion policy allows it. Offscreen artwork stops updating; backgrounding, disabled system animations and battery saver disable the decorative motion. Transforms run in graphics layers without animating text layout or adding dependencies.

Provider debug and test APK builds passed. The final combined run passed all nine checks on Honor NIC-LX2, including a new frame comparison proving visible artwork motion, unchanged title bounds and stationary rendering after motion is disabled. The first run lost its Compose test activity during the large-text case; the work-hub suite and then the complete nine-check run passed on retry. The provider update was installed with account data retained. No backend operations or personal media were changed. Local test evidence is `Documentation/connected-workflow/work-hub-motion-final-tests.log`.

### Active jobs video background

The user's `active_jobs.mp4` is now the Active jobs card's decorative background. Its portrait source is cropped to the upper square, resized to 480 × 480 and encoded as a silent H.264 MP4. Forward/reverse playback forms a 16-second repeating loop without an abrupt end cut. The packaged clip is 444,545 bytes; a matching 11,474-byte WebP provides a static fallback. The source remains in the parent workspace. Schedule, Request history and Earnings keep their existing illustrations until the corresponding videos are supplied.

A native TextureView/MediaPlayer renders beneath a lavender readability overlay and the existing card action. It starts only while the card is visible and decorative motion is allowed. Leaving the viewport, backgrounding or reduced motion removes the view and releases its decoder/surface. The poster stays visible during preparation and on decoder failure. There are no media controls, audio, network media requests, extra playback dependencies or operational data changes.

Provider debug and instrumentation APKs built and were installed on Honor NIC-LX2 with account data retained. All ten device checks passed. The new check compares actual decoded frames, exercises the card callback, scrolls it out of view and back, and disables motion to verify decoder removal and the poster. The rendered card screenshot was reviewed for cropping, clipping and text readability. Local evidence: `Documentation/connected-workflow/active-jobs-video-device-tests.log` and `active-jobs-video-card.png`; the screenshot shows an isolated navigation fixture with no backend writes.
