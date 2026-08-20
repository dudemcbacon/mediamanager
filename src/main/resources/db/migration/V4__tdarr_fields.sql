-- Tdarr's view of each available movie / TV episode, recorded during refresh from
-- POST /api/v2/client/search (see TdarrClient): the health-check and transcode verdicts, plus the sizes Tdarr
-- reports either side of a transcode. NOTE the size columns are GiB as fractional doubles — that is Tdarr's own
-- unit — unlike the byte-valued local_file_size / plex_media_size columns beside them, hence the _gb suffix.
-- All four are nullable: a title Tdarr has never seen, or an unreachable Tdarr, simply leaves them unset.
ALTER TABLE public.movie_request
    ADD COLUMN tdarr_health_check character varying(255),
    ADD COLUMN tdarr_transcode_decision_maker character varying(255),
    ADD COLUMN tdarr_old_size_gb double precision,
    ADD COLUMN tdarr_new_size_gb double precision;

ALTER TABLE public.tv_episode_request
    ADD COLUMN tdarr_health_check character varying(255),
    ADD COLUMN tdarr_transcode_decision_maker character varying(255),
    ADD COLUMN tdarr_old_size_gb double precision,
    ADD COLUMN tdarr_new_size_gb double precision;
