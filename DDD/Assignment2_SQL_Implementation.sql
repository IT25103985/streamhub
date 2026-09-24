-- ============================================================
-- IT2140 : Database Design and Development
-- Assignment Part 02 - SQL Implementation
-- Group: 2026-Y2-S1-MLB-B2G2-07
-- 
-- StreamHub - Online Video Browsing and Library System
-- Microsoft SQL Server (SQL Server Express)
-- ============================================================

USE [StreamHubDB];
GO

-- ============================================================
-- PART D : SQL QUERIES & OUTPUTS  (20%)
-- Five required query types:
--   1. Simple SELECT
--   2. JOIN
--   3. Aggregation
--   4. GROUP BY / HAVING
--   5. Subquery
-- ============================================================

-- -------------------------------------------------------
-- Query 1 : Simple SELECT
-- Purpose  : Retrieve all PUBLIC, non-hidden videos with
--            their title, uploader's username, duration,
--            and upload date; ordered by newest first.
-- -------------------------------------------------------
SELECT
    v.video_id,
    v.title,
    u.username        AS uploader,
    v.duration_seconds,
    v.view_count,
    v.created_at      AS uploaded_at
FROM   [dbo].[Video] v
JOIN   [dbo].[User]  u  ON u.user_id = v.uploader_id
WHERE  v.visibility = 'PUBLIC'
  AND  v.is_hidden  = 0
ORDER  BY v.created_at DESC;
GO

-- -------------------------------------------------------
-- Query 2 : JOIN (multi-table)
-- Purpose  : For each video, show:
--            - title
--            - uploader username
--            - category name
--            - total likes
--            - total comments
--            - total dislikes
-- This gives a dashboard-style engagement summary.
-- -------------------------------------------------------
SELECT
    v.video_id,
    v.title,
    u.username                                 AS uploader,
    c.name                                     AS category,
    v.view_count,
    COUNT(DISTINCT l.like_id)                  AS total_likes,
    COUNT(DISTINCT d.dislike_id)               AS total_dislikes,
    COUNT(DISTINCT cm.comment_id)              AS total_comments
FROM       [dbo].[Video]      v
INNER JOIN [dbo].[User]       u  ON u.user_id     = v.uploader_id
LEFT  JOIN [dbo].[categories] c  ON c.id          = v.category_id
LEFT  JOIN [dbo].[Like]       l  ON l.video_id    = v.video_id
LEFT  JOIN [dbo].[dislike]    d  ON d.video_id    = v.video_id
LEFT  JOIN [dbo].[Comment]    cm ON cm.video_id   = v.video_id
                                 AND cm.is_hidden = 0
WHERE v.is_hidden = 0
GROUP BY
    v.video_id, v.title, u.username, c.name, v.view_count
ORDER BY
    total_likes DESC;
GO

-- -------------------------------------------------------
-- Query 3 : Aggregation
-- Purpose  : Calculate overall platform statistics
-- -------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM [dbo].[User])                         AS total_users,
    (SELECT COUNT(*) FROM [dbo].[Video]  WHERE is_hidden = 0)   AS total_videos,
    (SELECT COUNT(*) FROM [dbo].[Like])                         AS total_likes,
    (SELECT COUNT(*) FROM [dbo].[dislike])                      AS total_dislikes,
    (SELECT COUNT(*) FROM [dbo].[Comment] WHERE is_hidden = 0)  AS total_comments,
    (SELECT COUNT(*) FROM [dbo].[Follow])                       AS total_follows,
    (SELECT AVG(CAST(duration_seconds AS FLOAT))
       FROM [dbo].[Video]
      WHERE duration_seconds IS NOT NULL
        AND is_hidden = 0)                                       AS avg_duration_seconds;
GO

-- -------------------------------------------------------
-- Query 4 : GROUP BY / HAVING
-- Purpose  : Find categories that have more than 1 video,
--            showing video count and total views per category.
-- -------------------------------------------------------
SELECT
    c.name                  AS category_name,
    COUNT(v.video_id)       AS video_count,
    SUM(v.view_count)       AS total_views,
    AVG(v.view_count)       AS avg_views_per_video,
    MAX(v.view_count)       AS max_views
FROM       [dbo].[categories] c
INNER JOIN [dbo].[Video]      v ON v.category_id = c.id
WHERE      v.is_hidden = 0
GROUP BY   c.id, c.name
HAVING     COUNT(v.video_id) > 1
ORDER BY   total_views DESC;
GO

-- -------------------------------------------------------
-- Query 5 : Subquery
-- Purpose  : Find registered users who have never uploaded
--            any video (viewer-only accounts).
-- -------------------------------------------------------
SELECT
    u.user_id,
    u.username,
    u.email,
    u.role,
    u.created_at
FROM   [dbo].[User] u
WHERE  u.user_id NOT IN (
           SELECT DISTINCT uploader_id
           FROM   [dbo].[Video]
       )
  AND  u.role = 'USER'
