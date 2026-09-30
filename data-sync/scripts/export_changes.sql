-- export_changes.sql
--
-- Called by daily-sync.ps1 via sqlcmd. Prints INSERT statements for
-- every row changed since $(Since), one section per table, in the
-- fixed dependency order. The "-- TABLE:Name" markers are how
-- daily-sync.ps1 groups statements from multiple teammates' files
-- together by table before applying them.
--
-- Do not run this file by hand unless you're testing it -- it's meant
-- to be driven by daily-sync.ps1, which supplies $(Since).

SET NOCOUNT ON;

PRINT '-- TABLE:User';
SELECT
    'INSERT INTO [User] (user_id, username, email, password_hash, role, created_at, updated_at) VALUES (' +
    '''' + CAST(user_id AS NVARCHAR(36)) + ''', ' +
    '''' + REPLACE(username, '''', '''''') + ''', ' +
    '''' + REPLACE(email, '''', '''''') + ''', ' +
    '''' + REPLACE(password_hash, '''', '''''') + ''', ' +
    '''' + role + ''', ' +
    '''' + CONVERT(VARCHAR(30), created_at, 126) + ''', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM [User]
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:Category';
SELECT
    'INSERT INTO Category (category_id, name, description, created_at, updated_at) VALUES (' +
    '''' + CAST(category_id AS NVARCHAR(36)) + ''', ' +
    '''' + REPLACE(name, '''', '''''') + ''', ' +
    CASE WHEN description IS NULL THEN 'NULL' ELSE '''' + REPLACE(description, '''', '''''') + '''' END + ', ' +
    '''' + CONVERT(VARCHAR(30), created_at, 126) + ''', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM Category
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:Video';
SELECT
    'INSERT INTO Video (video_id, uploader_id, category_id, title, description, file_path, thumbnail_path, thumbnail_url, duration_seconds, privacy, view_count, created_at, updated_at) VALUES (' +
    '''' + CAST(video_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(uploader_id AS NVARCHAR(36)) + ''', ' +
    CASE WHEN category_id IS NULL THEN 'NULL' ELSE '''' + CAST(category_id AS NVARCHAR(36)) + '''' END + ', ' +
    '''' + REPLACE(title, '''', '''''') + ''', ' +
    CASE WHEN description IS NULL THEN 'NULL' ELSE '''' + REPLACE(description, '''', '''''') + '''' END + ', ' +
    '''' + REPLACE(file_path, '''', '''''') + ''', ' +
    CASE WHEN thumbnail_path IS NULL AND thumbnail_url IS NULL THEN 'NULL' ELSE '''' + REPLACE(COALESCE(thumbnail_path, thumbnail_url), '''', '''''') + '''' END + ', ' +
    CASE WHEN thumbnail_url IS NULL AND thumbnail_path IS NULL THEN 'NULL' ELSE '''' + REPLACE(COALESCE(thumbnail_url, thumbnail_path), '''', '''''') + '''' END + ', ' +
    CASE WHEN duration_seconds IS NULL THEN 'NULL' ELSE CAST(duration_seconds AS NVARCHAR(20)) END + ', ' +
    '''' + privacy + ''', ' +
    CAST(view_count AS NVARCHAR(20)) + ', ' +
    '''' + CONVERT(VARCHAR(30), created_at, 126) + ''', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM Video
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:Playlist';
SELECT
    'INSERT INTO Playlist (playlist_id, owner_id, name, description, privacy, thumbnail_url, is_hidden, created_at, updated_at) VALUES (' +
    '''' + CAST(playlist_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(owner_id AS NVARCHAR(36)) + ''', ' +
    '''' + REPLACE(name, '''', '''''') + ''', ' +
    CASE WHEN description IS NULL THEN 'NULL' ELSE '''' + REPLACE(description, '''', '''''') + '''' END + ', ' +
    '''' + REPLACE(privacy, '''', '''''') + ''', ' +
    CASE WHEN thumbnail_url IS NULL THEN 'NULL' ELSE '''' + REPLACE(thumbnail_url, '''', '''''') + '''' END + ', ' +
    CASE WHEN is_hidden = 1 THEN '1' ELSE '0' END + ', ' +
    '''' + CONVERT(VARCHAR(30), created_at, 126) + ''', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM Playlist
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:PlaylistVideo';
SELECT
    'INSERT INTO PlaylistVideo (playlist_video_id, playlist_id, video_id, position, added_at, updated_at) VALUES (' +
    '''' + CAST(playlist_video_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(playlist_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(video_id AS NVARCHAR(36)) + ''', ' +
    CAST(position AS NVARCHAR(20)) + ', ' +
    '''' + CONVERT(VARCHAR(30), added_at, 126) + ''', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM PlaylistVideo
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:Comment';
SELECT
    'INSERT INTO Comment (comment_id, video_id, user_id, parent_comment_id, content, created_at, updated_at) VALUES (' +
    '''' + CAST(comment_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(video_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(user_id AS NVARCHAR(36)) + ''', ' +
    CASE WHEN parent_comment_id IS NULL THEN 'NULL' ELSE '''' + CAST(parent_comment_id AS NVARCHAR(36)) + '''' END + ', ' +
    '''' + REPLACE(content, '''', '''''') + ''', ' +
    '''' + CONVERT(VARCHAR(30), created_at, 126) + ''', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM Comment
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:Like';
SELECT
    'INSERT INTO [Like] (like_id, user_id, video_id, created_at, updated_at) VALUES (' +
    '''' + CAST(like_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(user_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(video_id AS NVARCHAR(36)) + ''', ' +
    '''' + CONVERT(VARCHAR(30), created_at, 126) + ''', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM [Like]
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:Report';
SELECT
    'INSERT INTO Report (report_id, reporter_id, video_id, comment_id, reason, description, status, reviewed_by, created_at, reviewed_at, updated_at) VALUES (' +
    '''' + CAST(report_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(reporter_id AS NVARCHAR(36)) + ''', ' +
    CASE WHEN video_id IS NULL THEN 'NULL' ELSE '''' + CAST(video_id AS NVARCHAR(36)) + '''' END + ', ' +
    CASE WHEN comment_id IS NULL THEN 'NULL' ELSE '''' + CAST(comment_id AS NVARCHAR(36)) + '''' END + ', ' +
    '''' + reason + ''', ' +
    CASE WHEN description IS NULL THEN 'NULL' ELSE '''' + REPLACE(description, '''', '''''') + '''' END + ', ' +
    '''' + status + ''', ' +
    CASE WHEN reviewed_by IS NULL THEN 'NULL' ELSE '''' + CAST(reviewed_by AS NVARCHAR(36)) + '''' END + ', ' +
    '''' + CONVERT(VARCHAR(30), created_at, 126) + ''', ' +
    CASE WHEN reviewed_at IS NULL THEN 'NULL' ELSE '''' + CONVERT(VARCHAR(30), reviewed_at, 126) + '''' END + ', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM Report
WHERE updated_at > '$(Since)';

PRINT '-- TABLE:WatchHistory';
SELECT
    'INSERT INTO WatchHistory (watch_history_id, user_id, video_id, watched_at, watch_duration_seconds, updated_at) VALUES (' +
    '''' + CAST(watch_history_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(user_id AS NVARCHAR(36)) + ''', ' +
    '''' + CAST(video_id AS NVARCHAR(36)) + ''', ' +
    '''' + CONVERT(VARCHAR(30), watched_at, 126) + ''', ' +
    CASE WHEN watch_duration_seconds IS NULL THEN 'NULL' ELSE CAST(watch_duration_seconds AS NVARCHAR(20)) END + ', ' +
    '''' + CONVERT(VARCHAR(30), updated_at, 126) + ''');'
FROM WatchHistory
WHERE updated_at > '$(Since)';
