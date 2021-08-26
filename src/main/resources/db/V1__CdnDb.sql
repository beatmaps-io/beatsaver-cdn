CREATE TABLE public.map
(
    "mapId" integer NOT NULL,
    "fileName" text COLLATE pg_catalog."default" NOT NULL,
    "songName" text COLLATE pg_catalog."default" NOT NULL,
    "levelAuthorName" text COLLATE pg_catalog."default" NOT NULL,
    deleted boolean NOT NULL,
    CONSTRAINT map_pkey PRIMARY KEY ("mapId")
)

CREATE TABLE public.version
(
    hash character(40) COLLATE pg_catalog."default" NOT NULL,
    "mapId" integer NOT NULL,
    published boolean NOT NULL,
    CONSTRAINT version_pkey PRIMARY KEY (hash),
    CONSTRAINT map FOREIGN KEY ("mapId")
        REFERENCES public.map ("mapId") MATCH SIMPLE
        ON UPDATE CASCADE
        ON DELETE CASCADE
)

ALTER TABLE public.map OWNER to "beatmaps";
ALTER TABLE public.version OWNER to "beatmaps";

CREATE UNIQUE INDEX published_ver ON public.version USING btree ("mapId") WHERE "published";
