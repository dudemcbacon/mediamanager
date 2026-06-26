-- Flyway baseline (V1): the full application schema, captured from the live database via
-- `pg_dump --schema-only`. On an existing database this migration is NOT executed --
-- spring.flyway.baseline-on-migrate stamps the schema at version 1 -- so it runs only to build
-- the schema on a fresh/empty database. JobRunr's own tables (jobrunr_*) are intentionally
-- excluded: JobRunr creates and migrates those itself at runtime. Future schema changes go in
-- new V2+/V{n} migrations alongside this file.


CREATE TABLE public.app_user (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    enabled boolean NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    updated_at timestamp(6) with time zone,
    username character varying(255) NOT NULL
);

CREATE SEQUENCE public.app_user_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.ffprobe_scans (
    id bigint NOT NULL,
    bit_rate bigint,
    created_at timestamp(6) with time zone,
    duration double precision,
    filename text,
    format_long_name character varying(255),
    format_name character varying(255),
    nb_programs integer,
    nb_streams integer,
    probe_score integer,
    request_id bigint,
    request_type character varying(255),
    size bigint,
    start_time double precision,
    updated_at timestamp(6) with time zone
);

CREATE SEQUENCE public.ffprobe_scans_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.ffprobe_streams (
    id bigint NOT NULL,
    avg_frame_rate character varying(255),
    bit_rate bigint,
    channel_layout character varying(255),
    channels integer,
    codec_long_name character varying(255),
    codec_name character varying(255),
    codec_type character varying(255),
    created_at timestamp(6) with time zone,
    duration double precision,
    height integer,
    nb_frames bigint,
    pix_fmt character varying(255),
    r_frame_rate character varying(255),
    sample_rate integer,
    stream_index integer,
    updated_at timestamp(6) with time zone,
    width integer,
    ffprobe_scan_id bigint NOT NULL
);

CREATE SEQUENCE public.ffprobe_streams_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.movie_request (
    plex_tmdbid integer,
    radarr_has_file boolean,
    radarr_is_available boolean,
    radarr_last_search_time timestamp(6) with time zone,
    radarr_monitored boolean,
    radarr_movie_file_path text,
    radarr_original_language character varying(255),
    radarr_path text,
    radarr_request_id integer,
    radarr_root_folder_path text,
    tmdbid integer,
    id bigint NOT NULL,
    radarr_quality_profile character varying(255),
    local_file_path_available boolean,
    local_file_size bigint
);

CREATE TABLE public.note (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    notes text NOT NULL,
    updated_at timestamp(6) with time zone,
    request_id bigint NOT NULL
);

CREATE SEQUENCE public.note_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.request (
    request_type character varying(31) NOT NULL,
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    marked_stale_at timestamp(6) with time zone,
    ombi_available boolean,
    ombi_request_id integer,
    ombi_request_status character varying(255),
    ombi_user_name character varying(255),
    plex_added_at bigint,
    plex_media_duration bigint,
    plex_media_filename text,
    plex_media_id integer,
    plex_media_size bigint,
    plex_metadata_id character varying(255),
    plex_metadata_url text,
    plex_updated_at bigint,
    stale boolean,
    stale_reason text,
    title character varying(255),
    updated_at timestamp(6) with time zone,
    ombi_requested_date timestamp(6) with time zone,
    CONSTRAINT request_request_type_check CHECK (((request_type)::text = ANY ((ARRAY['MOVIE'::character varying, 'TV'::character varying])::text[])))
);

CREATE SEQUENCE public.request_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.tv_child_request (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    ombi_available boolean,
    ombi_parent_request_id integer,
    ombi_request_id integer,
    ombi_request_status character varying(255),
    ombi_total_seasons integer,
    ombi_user_name character varying(255),
    title character varying(255),
    tvdb_id integer,
    updated_at timestamp(6) with time zone,
    parent_id bigint NOT NULL
);

CREATE SEQUENCE public.tv_child_request_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.tv_episode_request (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    ombi_approved boolean,
    ombi_available boolean,
    ombi_episode_id integer,
    ombi_episode_number integer,
    ombi_request_status character varying(255),
    ombi_requested boolean,
    ombi_title character varying(255),
    plex_path text,
    sonarr_last_search_time timestamp(6) with time zone,
    sonarr_path text,
    updated_at timestamp(6) with time zone,
    tv_season_request_id bigint NOT NULL,
    local_file_path_available boolean,
    local_file_size bigint,
    plex_media_size bigint
);

