# Chitthi

A quiet SMS app for Android that sorts your messages for you. In India most SMS are OTPs, bank alerts and promos, so Chitthi keeps the people you talk to apart from all of that.

Built with Kotlin and Jetpack Compose (Material 3 Expressive). Nothing leaves the phone: sorting runs on the device, with rules and, where the phone supports it, Gemini Nano.

## What it does

- **Four tabs.** Important (OTPs, banks, deliveries), Personal, Promos and Unknown. Swipe between them, or move a chat to another tab and Chitthi remembers.
- **Codes first.** A fresh OTP sits at the top of Important, and Chitthi can copy it to the clipboard the moment it arrives. Old codes from businesses can clear themselves out after a set age.
- **Send later.** Hold the send button to schedule a message: tonight, tomorrow morning, or a time you pick. It goes out on time even if the phone was restarted, and a clock on the chat row shows something is waiting.
- **Proper notifications.** Messages from people show as Android conversations, with their photo, a reply box and a place in the conversation shelf. Promos can arrive silently.
- **Faces that mean something.** Contacts show their photo, businesses show their logo. Tap a person's face to open their contact, or to save a stranger's number.
- **Smart replies** from Gemini Nano on supported phones. It also names unknown senders and re-sorts messages the rules are unsure about.
- **Made to feel good.** Shapes that morph when you press them, a header that tucks away as you scroll, blur under the bars, a few light haptics, and the old Messages blue (or Material You colours from your wallpaper).

## Small things worth knowing

| Do this | And |
|---|---|
| Hold a tab chip | Mark every chat in that tab as read |
| Hold a chat | Pin, move to another tab, mark read, delete |
| Hold the send button | Send later |
| Tap a scheduled message in a chat | Send now, reschedule or cancel |
| Tap a person's face | Open their contact, or save the number |
| Scroll up anywhere | The header comes back |

## Install

Download the APK from [Releases](https://github.com/jaswantk917/chitthi/releases), install it, then open Chitthi and let it become your SMS app. It can also run read-only next to your current SMS app.

Needs Android 10 or newer on a 64-bit ARM phone (practically every phone from the last few years).

## Permissions

| Permission | Why |
|---|---|
| SMS (read, send, receive, MMS) | It's an SMS app |
| Contacts | Names and photos for numbers |
| Phone state | Picks the SIM to send from |
| Notifications | New message alerts |
| Exact alarms, start at boot | Scheduled messages go out on time |
| Internet | MMS downloads, and business logos by their website |

## Build

```sh
./gradlew :app:assembleDebug        # debug build, installs next to a release one
./gradlew :app:testDebugUnitTest    # unit tests
```

Release builds sign with the keystore named by the `HEYRE_KEYSTORE`, `HEYRE_KEYSTORE_PASSWORD`, `HEYRE_KEY_ALIAS` and `HEYRE_KEY_PASSWORD` Gradle properties (for example in `~/.gradle/gradle.properties`). Without them the release APK is unsigned.

## Status

A personal project, built for daily use on a Pixel. Issues and ideas are welcome.
