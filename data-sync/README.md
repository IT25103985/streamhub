# data-sync/

This folder is how the six local StreamHubDB databases stay in sync. Nobody edits files in here directly — `daily-sync.ps1` manages everything.

## One-time setup (each member, once)

1. Run `docs/schema.sql` once against your local `StreamHubDB`.
2. In this folder, create a file called `member.config` containing just your first name in lowercase, e.g.:
   ```
   sandaruwan
   ```
3. Make sure `sqlcmd` works from your terminal (it installs with SQL Server / SSMS). Test with `sqlcmd -?`.
4. Add this to the repo's `.gitignore` if it isn't already there:
   ```
   data-sync/.state/
   data-sync/member.config
   ```
   (`member.config` is personal to your machine — everyone has their own name, so it shouldn't be committed.)

## Daily use

Once a day, from the repo root:

```
powershell -ExecutionPolicy Bypass -File data-sync\daily-sync.ps1
```

That's it. It pulls the latest code and data, applies your teammates' new rows to your local database, exports your own new rows, and pushes them — all in one run.

## How it works, briefly

- Every export file is named `<member>_<date>.sql` (e.g. `sandaruwan_2026-08-09.sql`) and contains only that member's new/changed rows since their last sync, grouped by table.
- Because every table uses UUID primary keys, two members' files never clash on an ID.
- When applying teammates' files, the script always processes tables in a fixed order — User, Category, Video, Playlist, PlaylistVideo, Comment, Like, Report, WatchHistory — regardless of which file a statement came from, so foreign key references always exist before they're needed.
- A local `SyncLog` table (created by `docs/schema.sql`) records which files have already been applied to your machine, so re-running the script never double-inserts anything.
