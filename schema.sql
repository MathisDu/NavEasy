-- Schema SQL pour Supabase
-- Tables correspondant aux modèles de données de core/

CREATE TYPE report_type AS ENUM (
    'STAIRS',
    'STEPS',
    'CURB',
    'CONSTRUCTION',
    'NARROW_PASSAGE',
    'FLOORS',
    'UNPAVED_SURFACE',
    'POOR_LIGHTING',
    'RAMP',
    'OTHER'
);

CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type report_type NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index pour les requêtes géographiques (recherche par zone)
CREATE INDEX idx_reports_location ON reports USING GIST (
    ll_to_earth(lat, lon)
);
