-- Schema SQL pour Supabase
-- Tables correspondant aux modèles de données de core/

CREATE TABLE public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID REFERENCES auth.users(id),

    title TEXT NOT NULL,
    description TEXT,

    category TEXT NOT NULL,

    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,

    disability_types TEXT[] NOT NULL,

    severity TEXT NOT NULL DEFAULT 'medium'
        CHECK (severity IN ('low', 'medium', 'high')),

    status TEXT NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'resolved')),

    resolved_votes INT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Threshold: when resolved_votes reaches this number, the report is marked as resolved
-- Current threshold: 5 votes

-- Index pour les requêtes géographiques (recherche par zone)
CREATE INDEX idx_reports_location ON reports USING GIST (
    ll_to_earth(latitude, longitude)
);

-- Grant table privileges to the anon role (required for Supabase REST API access)
-- DELETE is intentionally excluded — reports are resolved via voting (PATCH), not direct deletion
GRANT SELECT, INSERT ON public.reports TO anon;
GRANT SELECT, INSERT ON public.reports TO authenticated;
GRANT SELECT ON public.reports TO service_role;

-- Enable Row Level Security (RLS)
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;

-- RLS policies for anon role (public access)
CREATE POLICY "Allow public read access"
    ON public.reports FOR SELECT
    TO anon
    USING (true);

CREATE POLICY "Allow public insert"
    ON public.reports FOR INSERT
    TO anon
    WITH CHECK (true);

-- DELETE policy is intentionally not created — reports are resolved via voting (PATCH), not direct deletion

-- RLS policies for authenticated role
CREATE POLICY "Allow authenticated read access"
    ON public.reports FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Allow authenticated insert"
    ON public.reports FOR INSERT
    TO authenticated
    WITH CHECK (true);

-- DELETE policy is intentionally not created — reports are resolved via voting (PATCH), not direct deletion
