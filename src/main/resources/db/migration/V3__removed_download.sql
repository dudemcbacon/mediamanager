-- Records the stuck downloads the most recent notification run auto-removed, so both the summary email and the
-- Notifications page can show what was cleaned up since the last run. The table is replaced wholesale each run (it
-- only ever holds the latest run's removals), keyed by the torrent info hash, which is assigned rather than generated,
-- so no sequence is needed.
CREATE TABLE public.removed_download (
    hash character varying(255) NOT NULL,
    name text,
    progress double precision NOT NULL,
    stuckness double precision NOT NULL,
    linked_request text,
    removed_at timestamp(6) with time zone NOT NULL
);

ALTER TABLE ONLY public.removed_download
    ADD CONSTRAINT removed_download_pkey PRIMARY KEY (hash);
