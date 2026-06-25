# Guerrilla 450

**Low-power motorcycle navigation for the Royal Enfield Tripper Dash — with the phone screen off.**

Guerrilla 450 is an open-source Android app for the **Royal Enfield Himalayan 450**. It projects turn-by-turn navigation onto the bike's round **Tripper TFT dash** without cooking the phone in your tank bag.

> ⚠️ Independent, community project. **Not affiliated with, endorsed by, or supported by Royal Enfield.** The dash link is unofficial, so use it at your own risk. Guerrilla 450 only streams a video feed to the Tripper **display** over Wi-Fi (and reads the joystick) — it never touches the ECU, engine, brakes or anything you ride with, and it can't modify the bike. The realistic worst case is the dash simply doesn't show the stream, or needs a power-cycle. Validated on firmware **11.63**; other firmwares may behave differently.

---


---

## How it works

```
Google Maps share ─▶ route (OSRM) ─▶ off-screen map render (Canvas)
                                              │
                                   MediaCodec H.264 (hardware)
                                              │
                                     RTP over UDP :5000
                                              ▼
                                          Tripper Dash
        (K1G control plane over UDP broadcast :2000  ·  RSA-1024 + AES-256 auth)
```

The dash speaks a binary Wi-Fi protocol ("K1G"): a stateful RSA/AES handshake, then it decodes an H.264/RTP stream over UDP. Guerrilla 450 implements the link layer in Kotlin and feeds the dash a map it renders itself — the dash doesn't care what produces the video.

---

## Tech stack

- **Kotlin** + **Jetpack Compose** (native Android)
- **MediaCodec** hardware H.264 encode → custom RTP packetizer → UDP
- **MapLibre** + **OpenFreeMap** for the map (keyless); **OSRM** for road routing
- **SQLite** on-device as the source of truth; **Firebase** (optional) for sync
- **TextToSpeech** for voice; raw `LocationManager` GPS

---

## Building it yourself

```bash
git clone https://github.com/harshith001/Guerrilla450.git
cd Guerrilla450
./gradlew :app:assembleDebug
```

No API keys needed. The map is keyless (OpenFreeMap) and the app runs fully local.

**Optional — cross-device sync:** create your own [Firebase](https://firebase.google.com) project (Auth + Firestore), download its `google-services.json` into `app/`, and rebuild. Without that file the app simply skips sync and stays local. (Your `google-services.json` is git-ignored — never commit it.)

> **You'll need your own bike.** The dash protocol can only be fully validated against real hardware. Verified on firmware **11.63**; other firmwares may differ in the handshake.

---

## Status & roadmap

The navigation core — discover, connect, stream, route, reroute, free-roam, voice — works end-to-end against a real Tripper Dash (fw 11.63). Garage, fuel diary, and ride recording are functional and persisted.

**In progress / planned:**
- Move routing off the public OSRM demo server
- Downloadable offline map regions for no-signal mountain riding
- Per-ABI APK splits (the MapLibre native libs make the debug APK large)
- Media now-playing overlay

---

## License

Licensed under the **Apache License, Version 2.0** — see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).

**Trademarks / affiliation:** Guerrilla 450 is an independent community project and is **not affiliated with, endorsed by, or supported by Royal Enfield**. "Royal Enfield", "Himalayan", and "Tripper" are trademarks of their respective owners, used here only descriptively to identify compatible hardware. The dash link is unofficial and exists solely for interoperability with hardware you own — use at your own risk.



## Credits

This project is built on the foundation laid by **[adityadasika21](https://github.com/adityadasika21)**, the original creator of [NorthStar](https://github.com/adityadasika21/NorthStar) — the pioneering open-source app that first cracked the Tripper dash protocol and proved that screen-off navigation on the Himalayan 450 was possible. None of this exists without his work.

The dash protocol understanding is also cross-checked against [**better-dash**](https://github.com/norbertFeron/better-dash) by Norbert Feron (Apache-2.0); see [`NOTICE`](NOTICE).
