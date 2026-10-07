CREATE TABLE won_units (
 id UUID PRIMARY KEY, source_lot_id UUID UNIQUE REFERENCES lots(id) ON DELETE RESTRICT,
 auction_house_id UUID NOT NULL REFERENCES auction_houses(id) ON DELETE RESTRICT,
 data JSONB NOT NULL, fee_snapshot JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE unit_expenses (
 id UUID PRIMARY KEY, unit_id UUID NOT NULL REFERENCES won_units(id) ON DELETE CASCADE,
 expense_date DATE NOT NULL, category VARCHAR(60) NOT NULL, description TEXT NOT NULL,
 amount BIGINT NOT NULL CHECK(amount>0 AND amount<=100000000000)
);
CREATE INDEX unit_expenses_unit_idx ON unit_expenses(unit_id,expense_date);
CREATE TABLE garage_settings (id SMALLINT PRIMARY KEY CHECK(id=1), starting_funds BIGINT NOT NULL DEFAULT 0 CHECK(starting_funds>=0 AND starting_funds<=100000000000));
INSERT INTO garage_settings(id,starting_funds) VALUES(1,0);
