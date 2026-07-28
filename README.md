# CP BenchMark (Core Engine)

Console-based Java app that fetches a public LeetCode profile by **username only**
(no password), scores it out of 10 using a weighted rubric, gives strengths/
weaknesses/suggestions, and saves history to local JSON files.

## Requirements
- JDK 17 or newer
- Maven 3.6+
- Internet access (to fetch LeetCode data, and to download the Jackson
  dependency the first time you build)

## Build

```bash
mvn clean package
```

This produces `target/lpr-analyzer.jar` (a "fat jar" with Jackson bundled in,
via the shade plugin — so you can run it with a single command, no classpath
juggling needed).

## Run — console version

```bash
java -jar target/lpr-analyzer.jar
```

You'll be prompted for a LeetCode username. The app will:
1. Try to fetch public stats via LeetCode's unofficial GraphQL API.
2. If that fails (network issue, private/nonexistent profile, or the API
   has changed shape since this was written), it automatically falls back
   to manual entry — you'll be prompted for each stat by hand. This means
   a demo never gets blocked by a flaky third-party endpoint.
3. Print a full score breakdown + feedback.
4. Save a timestamped snapshot to `./data/<username>.json`, and show you
   how your score changed since last time (progress tracking, DB-free).

## Run — AWT GUI version

The fat jar's manifest points at the console `Main` by default, so for the
GUI you need to specify the GUI's main class explicitly:

```bash
java -cp target/lpr-analyzer.jar com.lpr.gui.GuiMain
```

The GUI window:
- Takes a username, has an "Analyze" button (pressing Enter in the field
  also works).
- Runs the network fetch on a background thread so the window doesn't freeze.
- If auto-fetch fails, opens a modal manual-entry dialog with the same
  fields as the console fallback.
- Prints the same score report + feedback into a read-only text area.
- Saves to the same `./data/<username>.json` history files as the console
  version — both versions share history, since they use the same
  `FileStorageManager`.

## Project structure

```
src/main/java/com/lpr/
├── Main.java                     # console entry point / orchestration
├── model/
│   ├── UserProfile.java          # raw profile stats
│   └── LPRScoreResult.java       # computed score + feedback
├── fetch/
│   ├── LeetCodeClient.java       # GraphQL calls via HttpClient + Jackson
│   └── LeetCodeFetchException.java
├── scoring/
│   └── LPRScoringEngine.java     # the weighted rubric (documented per-method)
├── storage/
│   └── FileStorageManager.java   # JSON file persistence (stand-in for JDBC/MySQL)
└── gui/
    ├── GuiMain.java               # AWT entry point
    ├── LPRFrame.java              # main window (Frame + TextField/Button/TextArea)
    └── ManualEntryDialog.java     # modal AWT Dialog for manual-entry fallback
```

## Why file storage instead of MySQL (for now)

`FileStorageManager` implements the same job a `JdbcStorageManager` would
(`save(...)`, history lookups) but writes JSON files under `./data/` instead
of a database. When you're ready to add MySQL:

1. Create `storage/JdbcStorageManager.java` with the same two public methods
   (`save`, `getPreviousScore`) that `FileStorageManager` has.
2. Swap the one line in `Main.java` that instantiates the storage manager.

Nothing else in the app needs to change — this is the benefit of coding
against a simple, narrow interface from day one.

## Known limitations / things to mention in your report

- LeetCode has **no official public API**. This project uses the same
  GraphQL endpoint the LeetCode website itself calls (undocumented,
  unofficial, can change without notice). This is why manual entry exists
  as a fallback — treat it as a first-class feature, not an afterthought.
- Acceptance rate is approximated from public submission-count data, since
  LeetCode's public API doesn't expose a direct accepted-vs-attempted count.
- Only LeetCode is supported currently. Codeforces/CodeChef/HackerRank/GFG
  support is realistic future work — each would need its own client class,
  which is straightforward given the current structure but out of scope
  for a first submission.
