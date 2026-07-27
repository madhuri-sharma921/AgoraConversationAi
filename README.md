# Filler-Free — Android + Agora Conversational AI

This is the real `agent-quickstart-android` project (cloned from
AgoraIO-Conversational-AI/agent-quickstart-android), with the "Filler-Free"
feature merged into it. The original quickstart's own README is preserved at
`docs/original-quickstart-readme.md`.

**The idea:** a live speech coach. You explain something out loud — a bug
you fixed, a project, an interview answer. The AI agent interrupts
immediately (under 8 words) the moment you ramble, repeat filler words, or
go vague — then goes quiet again the moment you're being clear. Silence is
the reward for a clean explanation.

## Architecture

- `fillerfree/domain` — pure Kotlin, zero Android dependencies, fully
  unit-testable: filler/repetition detection (`AnalyzeTranscriptUseCase`),
  session stats, summary generation (`BuildSessionSummaryUseCase`)
- `fillerfree/data` — `TranscriptAnalyticsRepositoryImpl`, in-memory,
  implements the domain repository contract
- `fillerfree/config` — `CoachAgentPromptBuilder`: the actual product logic.
  This builds the system prompt that controls when and how the agent
  interrupts. If you're asked "what is the AI actually doing," this file is
  the honest answer — tune it here before touching any UI.
- `fillerfree/presentation` — MVI-style `FillerFreeViewModel` (StateFlow) +
  three Compose screens (topic select → live session → summary), with a
  deliberately blunt dark "scoreboard" visual theme instead of a soft chat UI

The transport layer — Agora RTC/RTM session, token bootstrap, agent
invite/leave, barge-in detection — is the original quickstart code, reused
as-is. Filler-Free only adds a thin analytics layer on top of the transcript
stream it already exposes.

## What was changed vs. the stock quickstart (exact diff, three files)

1. **`MainActivity.kt`** — launches `FillerFreeScreen`/`FillerFreeViewModel`
   instead of `ConversationScreen`/`ConversationViewModel`. The mic
   permission request flow is preserved exactly as the original — Filler-Free's
   "Start talking" button is gated behind the same `RECORD_AUDIO` permission
   check before `startSession()` runs.
2. **`data/ConversationAgoraApi.kt`** — `inviteAgent(...)` gained an optional
   `systemPrompt: String? = null` parameter, forwarded as `system_prompt` in
   the JSON request body.
3. **`data/ConversationRepository.kt`** — same optional parameter threaded
   through from the ViewModel to the API layer.

That `system_prompt` field was **already fully supported server-side**
(`server/app/schemas.py` → `JoinRequest.system_prompt` → `agora_client.py`
→ `.with_llm(system_messages=[...])`) — the Python backend just had no
Android caller sending it yet. No server code was changed.

Everything else in the original quickstart (RTC session manager, turn
manager, barge-in detector, audio pipeline, theme, Python server) is
untouched.

## Setup — needs your own Agora account, can't be pre-generated

This zip intentionally does **not** include Agora credentials or a running
backend, since those are tied to your own Agora account.

```bash
# 1. Install the Agora CLI and log in with YOUR Agora account
curl -fsSL https://dl.agora.io/cli/install.sh | sh
agora login

# 2. Bind this existing folder to an Agora project (writes local.properties)
agora quickstart env write . --template android --project <your-project-name>

# 3. Sanity-check the setup
agora project doctor --deep
```

Then run the Python backend (needed for token bootstrap + agent invite):
```bash
cd server
pip install -r requirements.txt
./run.sh
```
See `docs/local-tunnels.md` if you're testing on a physical device and need
to tunnel the backend.

Then open the root folder in Android Studio, let Gradle sync, build, and run.

## Demo script (45–60 seconds)

1. Open the app, pick "Explain a bug you fixed"
2. Tap "Start talking", grant mic permission
3. Explain something real, but ramble a little on purpose — watch the agent
   cut in live 2–3 times (FILLERS/CUT-INS counters tick up, screen briefly
   flashes amber on each interruption)
4. Tap "End session" → closing card shows your top habit
5. Tap "Try again, tighter" → same explanation, cleaner, fewer interruptions

No narration needed — the live interruption is the whole pitch.

## Known limitations (worth being upfront about)

- Client-side filler/repetition detection (`AnalyzeTranscriptUseCase`) is a
  word-list + overlap-ratio heuristic, not real NLP — good enough for a
  hackathon demo, not production-grade.
- The interruption *behavior itself* is driven by the LLM following the
  system prompt, combined with Agora's own turn-detection/barge-in
  pipeline — the on-screen counters are analytics layered on top of that
  stream, not what triggers the interruption.
- No persistence across sessions (by design, for MVP simplicity) — every
  session starts fresh.
