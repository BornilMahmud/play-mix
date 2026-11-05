# Contributing to Play Mix

Thank you for your interest in contributing to **Play Mix**!

## Core Engineering Principles

1. **Strict Audio Category Isolation**
   - Every audio file must be classified into one of the 8 `AudioCategory` buckets (`MUSIC`, `ALARMS`, `VOICE_RECORDINGS`, `RINGTONES`, `NOTIFICATION_SOUNDS`, `PODCASTS`, `AUDIOBOOKS`, `UNCLASSIFIED`).
   - Uncertain or ambiguous audio clips must **never** be automatically classified as `MUSIC`.
   - The Music Home screen and Music Library must only display `MUSIC` tracks that are visible in `MUSIC`.

2. **Non-Destructive Media Category Management**
   - **Moving changes category**: Updating `primaryCategory` sets `isManualClassification = true` and preserves the physical file on disk.
   - **Removing changes visibility**: Adding a category to `removedFromCategoriesCsv` hides the item from that category without deleting the file or moving it.
   - **Deleting removes the original file**: Physical deletion requires explicit user confirmation with the filename displayed.

3. **Lightweight Performance & Battery Efficiency**
   - Target smooth 60/120 Hz scrolling and screen-off background playback on the **Realme GT Master Edition**.
   - Avoid heavy background polling when playback is paused.
   - Keep `Three.js` strictly confined to the self-disposing startup intro overlay (`ThreeJsIntroOverlay.kt`).

## Pull Request Checklist

- [ ] Run `gradle :app:testDebugUnitTest` and ensure all unit and Robolectric tests pass.
- [ ] Run `gradle lintDebug` and verify zero fatal lint errors.
- [ ] Run `gradle assembleDebug` to confirm APK packaging succeeds.
- [ ] Respect GPL-3.0 licensing and attribution requirements.
