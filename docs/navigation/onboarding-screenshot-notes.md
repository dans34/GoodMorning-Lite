# Runtime Screenshot Notes — Onboarding and Paywall

**Captured from:** user-provided screenshots of Routinery 3.30.0
**Date recorded:** 2026-10-14
**Evidence:** Direct visual observation of five screenshots. These establish the shown onboarding states, but not the full app navigation.

## Screenshot inventory (corrected to the supplied navigation order)

1. **Which routine should we build first?** — choice between Morning routine and Evening routine, represented by large illustrated cards. Original filename: `1000036010 (002).jpg`.
2. **What do you struggle with?** — multi-choice onboarding question with six options and an “I'm not sure” action; yellow progress indicator; disabled Next until a selection is made. Original filename: `1000036012 (002).jpg`.
3. **What time do you wake up?** — wheel-style time selector showing 6:00 AM; yellow progress fill and dark Next button. Original filename: `1000036011 (002).jpg`.
4. **This is how your day starts** — preview for a 6:00am morning routine, six minutes total, with three tasks: stretch and drink water, tidy the bed, and check the weather; a Looks good button. Original filename: `1000036013 (002).jpg`.
5. **Free for 7 days!** — subscription/trial screen with close control, trial timeline, annual price and Try for £0 button. Documented only to identify and exclude it from GoodMorning-Lite. Original filename: `1000036014 (002).jpg`.

## Observed onboarding sequence (user-confirmed order)

1. Ask whether to build a morning or evening routine first.
2. Ask what the user struggles with.
3. Ask the wake-up time (shown at 6:00 AM).
4. Preview the suggested routine and ask the user to confirm it.
5. The original flow then shows a trial/paywall screen; GoodMorning-Lite will omit this screen and continue into the app.

The user confirmed the order as **2, 1, 3, 4, 5** relative to the initially supplied screenshot list. The screenshots do not prove there are no intermediate screens.

## UI observations

- White background with generous vertical whitespace.
- Dark charcoal headings and body text, with muted grey supporting text.
- Bright yellow primary accent and onboarding progress colour.
- Rounded, very pale grey panels and selection cards.
- Large, full-width rounded primary buttons near the bottom.
- Friendly yellow circular mascot with a simple face and curved arrow motif.
- Large readable geometric sans-serif typography. Package analysis found Montserrat and Pretendard family indicators, but exact font use in each screenshot is not confirmed.
- Onboarding progress indicator near the top, with a yellow filled portion.
- Large tap targets, simple choices and minimal clutter.

These are design observations, not a claim that GoodMorning-Lite should copy proprietary image assets. Use independently implemented shapes/icons and appropriately licensed fonts/assets.

## GoodMorning-Lite decisions

- Keep short, optional onboarding asking the user's main difficulty, morning/evening preference and wake-up time.
- Allow skipping onboarding and changing preferences later.
- Generate one local starter routine and show an editable preview before saving.
- Do not include the subscription/trial screen, purchase SDK, billing flow or subscription tracking.
- Never require login or a network connection to complete setup.
- Persist setup answers and the routine locally.

## Still unknown

- Whether every new installation sees these screens in exactly this order.
- The full set of choices on intermediate screens and what each selection changes.
- Whether the routine preview can be edited directly at this stage.
- Exact font file, weights, spacing measurements and selected/unselected card states.
- What screen follows closing the trial/paywall in the original app.

## Image upload status

The screenshots are attached in the conversation. The available GitHub connection exposes text-file operations but no binary JPG upload operation. The actual JPG binaries have **not** been committed to GitHub by this action. This note records screenshot findings without pretending the image files themselves were uploaded. When binary upload is available, place the five originals under `docs/screenshots/reference/`.
