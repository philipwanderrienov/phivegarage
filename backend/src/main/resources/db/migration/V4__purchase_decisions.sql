CREATE TABLE purchase_decisions (
 id UUID PRIMARY KEY, source_lot_id UUID REFERENCES lots(id) ON DELETE RESTRICT,
 auction_house_id UUID REFERENCES auction_houses(id) ON DELETE RESTRICT,
 data JSONB NOT NULL, fee_snapshot JSONB NOT NULL, results JSONB NOT NULL,
 ai_status TEXT NOT NULL DEFAULT 'NONE' CHECK(ai_status IN ('NONE','QUEUED','PROCESSING','READY','FAILED')),
 ai JSONB, usage JSONB NOT NULL DEFAULT '{}', error TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE decision_photos (
 id UUID PRIMARY KEY, decision_id UUID NOT NULL REFERENCES purchase_decisions(id) ON DELETE CASCADE,
 file_path TEXT NOT NULL, mime_type TEXT NOT NULL
);
ALTER TABLE won_units ALTER COLUMN auction_house_id DROP NOT NULL;
ALTER TABLE won_units ADD COLUMN source_decision_id UUID UNIQUE REFERENCES purchase_decisions(id) ON DELETE RESTRICT;
CREATE INDEX decision_photos_decision_idx ON decision_photos(decision_id);
