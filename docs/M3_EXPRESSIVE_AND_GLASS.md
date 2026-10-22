# Combining Material 3 Expressive and Liquid Glass Seamlessly

In Petal Browser, **Material 3 Expressive** and **Liquid Glass** are complementary design layers:
- **Material 3 Expressive** defines structural hierarchy, responsive typography, tokenized color schemes, bouncy spring physics, pill containers, and morphing FABs/buttons.
- **Liquid Glass** defines optical material physics: live backdrop sampling, Gaussian blur, frosted translucency, rim light refraction, and AGSL lens distortion.

---

## 1. Golden Rules of Coexistence

1. **Hierarchy of Transparency**:
   - The Root Window / Page Viewport contains the web content rendered through Chromium's `TextureView` backend.
   - Floating elements (Dock bar, Omnibox, Overflow Menu, Petal Sheet) apply `Modifier.liquidGlassChrome(...)` over a recorded `PetalGlassWallpaper`.
   - Inner Material 3 elements (Cards, Buttons, Group Rows) must use `petalGroupSurfaceColor()` or `Color.Transparent` in glass mode. Never place an opaque `MaterialTheme.colorScheme.surface` on top of glass.

2. **Expressive Motion + Optical Physics**:
   - Always use M3 Expressive spatial springs (`PetalSpring.ExpressiveSpatialSpec`) for opening sheets, expanding dialogs, and morphing tab grids.
   - During animation, blur filters are kept stable to avoid per-frame shader re-allocations.
   - Never draw double borders: if `isLiquidGlassEnabled()`, drop M3's `BorderStroke` since `liquidGlassChrome` automatically renders the glass rim highlight.

3. **Dynamic Material You Fallback**:
   - On devices below Android 12 (API 31) where hardware AGSL / RenderEffect blurs are unavailable, or when the user disables glass in settings (`sp_liquid_glass_containments`), the UI automatically falls back to clean, high-contrast Material 3 Expressive tonal surfaces (`surfaceContainerHigh`).

---

## 2. Component Guidelines

| Component | Material 3 Expressive Role | Liquid Glass Role |
|---|---|---|
| **PetalSheet** (Bottom Sheet) | M3 bottom drag handle, expressive expansion curves, pill actions | Transparent outer window, inner blurred glass panel, zero tonal shadow |
| **PetalPopupMenu** | M3 expressive corner radii (24dp+), high contrast icons | Floating frosted glass background with rim reflection |
| **ExpressiveFloatingDockBar** | M3 pill shape, bouncy scale-down press animations | Refracts the live webpage content directly beneath it |
| **PetalSlider / IconSwitch** | Expressive thumb morphing & rolling animations | Pill track with subtle glass refraction |
| **PetalPrivacyShieldSheet** | M3 expressive badge counters, toggle switches | Live glass backdrop over current site |