ORDER  BY u.created_at DESC;
GO

-- -------------------------------------------------------
-- Bonus Query 6 : Correlated Subquery
-- Purpose  : Retrieve the most-liked video for each uploader.
-- -------------------------------------------------------
SELECT
    u.username,
    v.title                                   AS top_video,
    (SELECT COUNT(*)
       FROM [dbo].[Like] l
      WHERE l.video_id = v.video_id)          AS like_count,
    v.view_count
FROM   [dbo].[User]  u
JOIN   [dbo].[Video] v ON v.uploader_id = u.user_id
WHERE  v.is_hidden  = 0
  AND  (SELECT COUNT(*) FROM [dbo].[Like] l WHERE l.video_id = v.video_id)
       = (
            SELECT MAX(sub_like_count)
            FROM (
                SELECT COUNT(l2.like_id) AS sub_like_count
                FROM   [dbo].[Video] v2
                LEFT   JOIN [dbo].[Like] l2 ON l2.video_id = v2.video_id
                WHERE  v2.uploader_id = u.user_id
                  AND  v2.is_hidden   = 0
                GROUP  BY v2.video_id
            ) AS sub
         )
ORDER BY u.username;
GO


-- ============================================================
-- PART E : STORED FUNCTION / PROCEDURE  (15%)
-- ============================================================

