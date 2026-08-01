# MTBsim

MTBsim is a light-hearted first-person mountain-bike trail game for Android. The rider flows through a mountainous forest and responds to large, readable gesture prompts for wooden skinnies, drops, jumps, berms, rock rolls, teeter-totters, canoe chutes, and A-frames.

## Gameplay

- **First-person cockpit:** animated arms, grips, handlebars, bike frame, steering, and rider lean.
- **Gesture trail features:** taps, broad swipes, holds, releases, and a two-finger chute gesture.
- **Forgiving timing:** every feature activates well in advance and remains valid until just behind the rider.
- **Flow-based difficulty:** speed rises gradually from about 22 km/h to a capped 66 km/h.
- **Procedural scenery:** layered mountains, parallax forest, roots, birds, and occasional foreground branches.
- **No downloaded art assets:** the consistent cartoon/anime-inspired look is rendered with Android Canvas primitives.

### Gesture guide

| Feature | Gesture |
|---|---|
| Wooden skinny | Alternate left/right grip taps |
| Drop | Swipe down to shift back, then up to centre |
| Jump | Centre tap to preload, then swipe down to float the landing |
| Left/right berm | Broad horizontal swipes in the turn direction |
| Rock roll | Press and hold in the centre |
| Teeter-totter | Hold in the centre, then release |
| Canoe chute | Two-finger downward swipe |
| A-frame | Swipe down for the climb, then up over the crest |

## Build

GitHub Actions is the supported build environment. Every push and pull request runs unit tests, lint, and builds both debug and release APKs.

1. Open the repository's **Actions** tab.
2. Select **Build Android APK**.
3. Run the workflow, or open a run triggered by a push.
4. Download the `MTBsim-apks` artifact.

The workflow can also be started manually. Local Android Studio builds are intentionally not the documented path, keeping the project reproducible from a clean CI runner.

## Architecture

- `GameEngine`: deterministic speed, score, spawn, feature, and failure state.
- `FeatureRules`: data-driven gesture sequences and generous interaction distances.
- `GestureClassifier`: pure Kotlin gesture classification, covered by unit tests.
- `MountainBikeGameView`: frame loop, touch routing, procedural world rendering, HUD, and first-person bike animation.

## License

MIT