CREATE SEQUENCE public.tv_episode_request_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.tv_request (
    ombi_external_provider_id integer,
    ombi_total_seasons integer,
    plex_tvdb_id integer,
    sonarr_episode_count integer,
    sonarr_episode_file_count integer,
    sonarr_last_searched timestamp(6) with time zone,
    sonarr_monitored boolean,
    sonarr_monitored_all character varying(255),
    sonarr_original_language character varying(255),
    sonarr_path text,
    sonarr_root_folder_path text,
    sonarr_series_id integer,
    sonarr_title_slug character varying(255),
    sonarr_total_episode_count integer,
    tvdb_id integer,
    id bigint NOT NULL,
    sonarr_quality_profile character varying(255)
);

CREATE TABLE public.tv_season_request (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    ombi_season_available boolean,
    ombi_season_number integer,
    ombi_season_request_id integer,
    updated_at timestamp(6) with time zone,
    tv_child_request_id bigint NOT NULL
);

CREATE SEQUENCE public.tv_season_request_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.validation (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    result boolean,
    updated_at timestamp(6) with time zone,
    validation_name character varying(255),
    request_id bigint,
    tv_episode_id bigint,
    CONSTRAINT chk_validation_request_xor_episode CHECK (((request_id IS NULL) <> (tv_episode_id IS NULL)))
);

CREATE SEQUENCE public.validation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ffprobe_scans
    ADD CONSTRAINT ffprobe_scans_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ffprobe_streams
    ADD CONSTRAINT ffprobe_streams_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.movie_request
    ADD CONSTRAINT movie_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.note
    ADD CONSTRAINT note_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.request
    ADD CONSTRAINT request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tv_child_request
    ADD CONSTRAINT tv_child_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tv_episode_request
    ADD CONSTRAINT tv_episode_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tv_request
    ADD CONSTRAINT tv_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tv_season_request
    ADD CONSTRAINT tv_season_request_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT uk3k4cplvh82srueuttfkwnylq0 UNIQUE (username);

ALTER TABLE ONLY public.request
    ADD CONSTRAINT uk_request_ombi_id_type UNIQUE (ombi_request_id, request_type);

ALTER TABLE ONLY public.tv_child_request
    ADD CONSTRAINT uk_tv_child_request_ombi_id UNIQUE (ombi_request_id);

ALTER TABLE ONLY public.tv_episode_request
    ADD CONSTRAINT uk_tv_episode_request_season_episode UNIQUE (tv_season_request_id, ombi_episode_number);

ALTER TABLE ONLY public.tv_season_request
    ADD CONSTRAINT uk_tv_season_request_child_season UNIQUE (tv_child_request_id, ombi_season_number);

ALTER TABLE ONLY public.validation
    ADD CONSTRAINT uk_validation_name_request UNIQUE (validation_name, request_id);

ALTER TABLE ONLY public.validation
    ADD CONSTRAINT uk_validation_name_tv_episode UNIQUE (validation_name, tv_episode_id);

ALTER TABLE ONLY public.movie_request
    ADD CONSTRAINT ukciovc9y6l6302u64mrgne9f1b UNIQUE (radarr_request_id);

ALTER TABLE ONLY public.validation
    ADD CONSTRAINT validation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.ffprobe_streams
    ADD CONSTRAINT fk1sc94riu50y06w6rsbibdqdwj FOREIGN KEY (ffprobe_scan_id) REFERENCES public.ffprobe_scans(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.movie_request
    ADD CONSTRAINT fkcpmdbfle567ife6elhds2f36p FOREIGN KEY (id) REFERENCES public.request(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tv_child_request
    ADD CONSTRAINT fkdy9stmt81y9xb48q29r2e2ggr FOREIGN KEY (parent_id) REFERENCES public.tv_request(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.validation
    ADD CONSTRAINT fkepdxj4u0qg9feyhahrjb3q7x6 FOREIGN KEY (tv_episode_id) REFERENCES public.tv_episode_request(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.note
    ADD CONSTRAINT fkg9ahi0av7t5n2qqeordutlnaf FOREIGN KEY (request_id) REFERENCES public.request(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tv_season_request
    ADD CONSTRAINT fkh2b4232rwt99eqc6mxrffh447 FOREIGN KEY (tv_child_request_id) REFERENCES public.tv_child_request(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tv_request
    ADD CONSTRAINT fki52huvr457au9jguf9hvjbegl FOREIGN KEY (id) REFERENCES public.request(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tv_episode_request
    ADD CONSTRAINT fkktwi5r40fphrpnun8pliu9q9g FOREIGN KEY (tv_season_request_id) REFERENCES public.tv_season_request(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.validation
    ADD CONSTRAINT fktdtc5m9mrcx6dps04nbjj3aeu FOREIGN KEY (request_id) REFERENCES public.request(id) ON DELETE CASCADE;

