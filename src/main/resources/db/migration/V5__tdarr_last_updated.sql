-- Freshness stamp for the tdarr_* columns added in V4: when Tdarr data was last successfully applied to the row,
-- whether by a refresh reading it from Tdarr or by the /api/tdarr/transcode-complete webhook pushing it in.
-- Deliberately separate from updated_at, which moves on any change to the row rather than only on Tdarr activity.
-- Stays null until Tdarr has actually reported on the file, so null means "never heard from Tdarr", not "stale".
ALTER TABLE public.movie_request
    ADD COLUMN tdarr_last_updated timestamp(6) with time zone;

ALTER TABLE public.tv_episode_request
    ADD COLUMN tdarr_last_updated timestamp(6) with time zone;
