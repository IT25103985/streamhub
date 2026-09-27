-- SEED SCRIPT FOR NOTIFICATIONS (MANUAL TESTING)
-- Run this manually against StreamHubDB to test the Notification system
-- since no real triggers exist yet in Phase A.

-- Replace the @TestUserId value with an actual user_id from your local database
DECLARE @TestUserId UNIQUEIDENTIFIER = (SELECT TOP 1 user_id FROM [User]);

IF @TestUserId IS NOT NULL
BEGIN
    INSERT INTO Notification (id, user_id, type, title, message, related_entity_type, related_entity_id, is_read, created_at)
    VALUES 
        (NEWID(), @TestUserId, 'SYSTEM_ALERT', 'Welcome to StreamHub', 'Thanks for joining. Please review our community guidelines.', NULL, NULL, 0, DATEADD(hour, -2, SYSUTCDATETIME())),
        
        (NEWID(), @TestUserId, 'REPORT_ACTION', 'Report Update', 'We have reviewed your report and taken appropriate action.', 'VIDEO', (SELECT TOP 1 video_id FROM Video), 0, DATEADD(minute, -30, SYSUTCDATETIME())),
        
        (NEWID(), @TestUserId, 'COMMENT_REPLY', 'New Reply', 'Someone replied to your comment on a video.', 'PROFILE', @TestUserId, 1, DATEADD(day, -1, SYSUTCDATETIME())),
        
        (NEWID(), @TestUserId, 'FOLLOW', 'New Follower', 'Someone started following your channel.', 'PROFILE', @TestUserId, 0, DATEADD(minute, -10, SYSUTCDATETIME()));
        
    PRINT 'Successfully inserted 4 test notifications for user: ' + CAST(@TestUserId AS NVARCHAR(50));
END
ELSE
BEGIN
    PRINT 'No user found in the database to assign test notifications to.';
END
GO

