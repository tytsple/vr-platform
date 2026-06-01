-- Prevent duplicate active sessions for the same venue+app pair
CREATE UNIQUE INDEX IF NOT EXISTS idx_sessions_active_unique
    ON sessions(venue_id, application_id) WHERE status = 'active';
