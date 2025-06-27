--
-- PostgreSQL database dump
--

-- Dumped from database version 14.17 (Homebrew)
-- Dumped by pg_dump version 14.17 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: search_abbreviations(text, text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.search_abbreviations(search_term text DEFAULT ''::text, lang_code text DEFAULT ''::text, dom_code text DEFAULT ''::text) RETURNS TABLE(id integer, name character varying, description text, language_name character varying, language_code character varying, domain_name character varying, domain_code character varying, author_username character varying, meanings text[], views integer, likes integer, favorites integer, created_at timestamp without time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT
    a.id,
    a.name,
    a.description,
    l.name as language_name,
    l.code as language_code,
    d.name as domain_name,
    d.code as domain_code,
    u.username as author_username,
    ARRAY_AGG(am.meaning ORDER BY am.sort_order) as meanings,
    a.views,
    a.likes,
    a.favorites,
    a.created_at
FROM abbreviations a
         INNER JOIN languages l ON a.language_id = l.id
         INNER JOIN domains d ON a.domain_id = d.id
         INNER JOIN users u ON a.user_id = u.id
         LEFT JOIN abbreviation_meanings am ON a.id = am.abbreviation_id
WHERE
    (search_term = '' OR
        -- Căutare în numele abrevierii cu suport pentru diacritice
     a.name ILIKE '%' || search_term || '%' OR
         -- Căutare în descriere cu suport pentru diacritice
         a.description ILIKE '%' || search_term || '%' OR
         -- Căutare în semnificații cu suport pentru diacritice
         EXISTS (
             SELECT 1 FROM abbreviation_meanings am2
             WHERE am2.abbreviation_id = a.id
             AND am2.meaning ILIKE '%' || search_term || '%'
         ))
  -- Filtrare după codul limbii
  AND (lang_code = '' OR l.code = lang_code)
  -- Filtrare după codul domeniului
  AND (dom_code = '' OR d.code = dom_code)
GROUP BY a.id, l.name, l.code, d.name, d.code, u.username
-- Sortare alfabetică păstrând diacriticele
ORDER BY a.name COLLATE "ro_RO.UTF-8", l.name, d.name;
END;
$$;


ALTER FUNCTION public.search_abbreviations(search_term text, lang_code text, dom_code text) OWNER TO postgres;

--
-- Name: search_abbreviations_fuzzy(text, text, text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.search_abbreviations_fuzzy(search_term text DEFAULT ''::text, lang_code text DEFAULT ''::text, dom_code text DEFAULT ''::text) RETURNS TABLE(id integer, name character varying, description text, language_name character varying, language_code character varying, domain_name character varying, domain_code character varying, author_username character varying, meanings text[], views integer, likes integer, favorites integer, created_at timestamp without time zone)
    LANGUAGE plpgsql
    AS $$
BEGIN
RETURN QUERY
SELECT
    a.id,
    a.name,
    a.description,
    l.name as language_name,
    l.code as language_code,
    d.name as domain_name,
    d.code as domain_code,
    u.username as author_username,
    ARRAY_AGG(am.meaning ORDER BY am.sort_order) as meanings,
    a.views,
    a.likes,
    a.favorites,
    a.created_at
FROM abbreviations a
         INNER JOIN languages l ON a.language_id = l.id
         INNER JOIN domains d ON a.domain_id = d.id
         INNER JOIN users u ON a.user_id = u.id
         LEFT JOIN abbreviation_meanings am ON a.id = am.abbreviation_id
WHERE
    (search_term = '' OR
        -- Căutare fuzzy folosind unaccent pentru a ignora diacriticele
     unaccent(UPPER(a.name)) LIKE '%' || unaccent(UPPER(search_term)) || '%' OR
     unaccent(UPPER(a.description)) LIKE '%' || unaccent(UPPER(search_term)) || '%' OR
     EXISTS (
         SELECT 1 FROM abbreviation_meanings am2
         WHERE am2.abbreviation_id = a.id
           AND unaccent(UPPER(am2.meaning)) LIKE '%' || unaccent(UPPER(search_term)) || '%'
     ))
  AND (lang_code = '' OR l.code = lang_code)
  AND (dom_code = '' OR d.code = dom_code)
GROUP BY a.id, l.name, l.code, d.name, d.code, u.username
ORDER BY a.name, l.name, d.name;
END;
$$;


ALTER FUNCTION public.search_abbreviations_fuzzy(search_term text, lang_code text, dom_code text) OWNER TO postgres;

--
-- Name: update_favorites_count(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_favorites_count() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
UPDATE abbreviations
SET favorites = favorites + 1
WHERE id = NEW.abbreviation_id;
RETURN NEW;
ELSIF TG_OP = 'DELETE' THEN
UPDATE abbreviations
SET favorites = favorites - 1
WHERE id = OLD.abbreviation_id;
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION public.update_favorites_count() OWNER TO postgres;

--
-- Name: update_likes_count(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_likes_count() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
UPDATE abbreviations
SET likes = likes + 1
WHERE id = NEW.abbreviation_id;
RETURN NEW;
ELSIF TG_OP = 'DELETE' THEN
UPDATE abbreviations
SET likes = likes - 1
WHERE id = OLD.abbreviation_id;
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;


ALTER FUNCTION public.update_likes_count() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: abbreviation_favorites; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.abbreviation_favorites (
                                               id integer NOT NULL,
                                               abbreviation_id integer NOT NULL,
                                               user_id integer NOT NULL,
                                               created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.abbreviation_favorites OWNER TO postgres;

--
-- Name: abbreviation_favorites_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.abbreviation_favorites_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.abbreviation_favorites_id_seq OWNER TO postgres;

--
-- Name: abbreviation_favorites_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.abbreviation_favorites_id_seq OWNED BY public.abbreviation_favorites.id;


--
-- Name: abbreviation_likes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.abbreviation_likes (
                                           id integer NOT NULL,
                                           abbreviation_id integer NOT NULL,
                                           user_id integer NOT NULL,
                                           created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.abbreviation_likes OWNER TO postgres;

--
-- Name: abbreviation_likes_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.abbreviation_likes_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.abbreviation_likes_id_seq OWNER TO postgres;

--
-- Name: abbreviation_likes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.abbreviation_likes_id_seq OWNED BY public.abbreviation_likes.id;


--
-- Name: abbreviation_meanings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.abbreviation_meanings (
                                              id integer NOT NULL,
                                              abbreviation_id integer NOT NULL,
                                              meaning text NOT NULL,
                                              sort_order integer DEFAULT 0
);


ALTER TABLE public.abbreviation_meanings OWNER TO postgres;

--
-- Name: abbreviation_meanings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.abbreviation_meanings_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.abbreviation_meanings_id_seq OWNER TO postgres;

--
-- Name: abbreviation_meanings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.abbreviation_meanings_id_seq OWNED BY public.abbreviation_meanings.id;


--
-- Name: abbreviations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.abbreviations (
                                      id integer NOT NULL,
                                      name character varying(50) NOT NULL,
                                      language_id integer NOT NULL,
                                      domain_id integer NOT NULL,
                                      user_id integer NOT NULL,
                                      docbook text,
                                      description text,
                                      views integer DEFAULT 0,
                                      likes integer DEFAULT 0,
                                      favorites integer DEFAULT 0,
                                      created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.abbreviations OWNER TO postgres;

--
-- Name: abbreviations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.abbreviations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.abbreviations_id_seq OWNER TO postgres;

--
-- Name: abbreviations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.abbreviations_id_seq OWNED BY public.abbreviations.id;


--
-- Name: domains; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.domains (
                                id integer NOT NULL,
                                code character varying(20) NOT NULL,
                                name character varying(100) NOT NULL
);


ALTER TABLE public.domains OWNER TO postgres;

--
-- Name: domains_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.domains_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.domains_id_seq OWNER TO postgres;

--
-- Name: domains_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.domains_id_seq OWNED BY public.domains.id;


--
-- Name: languages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.languages (
                                  id integer NOT NULL,
                                  code character varying(5) NOT NULL,
                                  name character varying(100) NOT NULL
);


ALTER TABLE public.languages OWNER TO postgres;

--
-- Name: languages_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.languages_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.languages_id_seq OWNER TO postgres;

--
-- Name: languages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.languages_id_seq OWNED BY public.languages.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
                              id integer NOT NULL,
                              username character varying(50) NOT NULL,
                              email character varying(255) NOT NULL,
                              password character varying(255) NOT NULL,
                              role character varying(20) DEFAULT 'user'::character varying,
                              created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['admin'::character varying, 'user'::character varying, 'guest'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: abbreviation_favorites id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_favorites ALTER COLUMN id SET DEFAULT nextval('public.abbreviation_favorites_id_seq'::regclass);


--
-- Name: abbreviation_likes id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_likes ALTER COLUMN id SET DEFAULT nextval('public.abbreviation_likes_id_seq'::regclass);


--
-- Name: abbreviation_meanings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_meanings ALTER COLUMN id SET DEFAULT nextval('public.abbreviation_meanings_id_seq'::regclass);


--
-- Name: abbreviations id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviations ALTER COLUMN id SET DEFAULT nextval('public.abbreviations_id_seq'::regclass);


--
-- Name: domains id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.domains ALTER COLUMN id SET DEFAULT nextval('public.domains_id_seq'::regclass);


--
-- Name: languages id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.languages ALTER COLUMN id SET DEFAULT nextval('public.languages_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: abbreviation_favorites; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.abbreviation_favorites (id, abbreviation_id, user_id, created_at) FROM stdin;
2	8	4	2025-06-25 14:49:50.636688
10	5	4	2025-06-25 20:13:13.464817
13	32	4	2025-06-25 23:29:42.386952
14	38	1	2025-06-27 10:24:15.646223
15	50	2	2025-06-27 10:24:15.646223
16	57	1	2025-06-27 10:24:15.646223
17	55	3	2025-06-27 10:24:15.646223
\.


--
-- Data for Name: abbreviation_likes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.abbreviation_likes (id, abbreviation_id, user_id, created_at) FROM stdin;
2	8	4	2025-06-25 14:49:50.191614
10	5	4	2025-06-25 20:13:12.437947
13	32	4	2025-06-25 23:29:42.976533
14	38	1	2025-06-27 10:24:15.646223
15	38	2	2025-06-27 10:24:15.646223
16	50	1	2025-06-27 10:24:15.646223
17	57	2	2025-06-27 10:24:15.646223
18	55	3	2025-06-27 10:24:15.646223
\.


--
-- Data for Name: abbreviation_meanings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.abbreviation_meanings (id, abbreviation_id, meaning, sort_order) FROM stdin;
63	33	Bundesinstitut für Arzneimittel und Medizinprodukte	0
64	10	haide si tu acasa	0
66	32	Centre National de la Recherche Scientifique	0
67	34	Cascading Style Sheets	0
68	38	Artificial Intelligence	0
69	41	Rezonanță Magnetică Nucleară	0
70	50	Chief Executive Officer	0
71	53	Acid Dezoxiribonucleic	0
13	8	LOL	0
26	18	American Pharmaceutical Institute	0
54	5	sălata	0
58	30	Direcția de Sănătate Publică	0
61	31	Agence Nationale de Sécurité du Médicament et des Produits de Santé	0
\.


--
-- Data for Name: abbreviations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.abbreviations (id, name, language_id, domain_id, user_id, docbook, description, views, likes, favorites, created_at) FROM stdin;
8	LOL	1	1	4	<article xmlns="http://docbook.org/ns/docbook">\n    <title>LOL</title>\n    <para>haha</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>LOL</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	haha	6	1	1	2025-06-25 14:49:41.789735
31	ANSM	3	2	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>ANSM</title>\n    <para>Agence Nationale de Sécurité du Médicament et des Produits de Santé - autorité française de régulation des médicaments</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Agence Nationale de Sécurité du Médicament et des Produits de Santé</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Agence Nationale de Sécurité du Médicament et des Produits de Santé - autorité française de régulation des médicaments	92	15	8	2025-06-25 21:25:23.724615
5	OMSS	1	2	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>OMSS</title>\n    <para>sănătș</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>sălata</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	sănătș	82	8	3	2025-06-24 22:50:08.639039
32	CNRS	3	4	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>CNRS</title>\n    <para>Centre National de la Recherche Scientifique - plus grand organisme de recherche publique en France</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Centre National de la Recherche Scientifique</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Centre National de la Recherche Scientifique - plus grand organisme de recherche publique en France	157	23	12	2025-06-25 21:26:14.933799
33	BfArM	4	2	2	<article xmlns="http://docbook.org/ns/docbook">\n        <title>BfArM</title>\n        <para>Bundesinstitut für Arzneimittel und Medizinprodukte in Deutschland.</para>\n    </article>	Bundesinstitut für Arzneimittel und Medizinprodukte - deutsche Zulassungsbehörde für Arzneimittel	56	9	4	2025-06-25 21:26:50.354646
30	DSP	1	2	1	<article xmlns="http://docbook.org/ns/docbook">\n        <title>DSP</title>\n        <para>Direcția de Sănătate Publică din România.</para>\n    </article>	Direcția de Sănătate Publică - instituție responsabilă cu monitorizarea sănătății populației din România	67	12	5	2025-06-25 21:24:31.909325
10	HTML	1	2	5	<article xmlns="http://docbook.org/ns/docbook">\n    <title>HTML</title>\n    <para>acasa</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>haide si tu acasa</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	acasa	12	0	0	2025-06-25 18:52:54.639081
18	API	2	2	1	<article xmlns="http://docbook.org/ns/docbook">\n        <title>API - Medical</title>\n        <para>American Pharmaceutical Institute în contextul medical.</para>\n    </article>	În domeniul medical: American Pharmaceutical Institute	26	3	1	2025-06-25 19:38:35.892328
34	CSS	1	1	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>CSS</title>\n    <para>Cascading Style Sheets - limbaj pentru stilizarea paginilor web</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Cascading Style Sheets</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Limbaj pentru stilizarea paginilor web	45	8	3	2025-06-27 10:00:42.772425
35	JS	1	1	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>JS</title>\n    <para>JavaScript - limbaj de programare pentru web</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>JavaScript</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Limbaj de programare pentru web	67	12	5	2025-06-27 10:00:42.772425
36	URL	1	1	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>URL</title>\n    <para>Uniform Resource Locator - adresa unei resurse web</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Uniform Resource Locator</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Adresa unei resurse pe internet	89	15	7	2025-06-27 10:00:42.772425
39	ML	2	1	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>ML</title>\n    <para>Machine Learning - subset of AI that enables systems to learn automatically</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Machine Learning</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Subset of AI that enables systems to learn automatically	156	28	14	2025-06-27 10:24:15.646223
40	API	2	1	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>API</title>\n    <para>Application Programming Interface - set of protocols for building software</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Application Programming Interface</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Set of protocols and tools for building software applications	178	32	16	2025-06-27 10:24:15.646223
41	RMN	1	2	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>RMN</title>\n    <para>Rezonanță Magnetică Nucleară - investigație medicală imagistică</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Rezonanță Magnetică Nucleară</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Investigație medicală imagistică folosind câmpuri magnetice	87	16	8	2025-06-27 10:24:15.646223
42	EKG	1	2	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>EKG</title>\n    <para>Electrocardiogramă - examinare a activității electrice a inimii</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Electrocardiogramă</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Examinare a activității electrice a inimii	123	21	11	2025-06-27 10:24:15.646223
43	ATI	1	2	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>ATI</title>\n    <para>Anestezie și Terapie Intensivă - secție medicală specializată</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Anestezie și Terapie Intensivă</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Secție medicală pentru pacienți în stare critică	98	18	9	2025-06-27 10:24:15.646223
44	MRI	2	2	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>MRI</title>\n    <para>Magnetic Resonance Imaging - medical imaging technique</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Magnetic Resonance Imaging</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Medical imaging technique using magnetic fields and radio waves	145	27	13	2025-06-27 10:24:15.646223
45	ICU	2	2	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>ICU</title>\n    <para>Intensive Care Unit - specialized hospital department</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Intensive Care Unit</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Specialized hospital department for critically ill patients	167	31	15	2025-06-27 10:24:15.646223
46	WHO	2	2	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>WHO</title>\n    <para>World Health Organization - specialized agency of the United Nations</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>World Health Organization</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Specialized agency of the United Nations for international public health	289	54	28	2025-06-27 10:24:15.646223
47	SA	1	3	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>SA</title>\n    <para>Societate pe Acțiuni - formă juridică de organizare a companiilor</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Societate pe Acțiuni</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Formă juridică de organizare a companiilor cu capital împărțit în acțiuni	76	14	6	2025-06-27 10:24:15.646223
48	SRL	1	3	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>SRL</title>\n    <para>Societate cu Răspundere Limitată - formă juridică pentru companii mici și mijlocii</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Societate cu Răspundere Limitată</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Formă juridică pentru companii mici și mijlocii	134	25	12	2025-06-27 10:24:15.646223
49	TVA	1	3	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>TVA</title>\n    <para>Taxa pe Valoarea Adăugată - impozit indirect pe consum</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Taxa pe Valoarea Adăugată</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Impozit indirect pe consum aplicat la vânzarea de bunuri și servicii	201	38	19	2025-06-27 10:24:15.646223
51	CFO	2	3	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>CFO</title>\n    <para>Chief Financial Officer - executive responsible for financial operations</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Chief Financial Officer</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Executive responsible for managing financial operations and strategy	187	34	17	2025-06-27 10:24:15.646223
52	B2B	2	3	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>B2B</title>\n    <para>Business to Business - commerce transactions between businesses</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Business to Business</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Commerce transactions between businesses rather than individual consumers	156	29	14	2025-06-27 10:24:15.646223
53	ADN	1	4	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>ADN</title>\n    <para>Acid Dezoxiribonucleic - molecula care poartă informația genetică</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Acid Dezoxiribonucleic</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Molecula care poartă informația genetică în toate organismele vii	167	31	15	2025-06-27 10:24:15.646223
54	ARN	1	4	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>ARN</title>\n    <para>Acid Ribonucleic - moleculă implicată în sinteza proteinelor</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Acid Ribonucleic</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Moleculă implicată în sinteza proteinelor și expresia genelor	134	25	12	2025-06-27 10:24:15.646223
56	RNA	2	4	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>RNA</title>\n    <para>Ribonucleic Acid - molecule involved in protein synthesis</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Ribonucleic Acid</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Molecule involved in protein synthesis and gene expression	198	37	18	2025-06-27 10:24:15.646223
58	FAQ	1	5	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>FAQ</title>\n    <para>Întrebări Frecvente - secțiune cu răspunsuri la întrebări comune</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Întrebări Frecvente</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Secțiune cu răspunsuri la întrebările cel mai des puse	89	16	8	2025-06-27 10:24:15.646223
59	PDF	1	5	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>PDF</title>\n    <para>Format de Document Portabil - format de fișier pentru documente</para>\n    <variablelist>\n        <varlistentry>\n            <term>Semnificație</term>\n            <listitem><para>Format de Document Portabil</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Format de fișier pentru documente care păstrează formatarea	123	23	11	2025-06-27 10:24:15.646223
60	FAQ	2	5	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>FAQ</title>\n    <para>Frequently Asked Questions - section with answers to common questions</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Frequently Asked Questions</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Section containing answers to the most commonly asked questions	156	29	14	2025-06-27 10:24:15.646223
61	GPS	2	5	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>GPS</title>\n    <para>Global Positioning System - satellite navigation system</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Global Positioning System</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Satellite navigation system providing location and time information	234	44	22	2025-06-27 10:24:15.646223
62	OMS	3	2	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>OMS</title>\n    <para>Organisation Mondiale de la Santé - agence spécialisée des Nations Unies</para>\n    <variablelist>\n        <varlistentry>\n            <term>Signification</term>\n            <listitem><para>Organisation Mondiale de la Santé</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Agence spécialisée des Nations Unies pour la santé publique internationale	178	33	16	2025-06-27 10:24:15.646223
63	IRM	3	2	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>IRM</title>\n    <para>Imagerie par Résonance Magnétique - technique d'imagerie médicale</para>\n    <variablelist>\n        <varlistentry>\n            <term>Signification</term>\n            <listitem><para>Imagerie par Résonance Magnétique</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Technique d'imagerie médicale utilisant des champs magnétiques	145	27	13	2025-06-27 10:24:15.646223
64	WHO	4	2	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>WHO</title>\n    <para>Weltgesundheitsorganisation - Sonderorganisation der Vereinten Nationen</para>\n    <variablelist>\n        <varlistentry>\n            <term>Bedeutung</term>\n            <listitem><para>Weltgesundheitsorganisation</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Sonderorganisation der Vereinten Nationen für internationale Gesundheit	167	31	15	2025-06-27 10:24:15.646223
65	MRT	4	2	2	<article xmlns="http://docbook.org/ns/docbook">\n    <title>MRT</title>\n    <para>Magnetresonanztomographie - bildgebendes Verfahren in der Medizin</para>\n    <variablelist>\n        <varlistentry>\n            <term>Bedeutung</term>\n            <listitem><para>Magnetresonanztomographie</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Bildgebendes Verfahren mit Magnetfeldern und Radiowellen	134	25	12	2025-06-27 10:24:15.646223
38	AI	2	1	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>AI</title>\n    <para>Artificial Intelligence - technology that enables machines to learn and think</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Artificial Intelligence</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Technology that enables machines to learn and think like humans	234	47	24	2025-06-27 10:24:15.646223
50	CEO	2	3	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>CEO</title>\n    <para>Chief Executive Officer - highest-ranking executive in a company</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Chief Executive Officer</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Highest-ranking executive responsible for making major decisions	312	59	30	2025-06-27 10:24:15.646223
57	NASA	2	4	3	<article xmlns="http://docbook.org/ns/docbook">\n    <title>NASA</title>\n    <para>National Aeronautics and Space Administration - US space agency</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>National Aeronautics and Space Administration</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	United States government agency responsible for space exploration	345	65	33	2025-06-27 10:24:15.646223
55	DNA	2	4	1	<article xmlns="http://docbook.org/ns/docbook">\n    <title>DNA</title>\n    <para>Deoxyribonucleic Acid - molecule that carries genetic information</para>\n    <variablelist>\n        <varlistentry>\n            <term>Meaning</term>\n            <listitem><para>Deoxyribonucleic Acid</para></listitem>\n        </varlistentry>\n    </variablelist>\n</article>	Molecule that carries genetic information in all living organisms	278	53	27	2025-06-27 10:24:15.646223
\.


--
-- Data for Name: domains; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.domains (id, code, name) FROM stdin;
1	tech	Tehnologie
2	medical	Medical
3	business	Business
4	science	Științe
5	general	General
\.


--
-- Data for Name: languages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.languages (id, code, name) FROM stdin;
1	ro	Română
2	en	English
3	fr	Français
4	de	Deutsch
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, email, password, role, created_at) FROM stdin;
1	admin	admin@ama.ro	$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi	admin	2025-06-24 22:50:08.639039
2	user	user@ama.ro	$2a$10$TKh8H1.PfQx37YgCzwiKb.KjNyWgaHb9cbcoQgdIVFlYg7B77UdFm	user	2025-06-24 22:50:08.639039
3	test	test@ama.ro	$2a$10$TKh8H1.PfQx37YgCzwiKb.KjNyWgaHb9cbcoQgdIVFlYg7B77UdFm	user	2025-06-24 22:50:08.639039
5	alex	alex@email.com	d9508122cd143d69df229bf3624b7bcb2b8ac81ed210a0c926455ef119c12abd	user	2025-06-25 14:14:32.526263
4	rares	rares@email.com	eae6860224233e71b064fec5b227856909759de603cfe300057e22299a105846	admin	2025-06-25 13:06:22.695238
\.


--
-- Name: abbreviation_favorites_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.abbreviation_favorites_id_seq', 17, true);


--
-- Name: abbreviation_likes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.abbreviation_likes_id_seq', 18, true);


--
-- Name: abbreviation_meanings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.abbreviation_meanings_id_seq', 71, true);


--
-- Name: abbreviations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.abbreviations_id_seq', 65, true);


--
-- Name: domains_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.domains_id_seq', 5, true);


--
-- Name: languages_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.languages_id_seq', 4, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 5, true);


--
-- Name: abbreviation_favorites abbreviation_favorites_abbreviation_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_favorites
    ADD CONSTRAINT abbreviation_favorites_abbreviation_id_user_id_key UNIQUE (abbreviation_id, user_id);


--
-- Name: abbreviation_favorites abbreviation_favorites_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_favorites
    ADD CONSTRAINT abbreviation_favorites_pkey PRIMARY KEY (id);


--
-- Name: abbreviation_likes abbreviation_likes_abbreviation_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_likes
    ADD CONSTRAINT abbreviation_likes_abbreviation_id_user_id_key UNIQUE (abbreviation_id, user_id);


--
-- Name: abbreviation_likes abbreviation_likes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_likes
    ADD CONSTRAINT abbreviation_likes_pkey PRIMARY KEY (id);


--
-- Name: abbreviation_meanings abbreviation_meanings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_meanings
    ADD CONSTRAINT abbreviation_meanings_pkey PRIMARY KEY (id);


--
-- Name: abbreviations abbreviations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviations
    ADD CONSTRAINT abbreviations_pkey PRIMARY KEY (id);


--
-- Name: domains domains_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.domains
    ADD CONSTRAINT domains_code_key UNIQUE (code);


--
-- Name: domains domains_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.domains
    ADD CONSTRAINT domains_pkey PRIMARY KEY (id);


--
-- Name: languages languages_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.languages
    ADD CONSTRAINT languages_code_key UNIQUE (code);


--
-- Name: languages languages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.languages
    ADD CONSTRAINT languages_pkey PRIMARY KEY (id);


--
-- Name: abbreviations unique_abbreviation_language_domain; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviations
    ADD CONSTRAINT unique_abbreviation_language_domain UNIQUE (name, language_id, domain_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: idx_abbreviations_domain; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_abbreviations_domain ON public.abbreviations USING btree (domain_id);


--
-- Name: idx_abbreviations_language; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_abbreviations_language ON public.abbreviations USING btree (language_id);


--
-- Name: idx_abbreviations_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_abbreviations_name ON public.abbreviations USING btree (name);


--
-- Name: idx_abbreviations_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_abbreviations_user ON public.abbreviations USING btree (user_id);


--
-- Name: abbreviation_favorites trigger_update_favorites; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_update_favorites AFTER INSERT OR DELETE ON public.abbreviation_favorites FOR EACH ROW EXECUTE FUNCTION public.update_favorites_count();


--
-- Name: abbreviation_likes trigger_update_likes; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_update_likes AFTER INSERT OR DELETE ON public.abbreviation_likes FOR EACH ROW EXECUTE FUNCTION public.update_likes_count();


--
-- Name: abbreviation_favorites abbreviation_favorites_abbreviation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_favorites
    ADD CONSTRAINT abbreviation_favorites_abbreviation_id_fkey FOREIGN KEY (abbreviation_id) REFERENCES public.abbreviations(id) ON DELETE CASCADE;


--
-- Name: abbreviation_favorites abbreviation_favorites_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_favorites
    ADD CONSTRAINT abbreviation_favorites_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: abbreviation_likes abbreviation_likes_abbreviation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_likes
    ADD CONSTRAINT abbreviation_likes_abbreviation_id_fkey FOREIGN KEY (abbreviation_id) REFERENCES public.abbreviations(id) ON DELETE CASCADE;


--
-- Name: abbreviation_likes abbreviation_likes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_likes
    ADD CONSTRAINT abbreviation_likes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: abbreviation_meanings abbreviation_meanings_abbreviation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviation_meanings
    ADD CONSTRAINT abbreviation_meanings_abbreviation_id_fkey FOREIGN KEY (abbreviation_id) REFERENCES public.abbreviations(id) ON DELETE CASCADE;


--
-- Name: abbreviations abbreviations_domain_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviations
    ADD CONSTRAINT abbreviations_domain_id_fkey FOREIGN KEY (domain_id) REFERENCES public.domains(id);


--
-- Name: abbreviations abbreviations_language_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviations
    ADD CONSTRAINT abbreviations_language_id_fkey FOREIGN KEY (language_id) REFERENCES public.languages(id);


--
-- Name: abbreviations abbreviations_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.abbreviations
    ADD CONSTRAINT abbreviations_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

