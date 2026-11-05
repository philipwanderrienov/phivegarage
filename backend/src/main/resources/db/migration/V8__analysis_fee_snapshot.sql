-- Retain exact fee rules used for a run; avoid reading a mutable catalog at worker time.
ALTER TABLE analyses ADD COLUMN fee_snapshot JSONB;
