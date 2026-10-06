-- F6: add history_cleared_at to UserSettings for recommendation cycle restart
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('UserSettings') AND name = 'history_cleared_at'
)
BEGIN
    ALTER TABLE UserSettings ADD history_cleared_at DATETIME2 NULL;
END
GO
