# Implementation Plan: Unified Dark Theme & Single-Prompt Onboarding

## Overview
The goal is to unify the app's visual identity across all screens with a deep charcoal dark theme (`#0F172A`) and vibrant purple accents (`#8B5CF6`), and streamline onboarding into a single 2-step flow with persistent session storage so users are only prompted for their 6-digit password/code once.

---

## 1. Unified Deep Charcoal & Purple Theme
Standardize all screens so every view has an identical visual feel (dark charcoal background, slate surface cards, purple accents, crisp white text):
- **Theme Palette**:
  - Background: `Color(0xFF0F172A)` (Slate 900 / Deep Charcoal)
  - Surface / Cards: `Color(0xFF1E293B)` (Slate 800)
  - Surface Border / Dividers: `Color(0xFF334155)` (Slate 700)
  - Primary Accent: `Color(0xFF8B5CF6)` (Purple 500) & `Color(0xFF7C3AED)` (Purple 600)
  - Secondary Accent: `Color(0xFF06B6D4)` (Cyan 500)
  - Text Primary: `Color(0xFFF8FAFC)` (White / Slate 50)
  - Text Secondary: `Color(0xFF94A3B8)` (Slate 400)
- **Screens to Update**:
  - `Theme.kt`: Set M3 DarkColorScheme defaults and standard theme tokens.
  - `DashboardScreen.kt`: Replace light backgrounds (`Slate50`, `White`) with `DarkBackground` and `DarkSurface`. Update cards, chips, stats badges, and TopAppBar.
  - `SettingsScreen.kt`: Update background and cards to deep charcoal/purple style.
  - `PlatformsScreen.kt`: Update list items, dialogs, and background to dark theme.
  - `ExportScreen.kt` & `ExportHistoryScreen.kt`: Update backgrounds, cards, and buttons to match.

---

## 2. Onboarding & Authentication Flow ("Powered by Al Mudafioon")
Overhaul the entry experience into a unified 2-step card flow that prompts for the code exactly **once**:
- **Branding**:
  - Display **"Powered by Al Mudafioon"** prominently at the top with a refined security badge.
- **Step 1 (Email)**:
  - Header: *"Welcome to Al Mudafioon Security"*
  - Prompt: *"Enter your email address"*
  - Outlined text field with email validation and a prominent **"Continue"** purple button.
  - Website link / help button pointing to `https://mf-panel.vercel.app/`.
- **Step 2 (6-Digit Code)**:
  - Smooth in-place animated card transition.
  - Prompt: *"Enter your 6-digit passcode"* with email display and "Change" link.
  - 6 individual PIN entry boxes with auto-advance, backspace handling, and paste support.
  - "Verify & Access" button.
- **Fix Double Password Prompt & Session Persistence**:
  - Verify code against backend/linking repository once.
  - On success, persist `is_logged_in = true` and `device_token`/`user_email` in SharedPreferences.
  - Remove duplicate `DeviceLinkingScreen` from the initial routing stack.
  - `MainActivity` checks session state on launch: if active, bypass auth directly to `ROUTE_DASHBOARD`.
  - Only show auth again when the user explicitly clicks "Logout" in Settings.

---

## 3. Verification Plan
1. **Compilation**: Execute `compile_applet` to confirm clean Gradle build.
2. **Session Logic Test**: Verify app launch routes to Dashboard when active, and only asks for email + 6-digit passcode on clean launch / logout.
3. **Visual Audit**: Verify theme consistency across Dashboard, LinkQueue, Screenshots Gallery, Settings, and Platforms screens.
