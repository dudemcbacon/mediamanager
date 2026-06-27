-- Tracks how long each torrent has been continuously seedless, feeding the "drought" term of download stuckness.
-- A row exists only while a torrent currently has no seeds (created when its seed count first hits zero, removed
-- when seeds return or the torrent disappears), so the table stays small. Keyed by the torrent info hash, which is
-- assigned rather than generated, so no sequence is needed.
CREATE TABLE public.seedless_torrent (
    hash character varying(255) NOT NULL,
    seedless_since timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone
);

ALTER TABLE ONLY public.seedless_torrent
    ADD CONSTRAINT seedless_torrent_pkey PRIMARY KEY (hash);
