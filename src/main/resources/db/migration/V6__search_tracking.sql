-- Our own record of the searches this app has requested for a title, driving the automatic re-search in
-- AutoSearchService: how many we have asked for, and when the first and last of them were. Deliberately separate from
-- radarr_last_search_time / sonarr_last_search_time, which hold Radarr's and Sonarr's own values and are overwritten
-- from them on every refresh -- those say when the *arr last searched by whatever trigger, these say what we asked
-- for, so the first-to-last span stays self-consistent. A span wider than search.stale-after-days marks the request
-- stale. Null search_count means we have never searched for it, not that we searched zero times.
-- sonarr_episode_id is Sonarr's own episode id, already fetched during refresh and previously discarded; persisting
-- it lets an episode search be issued without re-fetching the series' episode list from Sonarr.
ALTER TABLE public.movie_request
    ADD COLUMN search_count integer,
    ADD COLUMN search_first_at timestamp(6) with time zone,
    ADD COLUMN search_last_at timestamp(6) with time zone;

ALTER TABLE public.tv_episode_request
    ADD COLUMN search_count integer,
    ADD COLUMN search_first_at timestamp(6) with time zone,
    ADD COLUMN search_last_at timestamp(6) with time zone,
    ADD COLUMN sonarr_episode_id integer;
