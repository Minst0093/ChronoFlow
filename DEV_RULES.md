# DEV RULES — Compose clickable Indication crash (PlatformRipple)

Problem
-------
On some devices / Compose + Material combinations the app crashed during composition with:

java.lang.IllegalArgumentException: clickable only supports IndicationNodeFactory instances provided to LocalIndication, but Indication was provided instead. The Indication instance provided here was: androidx.compose.material.ripple.PlatformRipple

Root cause
----------
The Compose foundation `clickable`/`selectable` modifier expects an Indication implemented as the newer IndicationNodeFactory when sourced from `LocalIndication`. Some Material implementations (e.g. `PlatformRipple`) implement the older `Indication` interface and cause a type mismatch when foundation's clickable attaches, leading to IllegalArgumentException on attach/measure.

What we changed
---------------
- Replaced unsafe/implicit clickable/selectable usages that relied on mismatched Indication with an explicit overload that receives an Indication and an InteractionSource.
- Restored ripple by explicitly passing `LocalIndication.current` together with `remember { MutableInteractionSource() }`.

Correct patterns (preferred)
---------------------------
- Use the clickable/selectable overload that accepts `indication` and `interactionSource` and pass `LocalIndication.current`:

```kotlin
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember

Box(
  modifier = Modifier.clickable(
    indication = LocalIndication.current,
    interactionSource = remember { MutableInteractionSource() }
  ) { /* onClick */ }
)
```

- For `selectable` similarly:

```kotlin
.selectable(
  selected = isSelected,
  onClick = { /* ... */ },
  interactionSource = remember { MutableInteractionSource() },
  indication = LocalIndication.current
)
```

Alternative (no ripple)
------------------------
If you intentionally want no visual ripple, pass `indication = null` (but still pass an interactionSource). This avoids the type check but disables ripple:

```kotlin
.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { ... }
```

Prevention checklist for PRs
----------------------------
- Always use the `indication` + `interactionSource` overload when using foundation `clickable`/`selectable` in code that mixes Compose Material and Compose Foundation.  
- Search for `.clickable(`, `.selectable(` and `.combinedClickable(` in PR diffs and verify the usage.  
- Prefer `LocalIndication.current` to retain Material ripple unless intentionally disabled.  
- If adding a custom Indication implementation, implement the new IndicationNodeFactory API (not the legacy Indication) to remain compatible.

How to search
-------------
- Find usages:
  - ripgrep: `rg "\\.clickable\\(|\\.selectable\\(|\\.combinedClickable\\("`
  - confirm any `indication = null` occurrences if intentional

If this resurfaces
------------------
1. Paste the full stack trace (start at FATAL EXCEPTION).  
2. Check recent changes for direct `.clickable {}` usages or custom Indication implementations.  
3. If needed, prefer explicit `LocalIndication.current` until libraries synchronize on IndicationNodeFactory.

Signed-off-by: dev-team (automated edit)


