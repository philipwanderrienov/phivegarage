-- PostgreSQL 15+; monetary values are integer IDR, never floating point.
CREATE TABLE catalogs (
 id UUID PRIMARY KEY, name TEXT NOT NULL, house TEXT NOT NULL, file_path TEXT NOT NULL,
 status TEXT NOT NULL CHECK (status IN ('QUEUED','EXTRACTING','READY','FAILED')),
 progress INT NOT NULL DEFAULT 0, pages INT NOT NULL DEFAULT 0, error TEXT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE lots (
 id UUID PRIMARY KEY, catalog_id UUID NOT NULL REFERENCES catalogs(id) ON DELETE CASCADE,
 lot_number TEXT NOT NULL, data JSONB NOT NULL, verified BOOLEAN NOT NULL DEFAULT false,
 UNIQUE(catalog_id,lot_number)
);
CREATE TABLE analyses (
 id UUID PRIMARY KEY, catalog_id UUID NOT NULL REFERENCES catalogs(id) ON DELETE CASCADE,
 status TEXT NOT NULL CHECK (status IN ('QUEUED','PROCESSING','READY','FAILED')),
 parameters JSONB NOT NULL, input_lots JSONB NOT NULL, results JSONB NOT NULL DEFAULT '[]', usage JSONB NOT NULL DEFAULT '{}',
 error TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX lots_catalog_idx ON lots(catalog_id);
CREATE INDEX analyses_catalog_idx ON analyses(catalog_id,created_at DESC);