-- -------------------------------------------------------
-- Scalar Function : fn_GetVideoEngagementScore
-- Returns a weighted engagement score for a video.
-- Score = (likes * 3) + (comments * 2) + (views * 0.01)
--           - (dislikes * 1)
-- -------------------------------------------------------
IF OBJECT_ID('dbo.fn_GetVideoEngagementScore', 'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_GetVideoEngagementScore;
GO

CREATE FUNCTION dbo.fn_GetVideoEngagementScore
(
    @video_id   UNIQUEIDENTIFIER
)
RETURNS FLOAT
AS
BEGIN
    DECLARE @likes        INT;
    DECLARE @dislikes     INT;
    DECLARE @comments     INT;
    DECLARE @views        INT;
    DECLARE @score        FLOAT;

    SELECT @likes = COUNT(*)
    FROM   [dbo].[Like]
    WHERE  video_id = @video_id;

    SELECT @dislikes = COUNT(*)
    FROM   [dbo].[dislike]
    WHERE  video_id = @video_id;

    SELECT @comments = COUNT(*)
    FROM   [dbo].[Comment]
    WHERE  video_id  = @video_id
      AND  is_hidden = 0;

    SELECT @views = view_count
    FROM   [dbo].[Video]
    WHERE  video_id = @video_id;

    SET @score = (@likes    * 3.0)
               + (@comments * 2.0)
               + (@views    * 0.01)
               - (@dislikes * 1.0);

    RETURN @score;
END;
GO


-- -------------------------------------------------------
-- Stored Procedure : sp_GetTopVideos
-- Parameters:
--   @TopN       INT              - number of top videos
--   @CategoryId UNIQUEIDENTIFIER (NULL = all categories)
-- Uses fn_GetVideoEngagementScore to rank videos.
-- -------------------------------------------------------
IF OBJECT_ID('dbo.sp_GetTopVideos', 'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_GetTopVideos;
GO

CREATE PROCEDURE dbo.sp_GetTopVideos
(
    @TopN        INT                  = 10,
    @CategoryId  UNIQUEIDENTIFIER     = NULL
)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP (@TopN)
        v.video_id,
        v.title,
        u.username                                      AS uploader,
        c.name                                          AS category,
        v.view_count,
        dbo.fn_GetVideoEngagementScore(v.video_id)      AS engagement_score,
        v.created_at
    FROM       [dbo].[Video]      v
    INNER JOIN [dbo].[User]       u  ON u.user_id    = v.uploader_id
    LEFT  JOIN [dbo].[categories] c  ON c.id         = v.category_id
    WHERE      v.is_hidden  = 0
      AND      v.visibility = 'PUBLIC'
      AND      (@CategoryId IS NULL OR v.category_id = @CategoryId)
    ORDER BY
        dbo.fn_GetVideoEngagementScore(v.video_id) DESC;
END;
GO

-- -------------------------------------------------------
-- Test Executions for Part E
-- -------------------------------------------------------

-- Test 1: Top 5 videos across all categories
EXEC dbo.sp_GetTopVideos @TopN = 5;
GO

-- Test 2: Top 3 videos
EXEC dbo.sp_GetTopVideos @TopN = 3, @CategoryId = NULL;
GO

-- Test the scalar function directly
DECLARE @sample_video_id UNIQUEIDENTIFIER;
SELECT TOP 1 @sample_video_id = video_id FROM [dbo].[Video] WHERE is_hidden = 0;

SELECT
    v.title,
    dbo.fn_GetVideoEngagementScore(v.video_id) AS engagement_score
FROM [dbo].[Video] v
WHERE v.video_id = @sample_video_id;
GO


-- ============================================================
-- PART F : TRIGGER  (15%)
-- ============================================================

-- -------------------------------------------------------
-- Trigger 1 : trg_AfterInsertLike_NotifyUploader
-- AFTER INSERT on [Like].
-- When a user likes a video, automatically creates a
-- notification for the video uploader.
-- -------------------------------------------------------
IF OBJECT_ID('dbo.trg_AfterInsertLike_NotifyUploader', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_AfterInsertLike_NotifyUploader;
GO

CREATE TRIGGER dbo.trg_AfterInsertLike_NotifyUploader
ON  [dbo].[Like]
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO [dbo].[notification]
    (
        [id],
        [created_at],
        [is_read],
        [message],
        [related_entity_id],
        [related_entity_type],
        [title],
        [type],
        [user_id]
    )
    SELECT
        NEWID(),
        SYSDATETIME(),
        0,
        u_liker.username + ' liked your video "' + v.title + '".',
        i.video_id,
        'VIDEO',
        'New Like on Your Video',
        'SOCIAL',
        v.uploader_id
    FROM inserted i
    JOIN [dbo].[Video] v       ON v.video_id  = i.video_id
    JOIN [dbo].[User]  u_liker ON u_liker.user_id = i.user_id
    WHERE v.is_hidden   = 0
      AND v.uploader_id <> i.user_id;   -- no self-like notifications
END;
GO

-- -------------------------------------------------------
-- Trigger 2 : trg_AfterDeleteVideo_Cleanup
-- AFTER DELETE on [Video].
-- Cascades deletion of all related records to keep data
-- consistent (likes, dislikes, comments, watch history,
-- playlist entries, view events).
-- -------------------------------------------------------
IF OBJECT_ID('dbo.trg_AfterDeleteVideo_Cleanup', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_AfterDeleteVideo_Cleanup;
GO

CREATE TRIGGER dbo.trg_AfterDeleteVideo_Cleanup
ON  [dbo].[Video]
AFTER DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DELETE FROM [dbo].[Like]
    WHERE video_id IN (SELECT video_id FROM deleted);

    DELETE FROM [dbo].[dislike]
    WHERE video_id IN (SELECT video_id FROM deleted);

    DELETE FROM [dbo].[CommentLike]
    WHERE comment_id IN (
        SELECT comment_id FROM [dbo].[Comment]
        WHERE video_id IN (SELECT video_id FROM deleted)
    );

    DELETE FROM [dbo].[Comment]
    WHERE video_id IN (SELECT video_id FROM deleted);

    DELETE FROM [dbo].[watch_history]
    WHERE video_id IN (SELECT video_id FROM deleted);

    DELETE FROM [dbo].[playlist_video]
    WHERE video_id IN (SELECT video_id FROM deleted);

    DELETE FROM [dbo].[VideoViewEvent]
    WHERE video_id IN (SELECT video_id FROM deleted);

    -- Audit trail in SyncLog
    INSERT INTO [dbo].[SyncLog] ([file_name], [applied_at])
    SELECT
        'VIDEO_DELETED:' + CAST(video_id AS NVARCHAR(36)),
        SYSDATETIME()
    FROM deleted;
END;
GO


-- -------------------------------------------------------
-- Demonstration of Trigger 1 (Like notification)
-- -------------------------------------------------------

DECLARE @test_user_id   UNIQUEIDENTIFIER;
DECLARE @test_video_id  UNIQUEIDENTIFIER;
DECLARE @test_like_id   UNIQUEIDENTIFIER;

SELECT TOP 1
    @test_video_id = v.video_id,
    @test_user_id  = (
        SELECT TOP 1 user_id
        FROM [dbo].[User]
        WHERE user_id <> v.uploader_id AND role = 'USER'
    )
FROM [dbo].[Video] v
WHERE v.is_hidden = 0 AND v.visibility = 'PUBLIC';

SET @test_like_id = NEWID();

IF NOT EXISTS (
    SELECT 1 FROM [dbo].[Like]
    WHERE user_id = @test_user_id AND video_id = @test_video_id
)
BEGIN
    INSERT INTO [dbo].[Like] (like_id, user_id, video_id, created_at, updated_at)
    VALUES (@test_like_id, @test_user_id, @test_video_id, SYSDATETIME(), SYSDATETIME());
    PRINT 'Like inserted. Trigger created a notification.';
END
ELSE
BEGIN
    PRINT 'Like already exists for this user/video — skipping.';
END;
GO

-- Verify notification was created by the trigger
SELECT TOP 5
    n.title,
    n.message,
    n.type,
    n.related_entity_type,
    n.created_at,
    n.is_read
FROM   [dbo].[notification] n
WHERE  n.type = 'SOCIAL'
ORDER  BY n.created_at DESC;
GO
