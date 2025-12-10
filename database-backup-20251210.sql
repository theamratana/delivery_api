--
-- PostgreSQL database dump
--

\restrict TuP8y0wkJnTBeQvJvK8A6aaNS4dxBNi0hZdSb0dXgsZLYE7qXeFV0rI6bRdBhgL

-- Dumped from database version 16.10 (Debian 16.10-1.pgdg13+1)
-- Dumped by pg_dump version 16.10 (Debian 16.10-1.pgdg13+1)

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auth_identities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.auth_identities (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    display_name character varying(255),
    last_login_at timestamp(6) with time zone,
    provider character varying(32) NOT NULL,
    provider_user_id character varying(128) NOT NULL,
    username character varying(255),
    user_id uuid NOT NULL,
    password_hash character varying(255),
    CONSTRAINT auth_identities_provider_check CHECK (((provider)::text = ANY (ARRAY[('TELEGRAM'::character varying)::text, ('PHONE'::character varying)::text, ('GOOGLE'::character varying)::text, ('FACEBOOK'::character varying)::text, ('TIKTOK'::character varying)::text, ('LOCAL'::character varying)::text])))
);


ALTER TABLE public.auth_identities OWNER TO postgres;

--
-- Name: companies; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.companies (
    id uuid NOT NULL,
    active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    name character varying(200) NOT NULL,
    updated_at timestamp(6) with time zone,
    address text,
    district character varying(255),
    province character varying(255),
    district_id uuid,
    category_id uuid,
    phone_number character varying(20),
    province_id uuid,
    created_by_user_id uuid,
    updated_by_user_id uuid,
    created_by_company_id uuid
);


ALTER TABLE public.companies OWNER TO postgres;

--
-- Name: company_categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.company_categories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    name_km character varying(100),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.company_categories OWNER TO postgres;

--
-- Name: company_invitations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.company_invitations (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    invitation_code character varying(255) NOT NULL,
    phone_number character varying(255),
    role character varying(255) NOT NULL,
    company_id uuid NOT NULL,
    CONSTRAINT company_invitations_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'MANAGER'::character varying, 'STAFF'::character varying, 'DRIVER'::character varying])::text[])))
);


ALTER TABLE public.company_invitations OWNER TO postgres;

--
-- Name: company_invitations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.company_invitations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.company_invitations_id_seq OWNER TO postgres;

--
-- Name: company_invitations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.company_invitations_id_seq OWNED BY public.company_invitations.id;


--
-- Name: delivery_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.delivery_items (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    currency character varying(3),
    delivery_address text,
    delivery_fee numeric(10,2),
    delivery_lat numeric(10,8),
    delivery_lng numeric(11,8),
    estimated_delivery_time timestamp(6) with time zone,
    item_description text,
    item_value numeric(10,2),
    pickup_address text,
    pickup_lat numeric(10,8),
    pickup_lng numeric(11,8),
    status character varying(255) NOT NULL,
    delivery_company_id uuid,
    delivery_driver_id uuid,
    receiver_id uuid,
    sender_id uuid,
    delivery_district character varying(255),
    delivery_province character varying(255),
    pickup_district character varying(255),
    pickup_province character varying(255),
    auto_created_company boolean NOT NULL,
    auto_created_driver boolean NOT NULL,
    auto_created_product boolean NOT NULL,
    auto_created_receiver boolean NOT NULL,
    fee_auto_calculated boolean NOT NULL,
    photo_urls text,
    product_id uuid,
    payment_method character varying(255) DEFAULT 'COD'::character varying NOT NULL,
    quantity integer DEFAULT 1 NOT NULL,
    batch_id uuid,
    last_status_note text,
    delivery_discount numeric(10,2) DEFAULT 0.00,
    order_discount numeric(10,2) DEFAULT 0.00,
    sub_total numeric(10,2),
    grand_total numeric(10,2),
    actual_delivery_cost numeric(10,2),
    khr_amount numeric(15,2),
    exchange_rate_used numeric(10,4),
    item_discount numeric(10,2) DEFAULT 0.00,
    sender_name character varying(255),
    sender_phone character varying(20),
    CONSTRAINT delivery_items_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'ASSIGNED'::character varying, 'PICKED_UP'::character varying, 'IN_TRANSIT'::character varying, 'OUT_FOR_DELIVERY'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying, 'RETURNED'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.delivery_items OWNER TO postgres;

--
-- Name: COLUMN delivery_items.delivery_discount; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.delivery_items.delivery_discount IS 'Discount applied specifically to delivery fee';


--
-- Name: COLUMN delivery_items.order_discount; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.delivery_items.order_discount IS 'Order-wide discount applied to the total';


--
-- Name: COLUMN delivery_items.sub_total; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.delivery_items.sub_total IS 'Calculated: item_value + delivery_fee - delivery_discount';


--
-- Name: COLUMN delivery_items.grand_total; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.delivery_items.grand_total IS 'Calculated: sub_total - order_discount';


--
-- Name: COLUMN delivery_items.actual_delivery_cost; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.delivery_items.actual_delivery_cost IS 'Actual delivery cost before discount (for tracking free delivery)';


--
-- Name: COLUMN delivery_items.khr_amount; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.delivery_items.khr_amount IS 'Grand total converted to KHR using exchange_rate_used';


--
-- Name: COLUMN delivery_items.exchange_rate_used; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.delivery_items.exchange_rate_used IS 'Exchange rate snapshot at time of transaction';


--
-- Name: delivery_packages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.delivery_packages (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    delivery_fee numeric(10,2),
    notes text,
    status character varying(255),
    sender_id uuid,
    CONSTRAINT delivery_packages_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'AWAITING_PICKUP'::character varying, 'PICKED_UP'::character varying, 'IN_TRANSIT'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying])::text[])))
);


ALTER TABLE public.delivery_packages OWNER TO postgres;

--
-- Name: delivery_photos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.delivery_photos (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    photo_url text NOT NULL,
    sequence_order integer NOT NULL,
    uploaded_at timestamp(6) with time zone NOT NULL,
    delivery_item_id uuid NOT NULL
);


ALTER TABLE public.delivery_photos OWNER TO postgres;

--
-- Name: delivery_pricing_rules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.delivery_pricing_rules (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    base_fee numeric(10,2) NOT NULL,
    district character varying(255),
    high_value_surcharge numeric(10,2),
    high_value_threshold numeric(10,2),
    is_active boolean NOT NULL,
    priority integer NOT NULL,
    province character varying(255),
    rule_name character varying(255) NOT NULL,
    company_id uuid NOT NULL
);


ALTER TABLE public.delivery_pricing_rules OWNER TO postgres;

--
-- Name: delivery_tracking; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.delivery_tracking (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    description text,
    latitude numeric(10,8),
    location text,
    longitude numeric(11,8),
    status character varying(255) NOT NULL,
    "timestamp" timestamp(6) with time zone NOT NULL,
    delivery_item_id uuid NOT NULL,
    status_updated_by uuid,
    CONSTRAINT delivery_tracking_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'ASSIGNED'::character varying, 'PICKED_UP'::character varying, 'IN_TRANSIT'::character varying, 'OUT_FOR_DELIVERY'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying, 'RETURNED'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.delivery_tracking OWNER TO postgres;

--
-- Name: districts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.districts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    name_kh character varying(100),
    code character varying(20) NOT NULL,
    province_id uuid NOT NULL,
    type character varying(50),
    area_km2 integer,
    population integer,
    postal_code character varying(10),
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by uuid,
    updated_by uuid,
    communes_commune integer DEFAULT 0,
    communes_sangkat integer DEFAULT 0,
    total_villages integer DEFAULT 0,
    reference_number character varying(50),
    reference_year integer,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid
);


ALTER TABLE public.districts OWNER TO postgres;

--
-- Name: TABLE districts; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.districts IS 'Cambodian districts/khans within provinces';


--
-- Name: COLUMN districts.name_kh; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.name_kh IS 'District name in Khmer script';


--
-- Name: COLUMN districts.code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.code IS 'Unique district code (province_code + sequential number)';


--
-- Name: COLUMN districts.type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.type IS 'Type of administrative unit (district, municipality, etc.)';


--
-- Name: COLUMN districts.communes_commune; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.communes_commune IS 'Number of rural communes in the district';


--
-- Name: COLUMN districts.communes_sangkat; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.communes_sangkat IS 'Number of urban sangkats in the district';


--
-- Name: COLUMN districts.total_villages; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.total_villages IS 'Total number of villages in the district';


--
-- Name: COLUMN districts.reference_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.reference_number IS 'Reference to Royal Gazette or official document';


--
-- Name: COLUMN districts.reference_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.districts.reference_year IS 'Year of the reference document';


--
-- Name: employees; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.employees (
    id uuid NOT NULL,
    active boolean NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    display_name character varying(255),
    first_name character varying(100),
    last_name character varying(100),
    phone_e164 character varying(255),
    updated_at timestamp(6) with time zone,
    user_role character varying(255) NOT NULL,
    company_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT employees_user_role_check CHECK (((user_role)::text = ANY ((ARRAY['OWNER'::character varying, 'MANAGER'::character varying, 'STAFF'::character varying, 'DRIVER'::character varying])::text[])))
);


ALTER TABLE public.employees OWNER TO postgres;

--
-- Name: exchange_rates; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.exchange_rates (
    id uuid NOT NULL,
    from_currency character varying(3) DEFAULT 'USD'::character varying NOT NULL,
    to_currency character varying(3) DEFAULT 'KHR'::character varying NOT NULL,
    rate numeric(12,4) DEFAULT 4000.0000 NOT NULL,
    effective_date timestamp with time zone DEFAULT now() NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_by uuid,
    updated_by uuid,
    company_id uuid
);


ALTER TABLE public.exchange_rates OWNER TO postgres;

--
-- Name: COLUMN exchange_rates.company_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.exchange_rates.company_id IS 'Company-specific exchange rate. NULL means system-wide default rate.';


--
-- Name: images; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.images (
    id uuid NOT NULL,
    url character varying(512) NOT NULL,
    uploader_id uuid,
    company_id uuid,
    created_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.images OWNER TO postgres;

--
-- Name: otp_attempts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.otp_attempts (
    id uuid NOT NULL,
    chat_id bigint,
    code_hash character varying(128),
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    link_code character varying(16) NOT NULL,
    max_tries integer NOT NULL,
    phonee164 character varying(32) NOT NULL,
    status character varying(32) NOT NULL,
    tries_count integer NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT otp_attempts_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'WAITING_FOR_CONTACT'::character varying, 'SENT'::character varying, 'VERIFIED'::character varying, 'EXPIRED'::character varying, 'BLOCKED'::character varying])::text[])))
);


ALTER TABLE public.otp_attempts OWNER TO postgres;

--
-- Name: pending_employees; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pending_employees (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    phonee164 character varying(32) NOT NULL,
    role character varying(32) NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    company_id uuid NOT NULL,
    CONSTRAINT pending_employees_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'MANAGER'::character varying, 'STAFF'::character varying, 'DRIVER'::character varying])::text[])))
);


ALTER TABLE public.pending_employees OWNER TO postgres;

--
-- Name: product_categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_categories (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    code character varying(50) NOT NULL,
    is_active boolean NOT NULL,
    khmer_name character varying(100),
    name character varying(100) NOT NULL,
    sort_order integer NOT NULL
);


ALTER TABLE public.product_categories OWNER TO postgres;

--
-- Name: product_images; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_images (
    id uuid NOT NULL,
    photo_index integer NOT NULL,
    image_id uuid NOT NULL,
    product_id uuid NOT NULL
);


ALTER TABLE public.product_images OWNER TO postgres;

--
-- Name: product_photos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_photos (
    product_id uuid NOT NULL,
    photo_url character varying(512),
    photo_index integer NOT NULL,
    id uuid NOT NULL
);


ALTER TABLE public.product_photos OWNER TO postgres;

--
-- Name: product_product_photos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_product_photos (
    product_id uuid NOT NULL,
    product_photos character varying(255)
);


ALTER TABLE public.product_product_photos OWNER TO postgres;

--
-- Name: products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.products (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    created_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    default_price numeric(10,2),
    description text,
    dimensions character varying(50),
    is_active boolean NOT NULL,
    last_used_at timestamp(6) with time zone,
    name character varying(255) NOT NULL,
    usage_count integer NOT NULL,
    weight_kg numeric(5,2),
    category_id uuid,
    company_id uuid NOT NULL,
    buying_price numeric(10,2),
    selling_price numeric(10,2),
    is_published boolean DEFAULT false NOT NULL,
    last_sell_price numeric(10,2)
);


ALTER TABLE public.products OWNER TO postgres;

--
-- Name: provinces; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.provinces (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    name_kh character varying(100),
    code character varying(2) NOT NULL,
    capital character varying(100),
    area_km2 integer,
    population integer,
    districts_krong integer DEFAULT 0,
    districts_srok integer DEFAULT 0,
    districts_khan integer DEFAULT 0,
    communes_commune integer DEFAULT 0,
    communes_sangkat integer DEFAULT 0,
    total_villages integer DEFAULT 0,
    reference_number character varying(50),
    reference_year integer,
    is_active boolean DEFAULT true,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by uuid,
    updated_by uuid,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp(6) with time zone,
    deleted_by uuid,
    total_communes integer,
    total_districts integer
);


ALTER TABLE public.provinces OWNER TO postgres;

--
-- Name: TABLE provinces; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.provinces IS 'Cambodian provinces with geographic, demographic, and administrative division data';


--
-- Name: COLUMN provinces.name_kh; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.name_kh IS 'Province name in Khmer script';


--
-- Name: COLUMN provinces.code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.code IS 'Numeric province code (01-25) as per Royal Gazette';


--
-- Name: COLUMN provinces.districts_krong; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.districts_krong IS 'Number of Krong (municipal) districts';


--
-- Name: COLUMN provinces.districts_srok; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.districts_srok IS 'Number of Srok (rural) districts';


--
-- Name: COLUMN provinces.districts_khan; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.districts_khan IS 'Number of Khan (urban) districts in Phnom Penh';


--
-- Name: COLUMN provinces.communes_commune; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.communes_commune IS 'Number of rural communes';


--
-- Name: COLUMN provinces.communes_sangkat; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.communes_sangkat IS 'Number of urban sangkats';


--
-- Name: COLUMN provinces.total_villages; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.total_villages IS 'Total number of villages across the province';


--
-- Name: COLUMN provinces.reference_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.reference_number IS 'Reference to Royal Gazette or official document';


--
-- Name: COLUMN provinces.reference_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.provinces.reference_year IS 'Year of the reference document';


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.refresh_tokens (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    device_info character varying(255),
    expires_at timestamp(6) with time zone NOT NULL,
    ip_address character varying(255),
    token_hash character varying(64) NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.refresh_tokens OWNER TO postgres;

--
-- Name: token_blacklist; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.token_blacklist (
    id character varying(255) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    reason character varying(255),
    token_hash character varying(64) NOT NULL,
    user_id uuid
);


ALTER TABLE public.token_blacklist OWNER TO postgres;

--
-- Name: user_audits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_audits (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    field_name character varying(50) NOT NULL,
    new_value text,
    old_value text,
    source character varying(50) NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.user_audits OWNER TO postgres;

--
-- Name: user_phones; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_phones (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    phone_e164 character varying(32) NOT NULL,
    is_primary boolean NOT NULL,
    updated_at timestamp(6) with time zone,
    verified_at timestamp(6) with time zone,
    user_id uuid NOT NULL
);


ALTER TABLE public.user_phones OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    display_name character varying(255),
    email character varying(255),
    email_verified_at timestamp(6) with time zone,
    phone_e164 character varying(255),
    phone_verified_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    active boolean NOT NULL,
    avatar_url character varying(512),
    first_name character varying(100),
    last_login_at timestamp(6) with time zone,
    last_name character varying(100),
    username character varying(50),
    company_name character varying(200),
    user_type character varying(255),
    company_id uuid,
    user_role character varying(255),
    is_incomplete boolean DEFAULT false NOT NULL,
    address text,
    district character varying(255),
    province character varying(255),
    default_address text,
    default_province_id uuid,
    default_district_id uuid,
    CONSTRAINT users_user_role_check CHECK (((user_role)::text = ANY (ARRAY[('SYSTEM_ADMINISTRATOR'::character varying)::text, ('OWNER'::character varying)::text, ('MANAGER'::character varying)::text, ('STAFF'::character varying)::text, ('DRIVER'::character varying)::text]))),
    CONSTRAINT users_user_type_check CHECK (((user_type)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'DRIVER'::character varying, 'COMPANY'::character varying, 'DELIVERY_COMPANY'::character varying, 'STORE_OWNER'::character varying, 'ADMIN'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: COLUMN users.company_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.company_id IS 'For CUSTOMER type: which company owns this customer record. NULL for other user types.';


--
-- Name: COLUMN users.default_address; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.default_address IS 'Default delivery address for this customer';


--
-- Name: COLUMN users.default_province_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.default_province_id IS 'Default province for customer deliveries';


--
-- Name: COLUMN users.default_district_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.default_district_id IS 'Default district for customer deliveries';


--
-- Name: company_invitations id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_invitations ALTER COLUMN id SET DEFAULT nextval('public.company_invitations_id_seq'::regclass);


--
-- Data for Name: auth_identities; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.auth_identities (id, created_at, display_name, last_login_at, provider, provider_user_id, username, user_id, password_hash) FROM stdin;
106363da-1189-48df-8db9-49e8a1eef646	2025-12-08 08:06:47.863304+00	System Administrator	2025-12-08 08:06:47.8439+00	LOCAL	Admin	Admin	44346f44-1dd1-4360-bd8e-7b6c92bf8025	$2a$10$iBVo31JsIsnhvsNuoq5KeupQ1g5M6gCzdAe.2nnHu0h60usL0OYFe
44865a37-657d-40b6-84ab-13b313112fcc	2025-12-08 10:38:11.558708+00	\N	\N	TELEGRAM	230752453	\N	2a8338ab-95e3-4b76-a357-784c4f5c0345	\N
fcdcc7ea-c0f4-418b-996a-a97cfd530ab4	2025-12-09 07:19:57.449988+00	\N	2025-12-09 07:19:57.430218+00	LOCAL	u_+85589504405	u_+85589504405	2a8338ab-95e3-4b76-a357-784c4f5c0345	$2a$10$9pRo6gGzDmiI0GOThU4D4OhQzd8uFvs2lXuhoUBWf1NZmSAQH8FLu
\.


--
-- Data for Name: companies; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.companies (id, active, created_at, name, updated_at, address, district, province, district_id, category_id, phone_number, province_id, created_by_user_id, updated_by_user_id, created_by_company_id) FROM stdin;
6675c21f-5cc2-44fa-8799-9787c58b2d7e	t	2025-12-08 08:06:47.855314+00	System Administration	2025-12-08 08:06:47.855343+00	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
43a072a6-90cc-422e-a47a-fe9ff64c9b3d	t	2025-12-08 09:54:06.649787+00	Test Company	2025-12-08 09:54:06.649828+00	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N
ed76b4f6-16d2-45f0-96cb-d1b90a6b6f93	t	2025-12-09 11:13:08.945995+00	Bling Jewelry	2025-12-09 11:13:08.94602+00	\N	\N	\N	\N	\N	\N	\N	2a8338ab-95e3-4b76-a357-784c4f5c0345	\N	\N
\.


--
-- Data for Name: company_categories; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.company_categories (id, code, name, name_km, created_at, updated_at) FROM stdin;
0d021234-9fb9-4fc3-a32b-e317d3efe1b6	DELIVERY	Delivery	សេវាដឹកជញ្ជូន	2025-12-08 07:59:12.82098+00	2025-12-08 07:59:12.82098+00
ac922c29-fc86-4bc8-8e10-0be0f2cda5c5	JEWELRY	Jewelry Store	ហាងគ្រឿងអលង្ការ	2025-12-08 07:59:12.82098+00	2025-12-08 07:59:12.82098+00
\.


--
-- Data for Name: company_invitations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.company_invitations (id, created_at, expires_at, invitation_code, phone_number, role, company_id) FROM stdin;
\.


--
-- Data for Name: delivery_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.delivery_items (id, created_at, created_by, is_deleted, deleted_at, deleted_by, updated_at, updated_by, currency, delivery_address, delivery_fee, delivery_lat, delivery_lng, estimated_delivery_time, item_description, item_value, pickup_address, pickup_lat, pickup_lng, status, delivery_company_id, delivery_driver_id, receiver_id, sender_id, delivery_district, delivery_province, pickup_district, pickup_province, auto_created_company, auto_created_driver, auto_created_product, auto_created_receiver, fee_auto_calculated, photo_urls, product_id, payment_method, quantity, batch_id, last_status_note, delivery_discount, order_discount, sub_total, grand_total, actual_delivery_cost, khr_amount, exchange_rate_used, item_discount, sender_name, sender_phone) FROM stdin;
\.


--
-- Data for Name: delivery_packages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.delivery_packages (id, created_at, created_by, is_deleted, deleted_at, deleted_by, updated_at, updated_by, delivery_fee, notes, status, sender_id) FROM stdin;
\.


--
-- Data for Name: delivery_photos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.delivery_photos (id, created_at, created_by, is_deleted, deleted_at, deleted_by, updated_at, updated_by, photo_url, sequence_order, uploaded_at, delivery_item_id) FROM stdin;
\.


--
-- Data for Name: delivery_pricing_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.delivery_pricing_rules (id, created_at, created_by, is_deleted, deleted_at, deleted_by, updated_at, updated_by, base_fee, district, high_value_surcharge, high_value_threshold, is_active, priority, province, rule_name, company_id) FROM stdin;
\.


--
-- Data for Name: delivery_tracking; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.delivery_tracking (id, created_at, created_by, is_deleted, deleted_at, deleted_by, updated_at, updated_by, description, latitude, location, longitude, status, "timestamp", delivery_item_id, status_updated_by) FROM stdin;
\.


--
-- Data for Name: districts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.districts (id, name, name_kh, code, province_id, type, area_km2, population, postal_code, is_active, created_at, updated_at, created_by, updated_by, communes_commune, communes_sangkat, total_villages, reference_number, reference_year, is_deleted, deleted_at, deleted_by) FROM stdin;
49e0b73a-bb2d-44f1-b606-c29a3d9de062	Mongkol Borei District	ស្រុកមង្គលបូរី	0102	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	district	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	13	0	159	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
4b9a6d88-85a7-4bc1-b328-22c0f9acf3a7	Phnum Srok District	ស្រុកភ្នំស្រុក	0103	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	district	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	6	0	60	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
ef289dbd-1422-44ff-99bf-144a40424d03	Preah Netr Preah District	ស្រុកព្រះនេត្រព្រះ	0104	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	district	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	9	0	118	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
958e8f11-4c68-43ad-99b9-46d816522291	Ou Chrov District	ស្រុកអូរជ្រៅ	0105	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	district	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	7	0	56	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
91452b71-e392-46be-8418-ad59752f5048	Serei Saophoan Municipality	ក្រុងសិរីសោភ័ណ	0106	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	municipality	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	0	7	46	អនុក្រឹត្យលេខ ១៦អនក្រ.បក	2008	f	\N	\N
8f077a4b-4a20-446a-853e-c340d3f96d6d	Thma Puok District	ស្រុកថ្មពួក	0107	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	district	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	6	0	67	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
41d70158-e568-443b-a3c7-95b8fff689cd	Svay Chek District	ស្រុកស្វាយចេក	0108	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	district	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	8	0	73	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
c623abf3-297b-4ab7-b400-4038d0a5c6f5	Malai District	ស្រុកម៉ាឡៃ	0109	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	district	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	6	0	49	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
d03edaa3-4eda-4590-8548-5e5b9ad93e7f	Paoy Paet Municipality	ក្រុងប៉ោយប៉ែត	0110	d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	municipality	\N	\N	\N	t	2025-12-08 07:49:58.328781+00	2025-12-08 07:49:58.328781+00	\N	\N	0	5	38	អនុក្រឹត្យលេខ ២៣២អនក្រ.បក	2008	f	\N	\N
ac666b87-3b64-4e62-b9fe-db6c4c242045	Banan District	ស្រុកបាណន់	0201	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	8	0	77	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
826559e4-c5af-43d9-a2fe-d1871f6877be	Thma Koul District	ស្រុកថ្មគោល	0202	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	10	0	71	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
9c2d384a-5a3e-4469-bb6f-1ab49850da99	Battambang Municipality	ក្រុងបាត់ដំបង	0203	68419730-25e6-4bb3-8e00-1531618a21ea	municipality	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	0	10	62	២២៣​អនក្រុ,បក	2008	f	\N	\N
d7e46d9c-cdba-4c2d-9305-6f8c8cc5d253	Bavel District	ស្រុកបវេល	0204	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	9	0	103	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
7b049711-63c8-4d21-b6cc-7b7c197d93f1	Aek Phnum District	ស្រុកឯកភ្នំ	0205	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	7	0	45	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
be7099ca-b62c-4726-9164-ea4c50477ac9	Moung Ruessei District	ស្រុកមោងឫស្សី	0206	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	9	0	93	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
1d24bd07-4af8-4eab-a357-a4c73089263f	Rotonak Mondol District	ស្រុករតនមណ្ឌល	0207	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	5	0	38	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
7cb59cf7-109b-4e23-a285-29214a50e21a	Sangkae District	ស្រុកសង្កែ	0208	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	10	0	64	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
9e5d2883-ec4d-401c-98d7-e1880b290d15	Samlout District	ស្រុកសំឡូត	0209	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	7	0	59	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
62b8c415-a517-4f99-ae72-395a1e459b8f	Sampov Lun District	ស្រុកសំពៅលូន	0210	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	6	0	42	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
77400857-e5ff-4722-b460-3aacbc691c8c	Phnum Proek District	ស្រុកភ្នំព្រឹក	0211	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	5	0	45	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
2ea86b86-76a9-4e63-9104-c39a919141bf	Kamrieng District	ស្រុកកំរៀង	0212	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	6	0	49	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
518937a6-2a09-4114-999d-7d5b2e22b11f	Koas Krala District	ស្រុកគាស់ក្រឡ	0213	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	6	0	51	លេខ​៤៩៣ប្រ.ក	2008	f	\N	\N
0f5b51ee-7fe1-4212-bf0c-6c33c2e3b106	Rukh Kiri District	ស្រុករុក្ខគិរី	0214	68419730-25e6-4bb3-8e00-1531618a21ea	district	\N	\N	\N	t	2025-12-08 07:49:58.331746+00	2025-12-08 07:49:58.331746+00	\N	\N	5	0	45	លេខ​04អនក្រ.បក	2008	f	\N	\N
04be610e-7323-4b7b-9b0d-25c02eafdbdf	Batheay District	ស្រុកបាធាយ	0301	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	10	0	89	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
1c2aeb63-cb10-44b7-90a2-28d87ae75ed2	Chamkar Leu District	ស្រុកចំការលើ	0302	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	9	0	75	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
46e7952a-312a-4b7b-bbe3-694f64e2b286	Cheung Prey District	ស្រុកជើងព្រៃ	0303	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	12	0	102	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
ae280751-9fa2-4524-a920-ec447d8dcf13	Chhuk District	ស្រុកឈូក	0304	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	8	0	67	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
e02c1372-590a-4c7f-960a-d51e2148a8b0	Chum Kiri District	ស្រុកជុំគិរី	0305	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	8	0	71	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
befb1426-54b1-48ca-9240-8d39def5d2a7	Dambae District	ស្រុកដំបែ	0306	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	9	0	78	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
07c4cad1-9d17-458d-affd-3ff811462841	Kang Meas District	ស្រុកកងមាស	0307	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	11	0	94	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
33c12920-a5eb-419c-a65d-4b46a844480d	Kaoh Soutin District	ស្រុកកោះសូទិន	0308	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	10	0	85	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
02fd2cce-7b0f-4cc1-bd29-b36e199f73ff	Kampong Cham Municipality	ក្រុងកំពង់ចាម	0309	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	municipality	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	0	4	26	អនុក្រឹត្យលេខ ២៣០ អនក្រ.បក	2008	f	\N	\N
bb710ffe-5624-40cf-a73c-75c98794e3b4	Kampong Siem District	ស្រុកកំពង់សៀម	0310	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	13	0	111	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
54118582-bfb3-4885-be3f-4e222566afc8	Kang Meas District	ស្រុកកងមាស	0311	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	11	0	94	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
cea427d6-d274-4d5e-a95a-7c92cc102624	Koh Kong District	ស្រុកកោះកុង	0312	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	4	0	35	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
4eab1ad0-18de-4fbc-ac06-f0758cda60bd	Ou Reang Ov District	ស្រុកអូររាំងឪ	0313	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	7	0	60	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
7e6ab822-5a4c-414a-8195-889ce81108ee	Ponhea Kraek District	ស្រុកពញាក្រែក	0314	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	8	0	70	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
2a474986-2f58-4c1d-9d26-2a86d9b51675	Prey Chhor District	ស្រុកព្រៃឈរ	0315	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	11	0	94	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
3829e488-d264-49a2-a02d-034cd8d5b44d	Srei Santhor District	ស្រុកស្រីសន្ធរ	0316	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	9	0	76	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
fcedc2e7-e23d-46a7-aa9e-5b5419b40f0e	Stueng Trang District	ស្រុកស្ទឹងត្រែង	0317	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	8	0	69	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
34f29d00-c894-4654-bb3b-39486ff6e2d1	Tboung Khmum District	ស្រុកត្បូងឃ្មុំ	0318	13fb5678-6174-49ac-b32d-3d34cb9a7ed6	district	\N	\N	\N	t	2025-12-08 07:49:58.333293+00	2025-12-08 07:49:58.333293+00	\N	\N	14	0	125	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
730c9599-b5ed-4557-86d2-623d07795a9c	Baribour District	ស្រុកបរិបូណ៌	0401	fe0abc72-3c57-47d4-b45a-d8bc347470e2	district	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	11	0	64	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
00d4952a-34e6-475a-84b8-9b43a22f93e6	Chol Kiri District	ស្រុកជលគីរី	0402	fe0abc72-3c57-47d4-b45a-d8bc347470e2	district	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	5	0	29	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
b5a8a7b0-483f-412b-aefb-db1c9942e280	Kampong Chhnang Municipality	ក្រុងកំពង់ឆ្នាំង	0403	fe0abc72-3c57-47d4-b45a-d8bc347470e2	municipality	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	0	4	26	អនុក្រឹត្យលេខ២៣១ អនក្រ.បក	2008	f	\N	\N
6d4e18b4-cb75-4256-b12b-057ca64e1437	Kampong Leaeng District	ស្រុកកំពង់លែង	0404	fe0abc72-3c57-47d4-b45a-d8bc347470e2	district	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	9	0	44	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
674e33e1-e113-4d96-ac27-b7a9681cfabd	Kampong Tralach District	ស្រុកកំពង់ត្រឡាច	0405	fe0abc72-3c57-47d4-b45a-d8bc347470e2	district	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	10	0	103	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
9a4e65bf-20f9-401b-b49c-8b80d0b1f201	Rolea B'ier District	ស្រុករលាប្អៀរ	0406	fe0abc72-3c57-47d4-b45a-d8bc347470e2	district	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	14	0	135	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
4c899020-28e6-4f68-a8fe-6c905404233a	Sameakki Mean Chey District	ស្រុកសាមគ្គីមានជ័យ	0407	fe0abc72-3c57-47d4-b45a-d8bc347470e2	district	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	9	0	90	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
db6b2d14-47ff-43a7-bc5e-0b7a8d9d5b86	Tuek Phos District	ស្រុកទឹកផុស	0408	fe0abc72-3c57-47d4-b45a-d8bc347470e2	district	\N	\N	\N	t	2025-12-08 07:49:58.33489+00	2025-12-08 07:49:58.33489+00	\N	\N	9	0	78	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
785b3535-8deb-4e93-8c43-8cf19110dd53	Basedth District	ស្រុកបរសេដ្ឋ	0501	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	district	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	15	0	218	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
42c3d46c-7163-4069-bbe2-72301d6c6ce8	Chbar Mon Municipality	ក្រុងច្បារមន	0502	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	municipality	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	0	5	56	អនុក្រឹត្យលេខ​ ២២៩ អនក្រ.បក	2008	f	\N	\N
5f244dc0-a4ff-4aff-be98-8b8ab3ce83e9	Kong Pisei District	ស្រុកគងពិសី	0503	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	district	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	13	0	250	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
b6ebde9c-351f-4ce3-9991-ebf97f9beac8	Aoral District	ស្រុកឱរ៉ាល់	0504	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	district	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	5	0	67	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
b2d26223-8110-4e74-bf11-af839b2760df	Phnum Sruoch District	ស្រុកភ្នំស្រួច	0506	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	district	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	13	0	149	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
7b7d90f3-36de-42f9-bdaa-16e57c328009	Samraong Tong District	ស្រុកសំរោងទង	0507	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	district	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	15	0	290	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
2efdd056-0636-4e50-93a2-a2f1eba340a3	Thpong District	ស្រុកថ្ពង	0508	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	district	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	7	0	84	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
eb3c1a65-7704-4076-9e10-e0e7a9a5770b	Odongk Maechay Municipality	ក្រុងឧដុង្គម៉ែជ័យ	0509	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	municipality	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	0	5	91	អនុក្រឹត្យលេខ២៧១ អនក្រ.បក ២៣ ធ្នូ ២០២២	2022	f	\N	\N
4836d9d3-f956-47a9-9e0a-6c1f52e9a404	Samkkei Munichay District	ស្រុកសាមគ្គីមុនីជ័យ	0510	2adc9b60-824a-4b94-b6f7-5b7df6e784bf	district	\N	\N	\N	t	2025-12-08 07:49:58.336111+00	2025-12-08 07:49:58.336111+00	\N	\N	10	0	160	អនុក្រឹត្យលេខ២៧១ អនក្រ.បក ២៣ ធ្នូ ២០២២	2022	f	\N	\N
44b2ad1d-7f67-4b94-bbc1-96f0eed65cb8	Baray District	ស្រុកបារាយណ៍	0601	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	10	0	97	ប្រកាសលេខ ៤៩៣ប្រ.ក	2008	f	\N	\N
f30fe063-10b8-4200-9d92-af26fcbb7953	Kampong Svay District	ស្រុកកំពង់ស្វាយ	0602	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	11	0	97	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
f7f0a331-5587-44fb-a770-8b0c7d32524a	Stueng Saen Municipality	ក្រុងស្ទឹងសែន	0603	2b424981-183e-4bc4-8249-ed001c475882	municipality	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	0	8	39	អនុក្រឹតលេខ ១៥ អនក្រ.បក	2008	f	\N	\N
34489424-670f-42e2-94bc-af53d230683b	Prasat Ballangk District	ស្រុកប្រាសាទបល្ល័ង្គ	0604	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	7	0	64	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
a5d5d9bc-3c4d-4fa8-8c85-fa48a36d037c	Prasat Sambour District	ស្រុកប្រាសាទសំបូរ	0605	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	5	0	66	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
00480660-14ab-41d9-a80a-2475aea38610	Sandan District	ស្រុកសណ្ដាន់	0606	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	9	0	84	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
edc116ec-0fd3-4a78-90e5-1076392cae7b	Santuk District	ស្រុកសន្ទុក	0607	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	10	0	92	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
4772aeb5-8bbf-495f-8f37-d0f9ded34352	Stoung District	ស្រុកស្ទោង	0608	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	13	0	135	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
ad0ea497-f7b5-4727-9aed-5161dc5a66c4	Taing Kouk District	ស្រុកតាំងគោក	0609	2b424981-183e-4bc4-8249-ed001c475882	district	\N	\N	\N	t	2025-12-08 07:49:58.337524+00	2025-12-08 07:49:58.337524+00	\N	\N	8	0	91	អនុក្រឹតលេខ០៥ អនក្រ.បក	2008	f	\N	\N
e824d100-4dab-46d6-ab18-8198907fabfa	Angkor Chey District	ស្រុកអង្គរជ័យ	0701	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	district	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	11	0	79	ប្រកាសលេខ៤៩៣​ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
73bb5500-59fe-45cc-b385-06e732a02cfe	Banteay Meas District	ស្រុកបន្ទាយមាស	0702	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	district	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	15	0	88	ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
9f5eda5b-0113-4428-af26-77d1b6424200	Chhuk District	ស្រុកឈូក	0703	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	district	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	15	0	80	ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
1fe586fa-e2ce-4e6c-9f93-b031e6ed14bf	Chum Kiri District	ស្រុកជុំគិរី	0704	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	district	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	7	0	39	ប្រកាសលេខ​ ៤៩៣​ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
c1c14fab-f0cb-4309-a38f-a3a191e394aa	Dang Tong District	ស្រុកដងទង់	0705	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	district	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	10	0	54	ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
b741267e-0c93-4f43-bcbf-57dc144576bf	Kampong Trach District	ស្រុកកំពង់ត្រាច	0706	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	district	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	14	0	70	ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
25edde04-2e5a-4ac4-880d-87134a0451c3	Tuek Chhou District	ស្រុកទឹកឈូ	0707	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	district	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	13	0	55	អនុក្រឹត្យលេខ​ ២២១ អនក្រ.បក	2008	f	\N	\N
4993dd48-92c2-4fdf-8830-e0acafe7e285	Kampot Municipality	ក្រុងកំពត	0708	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	municipality	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	0	5	15	អនុក្រឹត្យលេខ​ ២២១ អនក្រ.បក	2008	f	\N	\N
36a0cf2a-d98f-4401-9298-c37831342da9	Bokor Municipality	ក្រុងបូកគោ	0709	ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	municipality	\N	\N	\N	t	2025-12-08 07:49:58.338628+00	2025-12-08 07:49:58.338628+00	\N	\N	0	3	11	អនុក្រឹត្យលេខ​ ៣៨ អនក្រ.បក	2008	f	\N	\N
45794c0b-7de1-4842-adca-4082b35fcd2f	Kandal Stueng District	ស្រុកកណ្ដាលស្ទឹង	0801	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	18	0	127	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
65209795-57c8-492a-aa12-8c0c882475b4	Kien Svay District	ស្រុកកៀនស្វាយ	0802	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	8	0	67	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
dc74ff51-dd9c-42e8-8fe3-17f7a0a69860	Khsach Kandal District	ស្រុកខ្សាច់កណ្ដាល	0803	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	12	0	67	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
f3278b48-d3df-41b6-9420-1f49b5983e5b	Kaoh Thum District	ស្រុកកោះធំ	0804	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	6	0	60	តាមប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
9594257d-c21a-44b2-bbc4-08aab4977bcc	Leuk Daek District	ស្រុកលើកដែក	0805	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	7	0	25	លេខ​ ៤៩៣ ប្រ.ក	2008	f	\N	\N
46999f43-ff2b-4189-8f60-16e778778782	Lvea Aem District	ស្រុកល្វាឯម	0806	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	10	0	27	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
c0275fbb-eed7-48cc-8fec-0f37cd997273	Mukh Kampul District	ស្រុកមុខកំពូល	0807	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	7	0	39	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
3749f2b6-8edc-468c-88ac-233bab5cee40	Angk Snuol District	ស្រុកអង្គស្នួល	0808	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	10	0	200	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
0a437b00-ded9-4854-80ea-526f789c563d	Ponhea Lueu District	ស្រុកពញាឮ	0809	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	11	0	124	លេខ ៤៩​៣ ប្រ.ក	2008	f	\N	\N
5f166f8a-6a23-4e7e-baad-9b55783b8f11	S'ang District	ស្រុកស្អាង	0810	51e91af6-ee6e-40c2-bfcf-03b654a4800b	district	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	12	0	120	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
b201d224-a032-4051-a3b7-64f04613f5e7	Ta Khmau Municipality	ក្រុងតាខ្មៅ	0811	51e91af6-ee6e-40c2-bfcf-03b654a4800b	municipality	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	0	10	59	២២៨ អនក្រ.បក	2008	f	\N	\N
d49b8304-1cd6-4f88-a8fa-9b2215a7981b	Sampeou Poun Municipality	ក្រុងសំពៅពូន	0812	51e91af6-ee6e-40c2-bfcf-03b654a4800b	municipality	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	0	5	53	អនុក្រឹត្យលេខ២៦៩ អនក្រ.បក ២៣ ធ្នូ ២០២២	2022	f	\N	\N
e4685f43-ab4a-463e-8981-efc9ccfbd0a9	Akreiy Ksatr Municipality	ក្រុងអរិយក្សត្រ	0813	51e91af6-ee6e-40c2-bfcf-03b654a4800b	municipality	\N	\N	\N	t	2025-12-08 07:49:58.339712+00	2025-12-08 07:49:58.339712+00	\N	\N	0	11	42	អនុក្រឹត្យលេខ២៧២ អនក្រ.បក ២៣ ធ្នូ ២០២២	2022	f	\N	\N
3ba8fc80-a5de-4f98-81cc-9ad34b1f5685	Botum Sakor District	ស្រុកបុទុមសាគរ	0901	88b07a64-f996-4bf2-b6d0-1d776c23b696	district	\N	\N	\N	t	2025-12-08 07:49:58.340971+00	2025-12-08 07:49:58.340971+00	\N	\N	4	0	21	ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
bae65e00-b306-4da8-acb3-7cae1d493477	Kiri Sakor District	ស្រុកគិរីសាគរ	0902	88b07a64-f996-4bf2-b6d0-1d776c23b696	district	\N	\N	\N	t	2025-12-08 07:49:58.340971+00	2025-12-08 07:49:58.340971+00	\N	\N	3	0	9	ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
f84099d9-cee2-40a5-b7a4-9a21de33e45e	Kaoh Kong District	ស្រុកកោះកុង	0903	88b07a64-f996-4bf2-b6d0-1d776c23b696	district	\N	\N	\N	t	2025-12-08 07:49:58.340971+00	2025-12-08 07:49:58.340971+00	\N	\N	4	0	11	ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
000a136b-89db-438c-8e0b-87d2cc0450cc	Khemara Phoumin Municipality	ក្រុងខេមរភូមិន្ទ	0904	88b07a64-f996-4bf2-b6d0-1d776c23b696	municipality	\N	\N	\N	t	2025-12-08 07:49:58.340971+00	2025-12-08 07:49:58.340971+00	\N	\N	0	3	11	អនុក្រឹត្យលេខ ២២២​ អនុក្រ.បក	2008	f	\N	\N
daa4e3b6-00ec-4ad7-b234-c8cc77953aa9	Mondol Seima District	ស្រុកមណ្ឌលសីមា	0905	88b07a64-f996-4bf2-b6d0-1d776c23b696	district	\N	\N	\N	t	2025-12-08 07:49:58.340971+00	2025-12-08 07:49:58.340971+00	\N	\N	3	0	13	ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
9590f081-a9b3-41fa-92f1-325d71b3bf5c	Srae Ambel District	ស្រុកស្រែ អំបិល	0906	88b07a64-f996-4bf2-b6d0-1d776c23b696	district	\N	\N	\N	t	2025-12-08 07:49:58.340971+00	2025-12-08 07:49:58.340971+00	\N	\N	6	0	37	ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
b6c9ed3d-0218-425d-abf0-4d61e384330f	Thma Bang District	ស្រុកថ្មបាំង	0907	88b07a64-f996-4bf2-b6d0-1d776c23b696	district	\N	\N	\N	t	2025-12-08 07:49:58.340971+00	2025-12-08 07:49:58.340971+00	\N	\N	6	0	17	ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	f	\N	\N
d32561cc-f382-44af-b404-b4ac7319d838	Chhloung District	ស្រុកឆ្លូង	1001	e522d87d-cac0-4100-9c7a-556b3ca24022	district	\N	\N	\N	t	2025-12-08 07:49:58.342442+00	2025-12-08 07:49:58.342442+00	\N	\N	8	0	50	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
28e3819a-7373-4049-91bb-1461d02ea422	Kracheh Municipality	ក្រុងក្រចេះ	1002	e522d87d-cac0-4100-9c7a-556b3ca24022	municipality	\N	\N	\N	t	2025-12-08 07:49:58.342442+00	2025-12-08 07:49:58.342442+00	\N	\N	0	5	19	អនុក្រឹតលេខ ១០ អនក្រ.បក	2008	f	\N	\N
aeb08d54-caad-42ec-bb68-51d9a406d39e	Prek Prasab District	ស្រុកព្រែកប្រសព្វ	1003	e522d87d-cac0-4100-9c7a-556b3ca24022	district	\N	\N	\N	t	2025-12-08 07:49:58.342442+00	2025-12-08 07:49:58.342442+00	\N	\N	8	0	61	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
1aaedd9a-efa0-4e0d-86db-1b2b08fc6d7d	Sambour District	ស្រុកសំបូរ	1004	e522d87d-cac0-4100-9c7a-556b3ca24022	district	\N	\N	\N	t	2025-12-08 07:49:58.342442+00	2025-12-08 07:49:58.342442+00	\N	\N	6	0	34	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
be284ec1-8988-44ad-87f6-b943802ca435	Snuol District	ស្រុកស្នួល	1005	e522d87d-cac0-4100-9c7a-556b3ca24022	district	\N	\N	\N	t	2025-12-08 07:49:58.342442+00	2025-12-08 07:49:58.342442+00	\N	\N	6	0	69	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
e2061a35-2bb6-41e4-9acf-b6e5f2ad6c1f	Chetr Borei District	ស្រុកចិត្របុរី	1006	e522d87d-cac0-4100-9c7a-556b3ca24022	district	\N	\N	\N	t	2025-12-08 07:49:58.342442+00	2025-12-08 07:49:58.342442+00	\N	\N	10	0	69	អនុក្រឹតលេខ ១០ អនក្រ.បក	2008	f	\N	\N
ea22ebb1-aeb4-49c1-8cc5-41037d4eb172	Ou Krieng Saenchey District	ស្រុកអូរគ្រៀងសែនជ័យ	1007	e522d87d-cac0-4100-9c7a-556b3ca24022	district	\N	\N	\N	t	2025-12-08 07:49:58.342442+00	2025-12-08 07:49:58.342442+00	\N	\N	5	0	25	អនុក្រឹត្យលេខ២៧០ អនក្រ.បក ២៣ ធ្នូ ២០២២	2022	f	\N	\N
c00c99f4-aadf-4ef8-b9e9-be50160963be	Kaev Seima District	ស្រុកកែវសីមា	1101	d2461c0e-a6d7-41a1-b35e-57f9fb03d8d0	district	\N	\N	\N	t	2025-12-08 07:49:58.343681+00	2025-12-08 07:49:58.343681+00	\N	\N	5	0	27	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
346ccacc-335e-43f5-bdd9-61ff1e5b782e	Kaoh Nheaek District	ស្រុកកោះញែក	1102	d2461c0e-a6d7-41a1-b35e-57f9fb03d8d0	district	\N	\N	\N	t	2025-12-08 07:49:58.343681+00	2025-12-08 07:49:58.343681+00	\N	\N	6	0	26	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
ca19900b-4603-4ee2-a02f-31d333b85402	Ou Reang District	ស្រុកអូររាំង	1103	d2461c0e-a6d7-41a1-b35e-57f9fb03d8d0	district	\N	\N	\N	t	2025-12-08 07:49:58.343681+00	2025-12-08 07:49:58.343681+00	\N	\N	2	0	7	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
74ac5f05-946a-4e4c-92d0-4ac477832cbd	Pech Chreada District	ស្រុកពេជ្រាដា	1104	d2461c0e-a6d7-41a1-b35e-57f9fb03d8d0	district	\N	\N	\N	t	2025-12-08 07:49:58.343681+00	2025-12-08 07:49:58.343681+00	\N	\N	4	0	18	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
e4647096-01c3-4276-8fb0-b633ed7d7309	Saen Monourom Municipality	ក្រុងសែនមនោរម្យ	1105	d2461c0e-a6d7-41a1-b35e-57f9fb03d8d0	municipality	\N	\N	\N	t	2025-12-08 07:49:58.343681+00	2025-12-08 07:49:58.343681+00	\N	\N	0	4	14	អនុក្រឹតលេខ ២២៥ អនក្រ.បក	2008	f	\N	\N
8e75d45b-7c14-4fcc-a4b9-7b9dc6ade995	Chamkar Mon Khan	ខណ្ឌចំការមន	1201	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	5	40	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
453d49e8-63be-42ef-85c6-8bd4451e0f4c	Doun Penh Khan	ខណ្ឌដូនពេញ	1202	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	11	134	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
30fa6ce2-86aa-4775-ab11-96659633f718	Prampir Meakkakra Khan	ខណ្ឌ៧មករា	1203	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	8	66	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
85322efa-5ce2-45dc-97a0-ad101d318bf9	Tuol Kouk Khan	ខណ្ឌទួលគោក	1204	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	10	143	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
9840b523-f3f3-4e93-8fc6-cc766565c232	Dangkao Khan	ខណ្ឌដង្កោ	1205	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	12	81	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
955a36bc-8b3a-40e6-9ad9-e4993bd4256a	Mean Chey Khan	ខណ្ឌមានជ័យ	1206	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	7	59	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
a5d1b1eb-77f5-42bc-ace5-339d136facb0	Russey Keo Khan	ខណ្ឌឫស្សីកែវ	1207	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	7	30	ប្រកាសលេខ៤៩៣ ប្រ.ក	2008	f	\N	\N
3602b76d-435d-4ec6-9cdb-852716c4cd29	Saensokh Khan	ខណ្ឌសែនសុខ	1208	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	6	47	អនុក្រឹត្យលេខ០៣ អនក្រ.បក	2008	f	\N	\N
3ec7bfe7-f9ea-4cb7-ab3c-9356902f46bd	Pur SenChey Khan	ខណ្ឌពោធិ៍សែនជ័យ	1209	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	7	75	អនុក្រិតលេខ៨០	2008	f	\N	\N
9c53961d-4aaf-40c3-921f-08daa68e19b0	Chrouy Changvar Khan	ខណ្ឌជ្រោយចង្វារ	1210	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	5	22	អនុក្រឹត្យលេខ៥៧៧ អនក្រ.បក	2008	f	\N	\N
e61edd66-9f02-4f81-a6c7-9322414c4663	Praek Pnov Khan	ខណ្ឌព្រែកព្នៅ	1211	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	5	59	អនុក្រឹត្យលេខ៥៧៨ អនក្រ.បក	2008	f	\N	\N
acdac7bb-7e4c-4ee7-b992-c61ac1ef13dc	Chbar Ampov Khan	ខណ្ឌច្បារអំពៅ	1212	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	8	49	អនុក្រឹត្យលេខ៥៧៩ អនក្រ.បក	2008	f	\N	\N
4c14ac09-ac4c-4ab2-9921-9cbeb46603bc	Boeng Keng Kang Khan	ខណ្ឌបឹងកេងកង	1213	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	7	55	អនុក្រឹត្យលេខ០៣ អនក្រ.បក	2008	f	\N	\N
32246adb-2737-443d-8db5-6931cb594365	Kamboul Khan	ខណ្ឌកំបូល	1214	d17cd5db-c2bd-4847-a439-d239bb8aa615	khan	\N	\N	\N	t	2025-12-08 07:49:58.34488+00	2025-12-08 07:49:58.34488+00	\N	\N	0	7	93	អនុក្រឹត្យលេខ០៤ អនក្រ.បក	2008	f	\N	\N
bba86a1e-5e37-4e1d-8da3-11c36d2b4553	Chey Saen District	ស្រុកជ័យសែន	1301	9a354187-b19e-41e5-aa6d-36baa100d1a0	district	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	6	0	21	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
ce4f1e3c-9c4e-4a62-818d-b69b4d625ffe	Chhaeb District	ស្រុកឆែប	1302	9a354187-b19e-41e5-aa6d-36baa100d1a0	district	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	8	0	26	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
58cd0b78-ca5d-4c49-a45c-45a9e614ce34	Choam Ksant District	ស្រុកជាំក្សាន្ដ	1303	9a354187-b19e-41e5-aa6d-36baa100d1a0	district	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	8	0	49	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
5d41d645-e601-404e-9d6c-af5d5629223d	Kuleaen District	ស្រុកក្រឡាញ់	1304	9a354187-b19e-41e5-aa6d-36baa100d1a0	district	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	6	0	23	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
f66007dd-1526-403f-bb90-a7c0010c05a1	Rovieng District	ស្រុករវៀង	1305	9a354187-b19e-41e5-aa6d-36baa100d1a0	district	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	12	0	57	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
47c750a4-f3af-42f7-97bb-735f4958663a	Sangkum Thmei District	ស្រុកសង្គមថ្មី	1306	9a354187-b19e-41e5-aa6d-36baa100d1a0	district	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	5	0	24	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
c8d7986a-2071-4416-b331-f5661f707e8c	Tbaeng Mean Chey District	ស្រុកត្បែងមានជ័យ	1307	9a354187-b19e-41e5-aa6d-36baa100d1a0	district	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	4	0	12	លេខប្រកាស ៤៩៣ ប្រ.ក	2008	f	\N	\N
5598b136-344a-4072-9c1e-1a246634d52c	Preah Vihear Municipality	ក្រុងព្រះវិហារ	1308	9a354187-b19e-41e5-aa6d-36baa100d1a0	municipality	\N	\N	\N	t	2025-12-08 07:49:58.346064+00	2025-12-08 07:49:58.346064+00	\N	\N	0	2	20	អនុក្រឹតលេខ ១១ អនក្រ.បក	2008	f	\N	\N
bd4a6cb9-cf79-455c-8f18-d6bea21df33a	Ba Phnum District	ស្រុកបាភ្នំ	1401	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	9	0	108	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
19def76a-3e82-4624-8813-79f2e98d7d7c	Kamchay Mear District	ស្រុកកំចាយមារ	1402	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	8	0	129	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
64b60955-ea0c-427f-82e4-e83ed68ae567	Kampong Trabaek District	ស្រុកកំពង់ត្របែក	1403	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	13	0	122	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
2679315f-6393-4a1c-8af2-76f899673a62	Kanhchriech District	ស្រុកកញ្ជ្រៀច	1404	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	8	0	99	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
48ec522d-0258-4402-9593-bc5c77265110	Me Sang District	ស្រុកមេសាង	1405	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	8	0	118	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
d68b789b-ab47-4d84-b04c-dc46b6d98324	Peam Chor District	ស្រុកពាមជរ	1406	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	10	0	50	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
d46e996f-c86b-4944-9155-e894b335dda0	Peam Ro District	ស្រុកពាមរក៍	1407	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	8	0	44	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
c650433d-8ebc-4dfa-a69e-716888ac2c13	Pea Reang District	ស្រុកពារាំង	1408	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	9	0	93	តាមប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
57329ee4-b1e1-4329-88a1-db10421e8666	Preah Sdach District	ស្រុកព្រះស្ដេច	1409	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	11	0	145	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
9c2cb4f3-9432-4f5e-9c22-f754c45491eb	Prey Veng Municipality	ក្រុងព្រៃវែង	1410	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	municipality	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	0	4	23	០៩ អនក្រ.បក	2008	f	\N	\N
5c817102-81b1-4d26-94cd-48fd3e3a5e98	Pur Rieng District	ស្រុកពោធិ៍រៀង	1411	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	6	0	36	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
e1229ea6-1d79-4fe2-9454-6c27fbd4e380	Sithor Kandal District	ស្រុកស៊ីធរកណ្ដាល	1412	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	11	0	63	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
21df08df-8d82-4c60-a207-032e0b658032	Svay Antor District	ស្រុកស្វាយអន្ទរ	1413	7b15297f-aed7-48b1-b211-8b49f8e2eb3b	district	\N	\N	\N	t	2025-12-08 07:49:58.347623+00	2025-12-08 07:49:58.347623+00	\N	\N	11	0	138	០៩ អនក្រ.បក	2008	f	\N	\N
f7c6f70c-36c6-4184-8178-207cfb8647bc	Bakan District	ស្រុកបាកាន	1501	bf2b3873-853d-4f72-82ef-f296b3624db1	district	\N	\N	\N	t	2025-12-08 07:49:58.349016+00	2025-12-08 07:49:58.349016+00	\N	\N	9	0	132	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
6e3d6224-871b-49f7-b595-ed475681158e	Kandieng District	ស្រុកកណ្ដៀង	1502	bf2b3873-853d-4f72-82ef-f296b3624db1	district	\N	\N	\N	t	2025-12-08 07:49:58.349016+00	2025-12-08 07:49:58.349016+00	\N	\N	9	0	112	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
290ed041-5250-401e-9da8-ba44fd322b11	Krakor District	ស្រុកក្រគរ	1503	bf2b3873-853d-4f72-82ef-f296b3624db1	district	\N	\N	\N	t	2025-12-08 07:49:58.349016+00	2025-12-08 07:49:58.349016+00	\N	\N	11	0	107	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
c2eb096a-2cff-4494-89c7-a7cb551d12f1	Phnum Kravanh District	ស្រុកភ្នំក្រវ៉ាញ	1504	bf2b3873-853d-4f72-82ef-f296b3624db1	district	\N	\N	\N	t	2025-12-08 07:49:58.349016+00	2025-12-08 07:49:58.349016+00	\N	\N	6	0	47	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
1d7650f3-9428-4798-9441-3536e6103106	Pursat Municipality	ក្រុងពោធិ៍សាត់	1505	bf2b3873-853d-4f72-82ef-f296b3624db1	municipality	\N	\N	\N	t	2025-12-08 07:49:58.349016+00	2025-12-08 07:49:58.349016+00	\N	\N	0	7	73	លេខ​ ១៤​ អនក្រ.បក	2008	f	\N	\N
54ce6dd4-d647-4a36-9e2b-5e77b0f28871	Veal Veaeng District	ស្រុកវាលវែង	1506	bf2b3873-853d-4f72-82ef-f296b3624db1	district	\N	\N	\N	t	2025-12-08 07:49:58.349016+00	2025-12-08 07:49:58.349016+00	\N	\N	5	0	20	លេខ​៤៩៣​ប្រ.ក	2008	f	\N	\N
880cfba7-b3a5-432d-83d5-f60d6d405895	Ta Lou Senchey District	ស្រុកតាលោសែនជ័យ	1507	bf2b3873-853d-4f72-82ef-f296b3624db1	district	\N	\N	\N	t	2025-12-08 07:49:58.349016+00	2025-12-08 07:49:58.349016+00	\N	\N	2	0	35	អនុក្រឹត្យលេខ ០៧ អនក្រ.បក	2008	f	\N	\N
0d5ef79e-37fb-4999-a8cb-8f43e589affd	Andoung Meas District	ស្រុកអន្លង់វែង	1601	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	3	0	21	\N	\N	f	\N	\N
4fe64c08-1f82-45d8-a4fb-2ed26d573fb1	Ban Lung Municipality	ក្រុងបានលុង	1602	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	municipality	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	0	4	19	អនុក្រឹត្យលេខ ២២៧ អនក្រ.បក	2008	f	\N	\N
b5986e0e-cc99-455c-b4e5-0814f0a59a71	Bar Kaev District	ស្រុកបរកែវ	1603	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	6	0	34	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
9fbe1995-2d6a-4ff8-a8fd-136f80e3414d	Koun Mom District	ស្រុកកូនមុំ	1604	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	6	0	23	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
fd3790c2-1779-4944-a2be-9656bc792488	Lumphat District	ស្រុកលំផាត់	1605	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	6	0	26	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
51f5fbe7-882d-4cea-bd6e-5665b922cf25	Ou Chum District	ស្រុកអូរជុំ	1606	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	7	0	37	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
238ce660-abd0-4eb2-bd5f-297d2bdcc02e	Ou Ya Dav District	ស្រុកអូរយ៉ាដាវ	1607	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	7	0	29	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
4cfe45f1-5c59-44aa-88d1-b50ddc0dc067	Ta Veaeng District	ស្រុកតាវែង	1608	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	2	0	20	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
6294848f-8edc-43a8-b51c-0b044842a024	Veun Sai District	ស្រុកវើនសៃ	1609	0cd6409c-71a4-478d-92ea-7cfc0605ea4f	district	\N	\N	\N	t	2025-12-08 07:49:58.350046+00	2025-12-08 07:49:58.350046+00	\N	\N	9	0	34	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
2221fbe5-d581-4f20-a8c5-0d8f4e56457b	Angkor Chum District	ស្រុកអង្គរជុំ	1701	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	7	0	86	\N	\N	f	\N	\N
ee3709b2-b82c-42e4-bad3-fc49a0976ddd	Angkor Thum District	ស្រុកអង្គរធំ	1702	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	4	0	25	\N	\N	f	\N	\N
cf14c22c-e5be-47ac-81c8-01cca173f62d	Banteay Srei District	ស្រុកបន្ទាយស្រី	1703	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	5	0	29	\N	\N	f	\N	\N
6730657c-2772-4342-aecc-432e5467ed42	Chi Kraeng District	ស្រុកជីក្រែង	1704	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	12	0	155	\N	\N	f	\N	\N
0289895d-9b36-45bf-b8b7-2b618495f9e5	Kralanh District	ស្រុកក្រឡាញ់	1706	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	10	0	98	\N	\N	f	\N	\N
63fcc449-fdb8-4520-a2b8-8e74f5ae29f8	Puok District	ស្រុកពួក	1707	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	14	0	132	\N	\N	f	\N	\N
8cf26686-1eaa-41c4-b259-b06985bebf9c	Prasat Bakong District	ស្រុកប្រាសាទបាគង	1710	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	8	0	59	\N	\N	f	\N	\N
4c7f7fee-bfb6-4dc7-b318-89288bb39264	Siem Reap Municipality	ក្រុងសៀមរាប	1711	3b419502-f9fa-4a36-9fac-f3302091e6d0	municipality	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	0	12	99	អនុក្រឹតលេខ ១៣ អនក្រ.បក	2008	f	\N	\N
b225ed66-4bd7-468c-8815-274b0ab195b1	Soutr Nikom District	ស្រុកសូទ្រនិគម	1712	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	10	0	113	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
824cc52c-d58a-4144-ac97-c7b0153112e4	Srei Snam District	ស្រុកស្រីស្នំ	1713	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	6	0	39	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
1d573b3a-25ac-493e-9777-603f5481c1b1	Svay Leu District	ស្រុកស្វាយលើ	1714	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	5	0	34	\N	\N	f	\N	\N
4ab66a79-84ee-40cc-a18a-361e05bdac49	Varin District	ស្រុកវ៉ារិន	1715	3b419502-f9fa-4a36-9fac-f3302091e6d0	district	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	5	0	25	\N	\N	f	\N	\N
af4ee12e-1eb8-4ef4-b22c-96dd2759473d	Run Ta Aek Techo Sen Municipality	ក្រុងរុនតាឯកតេជោសែន	1716	3b419502-f9fa-4a36-9fac-f3302091e6d0	municipality	\N	\N	\N	t	2025-12-08 07:49:58.351347+00	2025-12-08 07:49:58.351347+00	\N	\N	0	2	15	អនុក្រឹត្យលេខ០៧ ១១មករា២០២៤	2024	f	\N	\N
30aee1ce-5bcb-46dc-9c2a-d32afeab53b2	Preah Sihanouk Municipality	ក្រុងព្រះសីហនុ	1801	eb47ba73-ab32-4462-ad7a-a1156d28c7ec	municipality	\N	\N	\N	t	2025-12-08 07:49:58.352861+00	2025-12-08 07:49:58.352861+00	\N	\N	0	4	15	អនុក្រឹត្យលេខ ០៧ អនក្រ.បក	2008	f	\N	\N
6d3b58a9-38bb-4219-8fb4-c4785ed5e3f3	Prey Nob District	ស្រុកព្រៃនប់	1802	eb47ba73-ab32-4462-ad7a-a1156d28c7ec	district	\N	\N	\N	t	2025-12-08 07:49:58.352861+00	2025-12-08 07:49:58.352861+00	\N	\N	10	0	42	អនុក្រឹត្យលេខ ០៧ អនក្រ.បក	2008	f	\N	\N
d8d6eccf-9686-4f13-8018-929502573af9	Stueng Hav District	ស្រុកស្ទឹងហាវ	1803	eb47ba73-ab32-4462-ad7a-a1156d28c7ec	district	\N	\N	\N	t	2025-12-08 07:49:58.352861+00	2025-12-08 07:49:58.352861+00	\N	\N	4	0	13	អនុក្រឹត្យលេខ ០៧ អនក្រ.បក	2008	f	\N	\N
24adc8f0-16fd-4270-bcd2-57e76ca3f529	Kampong Seila District	ស្រុកកំពង់សីលា	1804	eb47ba73-ab32-4462-ad7a-a1156d28c7ec	district	\N	\N	\N	t	2025-12-08 07:49:58.352861+00	2025-12-08 07:49:58.352861+00	\N	\N	4	0	14	ព្រះរាជក្រឹត្យលេខ នស/រកត/១២០៨/១៣៨៥	2008	f	\N	\N
4a5bee7b-f0c8-4b6b-8151-d3e78417cd2f	Kaoh Rung Municipality	ក្រុងកោះរ៉ុង	1805	eb47ba73-ab32-4462-ad7a-a1156d28c7ec	municipality	\N	\N	\N	t	2025-12-08 07:49:58.352861+00	2025-12-08 07:49:58.352861+00	\N	\N	0	2	4	អនុក្រឹត្យលេខ០២អនក្រ.បក	2008	f	\N	\N
4667b6c1-44c4-4714-98c4-1f3f5072242e	Kampong Soam Municipality	ក្រុងកំពង់សោម	1806	eb47ba73-ab32-4462-ad7a-a1156d28c7ec	municipality	\N	\N	\N	t	2025-12-08 07:49:58.352861+00	2025-12-08 07:49:58.352861+00	\N	\N	0	5	23	អនុក្រឹត្យលេខ ២០១ អនក្រ.បក	2008	f	\N	\N
6f2e4476-41a5-49f1-8dc2-9330e519de46	Sesan District	ស្រុកសេសាន	1901	c353b702-6b0d-4df2-b763-e36c89e1a8d7	district	\N	\N	\N	t	2025-12-08 07:49:58.354086+00	2025-12-08 07:49:58.354086+00	\N	\N	7	0	22	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
16ae53e2-6773-49d4-961d-c8b4cca96bda	Siem Bouk District	ស្រុកសៀមបូក	1902	c353b702-6b0d-4df2-b763-e36c89e1a8d7	district	\N	\N	\N	t	2025-12-08 07:49:58.354086+00	2025-12-08 07:49:58.354086+00	\N	\N	7	0	17	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
cf7b310b-7454-438d-bfc5-7a3a215de898	Siem Pang District	ស្រុកសៀមប៉ាង	1903	c353b702-6b0d-4df2-b763-e36c89e1a8d7	district	\N	\N	\N	t	2025-12-08 07:49:58.354086+00	2025-12-08 07:49:58.354086+00	\N	\N	5	0	27	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
6fd95e1b-4335-465f-aebd-f3874b7729b0	Stueng Traeng Municipality	ក្រុងស្ទឹងត្រែង	1904	c353b702-6b0d-4df2-b763-e36c89e1a8d7	municipality	\N	\N	\N	t	2025-12-08 07:49:58.354086+00	2025-12-08 07:49:58.354086+00	\N	\N	0	4	22	អនុក្រឹត្យលេខ២២៤ អនក្រ.បក	2008	f	\N	\N
4b72cd10-ec17-4cda-9522-9c54f555d4cd	Thala Barivat District	ស្រុកថាឡាបរិវ៉ាត់	1905	c353b702-6b0d-4df2-b763-e36c89e1a8d7	district	\N	\N	\N	t	2025-12-08 07:49:58.354086+00	2025-12-08 07:49:58.354086+00	\N	\N	8	0	31	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
891f50ed-96ed-47d2-b285-b14f90c88c47	Borei Ou Svay Senchey District	ស្រុកបុរីអូរស្វាយសែនជ័យ	1906	c353b702-6b0d-4df2-b763-e36c89e1a8d7	district	\N	\N	\N	t	2025-12-08 07:49:58.354086+00	2025-12-08 07:49:58.354086+00	\N	\N	3	0	18	អនុក្រឹត្យលេខ០៦ អនក្រ.បក	2008	f	\N	\N
5e4258cb-62ab-405c-9340-7d3c012a9562	Chantrea District	ស្រុកចន្ទ្រា	2001	bea34eb3-bf88-4baa-b73f-77fd85aa597c	district	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	6	0	29	៤៩៣ ប្រ.ក	2008	f	\N	\N
414fb779-7fae-49c5-a30d-055593b253ed	Kampong Rou District	ស្រុកកំពង់រោទិ៍	2002	bea34eb3-bf88-4baa-b73f-77fd85aa597c	district	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	11	0	80	៤៩៣ ប្រ.ក	2008	f	\N	\N
80aa2c1e-8999-4ebc-a1d9-30470a17272c	Rumduol District	ស្រុករមាសហែក	2003	bea34eb3-bf88-4baa-b73f-77fd85aa597c	district	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	10	0	78	៤៩៣ ប្រ.ក	2008	f	\N	\N
d9818cf1-2f30-4295-b6f3-51c65e56377d	Romeas Haek District	ស្រុករមាសហែក	2004	bea34eb3-bf88-4baa-b73f-77fd85aa597c	district	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	16	0	204	៤៩៣ ប្រ.ក	2008	f	\N	\N
a56570fa-72c1-4c71-974b-9ccb6fc83752	Svay Chrum District	ស្រុកស្វាយជ្រំ	2005	bea34eb3-bf88-4baa-b73f-77fd85aa597c	district	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	16	0	158	តាមប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
e3f433a6-7c81-4fd0-94df-306382d8fb2a	Svay Rieng Municipality	ក្រុងស្វាយរៀង	2006	bea34eb3-bf88-4baa-b73f-77fd85aa597c	municipality	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	0	7	43	១២ អនក្រ.បក	2008	f	\N	\N
98c492d5-6442-4e8b-b344-1b8777e099fd	Svay Teab District	ស្រុកស្វាយទាប	2007	bea34eb3-bf88-4baa-b73f-77fd85aa597c	district	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	9	0	63	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
ea464e17-4d5c-48bd-abcc-aa857cd9b5c5	Bavet Municipality	ក្រុងបាវិត	2008	bea34eb3-bf88-4baa-b73f-77fd85aa597c	municipality	\N	\N	\N	t	2025-12-08 07:49:58.355212+00	2025-12-08 07:49:58.355212+00	\N	\N	0	5	35	២២៨ អនក្រ.បក	2008	f	\N	\N
cff0cfc1-2a21-4285-a877-c2dddd49e9e6	Angkor Borei District	ស្រុកអង្គរបូរី	2101	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	6	0	34	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
5fbc6a14-3780-4bab-9567-47680e9df972	Bati District	ស្រុកបាទី	2102	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	15	0	168	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
8d917079-5bab-468a-89c6-18eb11c014ae	Borei Cholsar District	ស្រុកបូរីជលសារ	2103	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	5	0	39	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
22d5d348-b1e0-4016-9e63-b31dddef0e55	Kiri Vong District	ស្រុកគីរីវង់	2104	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	12	0	115	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
b3604379-3189-45b6-a4d0-75bcd2ecd2c4	Kaoh Andaet District	ស្រុកកោះអណ្ដែត	2105	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	6	0	68	៤៩៣ ប្រ.ក	2008	f	\N	\N
30194b18-3ea1-48c2-b2f6-b248896bfed6	Prey Kabbas District	ស្រុកព្រៃកប្បាស	2106	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	13	0	112	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
734f605e-908d-4727-aa34-58f5c28c851d	Samraong District	ស្រុកសំរោង	2107	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	11	0	147	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
c54e3bed-a8f6-4d70-88e4-076630eb45c8	Doun Kaev Municipality	ក្រុងដូនកែវ	2108	c44e6e73-0cb5-4113-8fd6-5edbde182371	municipality	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	0	3	40	២២៦ អនក្រ.បក	2008	f	\N	\N
0fda74db-a69d-4b37-b7c7-4241f1edb25a	Tram Kak District	ស្រុកត្រាំកក់	2109	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	15	0	244	តាមប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
66fd6299-fab7-4f2a-aef1-9708375c097c	Treang District	ស្រុកទ្រាំង	2110	c44e6e73-0cb5-4113-8fd6-5edbde182371	district	\N	\N	\N	t	2025-12-08 07:49:58.356463+00	2025-12-08 07:49:58.356463+00	\N	\N	14	0	154	លេខ ៤៩៣ ប្រ.ក	2008	f	\N	\N
00d72752-c459-4dc9-a764-70dca2257d05	Anlong Veaeng District	ស្រុកអន្លង់វែង	2201	5d965f0c-ccb4-45b0-a142-1e20a4d8269b	district	\N	\N	\N	t	2025-12-08 07:49:58.357776+00	2025-12-08 07:49:58.357776+00	\N	\N	5	0	58	\N	\N	f	\N	\N
29fbdc14-c5f6-4686-8a51-0fce3c786b81	Banteay Ampil District	ស្រុកបន្ទាយអំពិល	2202	5d965f0c-ccb4-45b0-a142-1e20a4d8269b	district	\N	\N	\N	t	2025-12-08 07:49:58.357776+00	2025-12-08 07:49:58.357776+00	\N	\N	4	0	86	\N	\N	f	\N	\N
18fabd05-6995-463a-bf0a-7a5cbb320165	Chong Kal District	ស្រុកចុងកាល់	2203	5d965f0c-ccb4-45b0-a142-1e20a4d8269b	district	\N	\N	\N	t	2025-12-08 07:49:58.357776+00	2025-12-08 07:49:58.357776+00	\N	\N	4	0	35	\N	\N	f	\N	\N
8354e358-3770-4eff-9802-3d023f8886dc	Samraong Municipality	ក្រុងសំរោង	2204	5d965f0c-ccb4-45b0-a142-1e20a4d8269b	municipality	\N	\N	\N	t	2025-12-08 07:49:58.357776+00	2025-12-08 07:49:58.357776+00	\N	\N	0	5	76	\N	\N	f	\N	\N
b252d2a4-25f7-4671-baa5-20d9da068dfb	Trapeang Prasat District	ស្រុកត្រពាំងប្រាសាទ	2205	5d965f0c-ccb4-45b0-a142-1e20a4d8269b	district	\N	\N	\N	t	2025-12-08 07:49:58.357776+00	2025-12-08 07:49:58.357776+00	\N	\N	6	0	53	\N	\N	f	\N	\N
118e7e4d-fa50-414e-8b12-ccb44b76b631	Damnak Chang'aeur District	ស្រុកដំណាក់ចង្អើរ	2301	5bf02136-740f-4b20-a7f7-849cd0c08c70	district	\N	\N	\N	t	2025-12-08 07:49:58.358842+00	2025-12-08 07:49:58.358842+00	\N	\N	2	0	11	អនុក្រឹត្យលេខ ០៦ អនក្រ.បក	2008	f	\N	\N
b67c0aea-35ac-428f-aaed-27245cb368c3	Kaeb Municipality	ក្រុងកែប	2302	5bf02136-740f-4b20-a7f7-849cd0c08c70	municipality	\N	\N	\N	t	2025-12-08 07:49:58.358842+00	2025-12-08 07:49:58.358842+00	\N	\N	0	3	7	អនុក្រឹត្យលេខ ០៦​ អនក្រ.បក	2008	f	\N	\N
248bb9e0-7763-4250-9986-b7677e47cca7	Pailin Municipality	ក្រុងប៉ៃលិន	2401	6fe6eceb-7fd4-4c89-8bb1-e0ae60b2bfd7	municipality	\N	\N	\N	t	2025-12-08 07:49:58.359771+00	2025-12-08 07:49:58.359771+00	\N	\N	0	4	41	លេខ 05​អនក្រុ.បក	2008	f	\N	\N
c48b4adc-b021-47cd-87a5-d6d70076dc1e	Sala Krau District	ស្រុកសាលាក្រៅ	2402	6fe6eceb-7fd4-4c89-8bb1-e0ae60b2bfd7	district	\N	\N	\N	t	2025-12-08 07:49:58.359771+00	2025-12-08 07:49:58.359771+00	\N	\N	4	0	51	០៥​អនក្រ.បក​	2008	f	\N	\N
11653f04-db71-4aeb-9ac3-33975ad51ee2	Dambae District	ស្រុកដំណាក់ចង្អើរ	2501	0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	district	\N	\N	\N	t	2025-12-08 07:49:58.360878+00	2025-12-08 07:49:58.360878+00	\N	\N	7	0	83	Royal Degree 1445	2013	f	\N	\N
fc86a55d-d1a4-4771-a33b-0356337e8eec	Krouch Chhmar District	ស្រុកក្រូចឆ្មារ	2502	0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	district	\N	\N	\N	t	2025-12-08 07:49:58.360878+00	2025-12-08 07:49:58.360878+00	\N	\N	12	0	77	Royal Degree 1445	2013	f	\N	\N
1d63d10b-6ec5-4f01-a9ba-77fd0ee3eae7	Memot District	ស្រុកមេមត់	2503	0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	district	\N	\N	\N	t	2025-12-08 07:49:58.360878+00	2025-12-08 07:49:58.360878+00	\N	\N	14	0	182	Royal Degree 1445	2013	f	\N	\N
93d2b647-6cd2-4fb7-83b8-c118e31cefa7	Ou Reang Ov District	ស្រុកអូររាំងឪ	2504	0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	district	\N	\N	\N	t	2025-12-08 07:49:58.360878+00	2025-12-08 07:49:58.360878+00	\N	\N	7	0	142	Royal Degree 1445	2013	f	\N	\N
1a45f847-087a-4cdb-a5ee-69edafaf8d5d	Ponhea Kraek District	ស្រុកពញាក្រែក	2505	0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	district	\N	\N	\N	t	2025-12-08 07:49:58.360878+00	2025-12-08 07:49:58.360878+00	\N	\N	8	0	150	Royal Degree 1445	2013	f	\N	\N
ea8bf4f0-50f3-4558-ae3b-17a3c21c3708	Suong Municipality	ក្រុងសួង	2506	0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	municipality	\N	\N	\N	t	2025-12-08 07:49:58.360878+00	2025-12-08 07:49:58.360878+00	\N	\N	0	2	30	ព្រះរាជក្រិត្យលេខ ១៤៤៥	2013	f	\N	\N
beb826ec-e185-472f-a468-fb27d13dfe27	Tboung Khmum District	ស្រុកត្បូងឃ្មុំ	2507	0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	district	\N	\N	\N	t	2025-12-08 07:49:58.360878+00	2025-12-08 07:49:58.360878+00	\N	\N	14	0	211	Royal Degree 1445	2013	f	\N	\N
\.


--
-- Data for Name: employees; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.employees (id, active, created_at, display_name, first_name, last_name, phone_e164, updated_at, user_role, company_id, user_id) FROM stdin;
cc061eeb-e8dd-448b-9e25-cdf2712db4c7	t	2025-12-09 11:13:08.955551+00	Bling Fiary	Sovanmoney	SOK	+85589504405	2025-12-09 11:13:08.955588+00	OWNER	ed76b4f6-16d2-45f0-96cb-d1b90a6b6f93	2a8338ab-95e3-4b76-a357-784c4f5c0345
\.


--
-- Data for Name: exchange_rates; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.exchange_rates (id, from_currency, to_currency, rate, effective_date, is_active, notes, created_at, updated_at, deleted_at, created_by, is_deleted, deleted_by, updated_by, company_id) FROM stdin;
22cdb99d-8932-493d-849b-f596e09900f6	USD	KHR	4000.0000	2025-12-03 07:57:09.547893+00	t	Default exchange rate: 1 USD = 4000 KHR	2025-12-03 07:57:09.547893+00	2025-12-03 07:57:09.547893+00	\N	\N	f	\N	\N	\N
\.


--
-- Data for Name: images; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.images (id, url, uploader_id, company_id, created_at) FROM stdin;
\.


--
-- Data for Name: otp_attempts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.otp_attempts (id, chat_id, code_hash, created_at, expires_at, link_code, max_tries, phonee164, status, tries_count, updated_at) FROM stdin;
3c8b70ee-618d-4527-bc2f-9914f628da35	\N	\N	2025-12-08 09:33:53.344202+00	2025-12-08 09:43:53.310625+00	lczj9-JZ-v6fjzAX	5	+85589504405	PENDING	0	2025-12-08 09:33:53.344202+00
35ced305-1506-4abb-8049-e68fd0c1cb5a	\N	\N	2025-12-08 09:44:23.18313+00	2025-12-08 09:54:23.180624+00	JlwgcxIcL-ySUl6k	5	+85589504405	PENDING	0	2025-12-08 09:44:23.18313+00
bb9e3bd8-c7e0-4b06-8fdd-2ff24eb949e3	230752453	774fad6ea60793ca901074dac6b8051cc432a9bfcec48348461ff3b3b438dcf1	2025-12-08 09:45:10.332932+00	2025-12-08 09:51:11.944627+00	3sqHAXZ9hQt9E5af	5	+85589504405	SENT	0	2025-12-08 09:46:12.373306+00
d2da6190-6ae5-4c54-b2b7-1b9ec687b1d8	\N	\N	2025-12-08 09:46:24.889925+00	2025-12-08 09:56:24.889584+00	jBxxieENhqqCVnXC	5	+85589504405	PENDING	0	2025-12-08 09:46:24.889925+00
ea75ad29-29b7-46e2-bf49-61bffaf25a28	\N	\N	2025-12-08 09:55:57.472506+00	2025-12-08 10:05:57.464861+00	cPQCejz0pkONuEYa	5	+85589504405	PENDING	0	2025-12-08 09:55:57.472506+00
5ea822e3-389d-472b-ae00-7c71b2b8a7d3	230752453	87ce9a3bf4689231bcf6c66fd1d27a9342c8caa34b25fe1dad74443c8e2dd104	2025-12-08 10:37:36.058784+00	2025-12-08 10:42:46.451911+00	9DJKg64p1LRS7Yts	5	+85589504405	VERIFIED	1	2025-12-08 10:38:11.55683+00
eebea270-cebb-4ee6-8195-5128c259bca7	230752453	343528bda474656052862cd152b825d3a12110f2052c4756c1200865cddf5330	2025-12-08 10:38:57.746577+00	2025-12-08 10:43:57.750959+00	mS40WWOunrYVK44c	5	+85589504405	SENT	0	2025-12-08 10:38:57.751368+00
232f1544-7350-45c2-8d50-48f69c43b183	230752453	828c6d3bb39630e8b50b3d8d28ec41f926ce91282474718ec3c71a6ac2f103a9	2025-12-09 07:13:42.806354+00	2025-12-09 07:18:42.830552+00	wHDr2ZuAimZ3cHQj	5	+85589504405	VERIFIED	1	2025-12-09 07:14:11.295246+00
1381a8cf-4d16-424d-8d93-3d6a16ec5ad1	230752453	abe8d41489484243c5f077024a0bcd6e46f88ad4001a539fd757092e3e900e9b	2025-12-09 07:19:08.259872+00	2025-12-09 07:24:08.266617+00	WKB1uGC0VKXpr6JQ	5	+85589504405	VERIFIED	1	2025-12-09 07:19:42.225892+00
f795da5a-9f47-443a-a292-d8df6a222774	230752453	4ce6bd7fd0daf9e5081d17d869871d2253f0028558a033edbfb5bac4cb713e9a	2025-12-09 11:07:52.088975+00	2025-12-09 11:12:52.155599+00	nWLj_sVmgGSlyRlE	5	+85589504405	VERIFIED	1	2025-12-09 11:07:58.820309+00
adca89be-5add-40e0-8f95-c97ab4a9b5ea	230752453	d2ea2028582c9ceaba4bf12ac6e53108212e7ed9d5b6742637616915a44f2df3	2025-12-10 01:59:10.000422+00	2025-12-10 02:04:10.019198+00	PLz0Aoa9SmOWoEzP	5	+85589504405	VERIFIED	1	2025-12-10 01:59:17.793321+00
0040d5ab-4f0d-4b29-aea6-fb5e069ae636	230752453	\N	2025-12-10 02:01:09.69856+00	2025-12-10 02:11:09.697753+00	hgaWcesyXSjdAloE	5	+85569862999	BLOCKED	0	2025-12-10 02:01:26.479111+00
6df39713-d966-474b-8393-553c17f48316	230752453	4d73749b69d8083bbc3314e7cc416ea7872a07c0171bb6d697d7f03bccd178e3	2025-12-10 03:03:31.913912+00	2025-12-10 03:08:31.935275+00	57T72PQlrKgPCBX9	5	+85589504405	VERIFIED	1	2025-12-10 03:03:40.2263+00
\.


--
-- Data for Name: pending_employees; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.pending_employees (id, created_at, expires_at, phonee164, role, updated_at, company_id) FROM stdin;
\.


--
-- Data for Name: product_categories; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_categories (id, created_at, created_by, is_deleted, deleted_at, deleted_by, updated_at, updated_by, code, is_active, khmer_name, name, sort_order) FROM stdin;
d83bee9e-a1d3-4836-8b5c-82dafa77565f	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	ELECTRONICS	t	អេឡិចត្រូនិច	Electronics	1
2b31c929-119d-41b6-8ab4-d21c5d4a4451	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	CLOTHING	t	សម្លៀកបំពាក់	Clothing	2
ace2e987-25f5-4419-b288-26f12939c6d8	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	FOOD	t	អាហារ	Food	3
c8c92a6a-6ac9-45fa-accc-8996443311b0	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	BOOKS	t	សៀវភៅ	Books	4
9fac392d-e06a-477d-b251-3d0d1b3ecdd1	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	COSMETICS	t	គ្រឿងសម្អាង	Cosmetics	5
ada64f7e-7081-46a0-a835-5111f5ea490e	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	MEDICINE	t	ឱសថ	Medicine	6
df8a58e9-0550-4a80-931e-be95d0d070a9	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	DOCUMENTS	t	ឯកសារ	Documents	7
7e5a72ad-fc9f-48e6-9ab3-fb29047d304c	2025-12-08 07:58:58.316167+00	\N	f	\N	\N	\N	\N	OTHER	t	ផ្សេងៗ	Other	99
\.


--
-- Data for Name: product_images; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_images (id, photo_index, image_id, product_id) FROM stdin;
\.


--
-- Data for Name: product_photos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_photos (product_id, photo_url, photo_index, id) FROM stdin;
\.


--
-- Data for Name: product_product_photos; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_product_photos (product_id, product_photos) FROM stdin;
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.products (id, created_at, created_by, is_deleted, deleted_at, deleted_by, updated_at, updated_by, default_price, description, dimensions, is_active, last_used_at, name, usage_count, weight_kg, category_id, company_id, buying_price, selling_price, is_published, last_sell_price) FROM stdin;
\.


--
-- Data for Name: provinces; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.provinces (id, name, name_kh, code, capital, area_km2, population, districts_krong, districts_srok, districts_khan, communes_commune, communes_sangkat, total_villages, reference_number, reference_year, is_active, created_at, updated_at, created_by, updated_by, is_deleted, deleted_at, deleted_by, total_communes, total_districts) FROM stdin;
d78e226c-0c0d-4bd0-ac71-c32adf7c12ae	Banteay Meanchey Province	ខេត្តបន្ទាយមានជ័យ	01	\N	\N	\N	2	7	0	55	12	666	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
68419730-25e6-4bb3-8e00-1531618a21ea	Battambang Province	ខេត្តបាត់ដំបង	02	\N	\N	\N	1	13	0	93	10	844	លេខ​៤៩៣ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
13fb5678-6174-49ac-b32d-3d34cb9a7ed6	Kampong Cham Province	ខេត្តកំពង់ចាម	03	\N	\N	\N	1	9	0	105	4	947	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
fe0abc72-3c57-47d4-b45a-d8bc347470e2	Kampong Chhnang Province	ខេត្តកំពង់ឆ្នាំង	04	\N	\N	\N	1	7	0	67	4	569	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
2adc9b60-824a-4b94-b6f7-5b7df6e784bf	Kampong Speu Province	ខេត្តកំពង់ស្ពឺ	05	\N	\N	\N	2	7	0	78	10	1365	លេខ​៤៩៣​ប្រ,ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
2b424981-183e-4bc4-8249-ed001c475882	Kampong Thom Province	ខេត្តកំពង់ធំ	06	\N	\N	\N	1	8	0	73	8	765	ប្រកាសលេខ ៤៩៣​ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
ff2a7afc-d50f-4b4a-beef-eefaaf1e77ac	Kampot Province	ខេត្តកំពត	07	\N	\N	\N	2	7	0	85	8	491	ប្រកាសលេខ ៤៩៣ ​ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
51e91af6-ee6e-40c2-bfcf-03b654a4800b	Kandal Province	ខេត្តកណ្ដាល	08	\N	\N	\N	3	10	0	101	26	1010	ប្រកាសលេខ ៤៩៣​ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
88b07a64-f996-4bf2-b6d0-1d776c23b696	Koh Kong Province	ខេត្តកោះកុង	09	\N	\N	\N	1	6	0	26	3	119	ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
e522d87d-cac0-4100-9c7a-556b3ca24022	Kratie Province	ខេត្តក្រចេះ	10	\N	\N	\N	1	6	0	43	5	327	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
d2461c0e-a6d7-41a1-b35e-57f9fb03d8d0	Mondul Kiri Province	ខេត្តមណ្ឌលគិរី	11	\N	\N	\N	1	4	0	17	4	92	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
d17cd5db-c2bd-4847-a439-d239bb8aa615	Phnom Penh Capital	រាជធានីភ្នំពេញ	12	\N	\N	\N	0	0	14	0	105	953	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
9a354187-b19e-41e5-aa6d-36baa100d1a0	Preah Vihear Province	ខេត្តព្រះវិហារ	13	\N	\N	\N	1	7	0	49	2	232	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
7b15297f-aed7-48b1-b211-8b49f8e2eb3b	Prey Veng Province	ខេត្តព្រៃវែង	14	\N	\N	\N	1	12	0	112	4	1168	ប្រកាសលេខ ៤៩៣​ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
bf2b3873-853d-4f72-82ef-f296b3624db1	Pursat Province	ខេត្តពោធិ៍សាត់	15	\N	\N	\N	1	6	0	42	7	526	លេខ​៤៩៣​ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
0cd6409c-71a4-478d-92ea-7cfc0605ea4f	Ratanak Kiri Province	ខេត្តរតនគិរី	16	\N	\N	\N	1	8	0	46	4	243	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
3b419502-f9fa-4a36-9fac-f3302091e6d0	Siemreap Province	ខេត្តសៀមរាប	17	\N	\N	\N	2	11	0	86	14	909	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
eb47ba73-ab32-4462-ad7a-a1156d28c7ec	Preah Sihanouk Province	ខេត្តព្រះសីហនុ	18	\N	\N	\N	3	3	0	18	11	111	ព្រះរាជក្រឹត្យលេខ នស/រកត/១២០៨/១៣៨៥	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
c353b702-6b0d-4df2-b763-e36c89e1a8d7	Stung Treng Province	ខេត្តស្ទឹងត្រែង	19	\N	\N	\N	1	5	0	30	4	137	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
bea34eb3-bf88-4baa-b73f-77fd85aa597c	Svay Rieng Province	ខេត្តស្វាយរៀង	20	\N	\N	\N	2	6	0	68	12	690	ប្រកាសលេខ ៤៩៣​ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
c44e6e73-0cb5-4113-8fd6-5edbde182371	Takeo Province	ខេត្តតាកែវ	21	\N	\N	\N	1	9	0	97	3	1121	ប្រកាសលេខ ៤៩៣​ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
5d965f0c-ccb4-45b0-a142-1e20a4d8269b	Oddar Meanchey Province	ខេត្តឧត្ដរមានជ័យ	22	\N	\N	\N	1	4	0	19	5	308	ប្រកាសលេខ ៤៩៣ ប្រ.ក	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
5bf02136-740f-4b20-a7f7-849cd0c08c70	Kep Province	ខេត្តកែប	23	\N	\N	\N	1	1	0	2	3	18	ព្រះរាជក្រឹត្យលេខ នស/រកត/១២០៨/១៣៨៣	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
6fe6eceb-7fd4-4c89-8bb1-e0ae60b2bfd7	Pailin Province	ខេត្តប៉ៃលិន	24	\N	\N	\N	1	1	0	4	4	92	នស/រកម/1208/1384​	2008	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
0ac94daf-2aaf-4d9f-9ac7-f7e463a4cfee	Tboung Khmum Province	ខេត្តត្បូងឃ្មុំ	25	\N	\N	\N	1	6	0	62	2	875	ព្រះរាជក្រឹក្យលេខ នស/រកត/១២១៣/១៤៤៥	2013	t	2025-12-08 07:49:58.325366+00	2025-12-08 07:49:58.325366+00	\N	\N	f	\N	\N	\N	\N
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.refresh_tokens (id, created_at, device_info, expires_at, ip_address, token_hash, user_id) FROM stdin;
f616ea01-13db-444b-bb4f-29aa39b53f3c	2025-12-03 08:20:11.186068+00	u_+85589504405	2025-12-10 08:20:11.174655+00	local-login	3b0f6bf2f26f5c83faf9967db8145dbcdd16d0e67e4d07e866ee3ca8dfe688f4	f642b7b6-356a-4ebb-9a1f-b152419351ad
b27c6dbf-8dcb-4abc-846d-3a2bbfc2967f	2025-12-03 08:31:42.868385+00	u_+85589504405	2025-12-10 08:31:42.859881+00	local-login	ff083c74ee55cbe1e1349e5c8dd1e498968bc3c2e3ace51633c08e0426c98682	f642b7b6-356a-4ebb-9a1f-b152419351ad
64dc71e7-d679-44b6-80e9-c77af6d27899	2025-12-03 08:34:09.22566+00	u_+85589504405	2025-12-10 08:34:09.221089+00	local-login	cd519086817b5c44e2a64e871da1fdadb5c35f4ec5e9f3ee35a76bdfef629bd8	f642b7b6-356a-4ebb-9a1f-b152419351ad
2494a4e2-f5f1-4082-97bd-80689734faf5	2025-12-03 10:06:03.795928+00	u_+85589504405	2025-12-10 10:06:03.774037+00	local-login	1c78bece4c1aa9f60ad1b86534bbb6eb8b7940c1221ce4f2ef11fc6f9086049e	f642b7b6-356a-4ebb-9a1f-b152419351ad
97a5ab22-5f14-4477-82d5-ad422006945a	2025-12-03 10:09:57.944877+00	u_+85589504405	2025-12-10 10:09:57.940175+00	local-login	5cc33967dccb0dd480af6eea467d715bf59b1d6367d6262ee58626c79ac3a470	f642b7b6-356a-4ebb-9a1f-b152419351ad
332bf351-5f5b-491e-acfd-b1d25916d33e	2025-12-03 07:27:39.435218+00	u_+85589504405	2025-12-10 07:27:39.426641+00	local-login	82e9afe3a2450206be4a299c54a9912ff38850dfd5a0584a82f294a3b35878b3	f642b7b6-356a-4ebb-9a1f-b152419351ad
55105f3b-80af-4729-ad6f-0885b96bf9b0	2025-12-03 08:35:19.146711+00	u_+85589504405	2025-12-10 08:35:19.141464+00	local-login	0415e4e131fd6cfe5089d4be856a7d397f6bb137c24f39a20e0f19ab2b71c8f0	f642b7b6-356a-4ebb-9a1f-b152419351ad
ac7fd5b6-b788-4066-b9fb-5ff3aec80408	2025-12-03 08:50:54.431429+00	u_+85589504405	2025-12-10 08:50:54.422256+00	local-login	4772ae900c2d805911ddd4f0184fed013e58f2604ab51f5a9e85792c2030559c	f642b7b6-356a-4ebb-9a1f-b152419351ad
5b598b1f-61a8-4e17-9c23-d3e3d657bd4e	2025-12-03 10:38:43.946511+00	u_+85589504405	2025-12-10 10:38:43.875478+00	local-login	fa0b792788fc346f15b7b13973bae679221d05cda56852d8975b3bfd09e213a8	f642b7b6-356a-4ebb-9a1f-b152419351ad
c6bb00b2-50d9-448e-ab60-b835770f2cd6	2025-12-04 01:59:07.041676+00	u_+85589504405	2025-12-11 01:59:07.038238+00	local-login	7fa739cd89c7f9d858b87b5267dfee7b6230b20a4c9c0d432bac7fa9920be982	f642b7b6-356a-4ebb-9a1f-b152419351ad
8e0d6244-213f-4866-bfde-a6bfb2957bb2	2025-12-04 04:40:21.176183+00	u_+85589504405	2025-12-11 04:40:21.171597+00	local-login	02a7e4a7e80eac7159c00d60083b34d269a7dc84b93893a8b2dc854cb2a67400	f642b7b6-356a-4ebb-9a1f-b152419351ad
077fd83e-dfd8-43e6-a1ce-6cde51b38d83	2025-12-04 08:00:01.534861+00	u_+85589504405	2025-12-11 08:00:01.523362+00	local-login	60d09ac932f7657a861038243b0c233b0b1e1e021097c47837bc0493ef46c928	f642b7b6-356a-4ebb-9a1f-b152419351ad
c3d7802c-4863-4f6a-8e61-93b713b04cc1	2025-12-04 08:00:06.205563+00	u_+85589504405	2025-12-11 08:00:06.204142+00	local-login	97c1a70b80aad71454e3205e86485ff0d6129f1eac02243ba6fc949105afcbd6	f642b7b6-356a-4ebb-9a1f-b152419351ad
922901a9-9cf9-4b3f-ab6f-7cf00ebb93d6	2025-12-04 08:02:30.773339+00	u_+85589504405	2025-12-11 08:02:30.771618+00	local-login	941868abf988de145b4d7132d472cda6a53236eec09d7e2394eda16a16f815e0	f642b7b6-356a-4ebb-9a1f-b152419351ad
fedf44c7-5888-48e5-82ca-b2c5f0dad361	2025-12-05 05:07:40.329456+00	u_+85589504405	2025-12-12 05:07:40.323985+00	local-login	faa86299b0d71230e1b3fec1b0656b5aeacfa55284eba9edddc32baf7a2d414e	f642b7b6-356a-4ebb-9a1f-b152419351ad
66f48ee4-c10e-4822-876a-451827fe3430	2025-12-03 07:38:39.940422+00	u_+85589504405	2025-12-10 07:38:39.93786+00	local-login	b7a8125e85837e6f6de0f477d1b1b43361a8dec06bc63ee9b03d4788b2cc39a2	f642b7b6-356a-4ebb-9a1f-b152419351ad
b5b7e289-2a3d-476c-a0b1-4f85f064c806	2025-12-03 09:33:07.047594+00	u_+85589504405	2025-12-10 09:33:07.03432+00	local-login	b83b2fc80371d85dcc4101a87a46f457f3bba47d5cf293faa3eec3e479854768	f642b7b6-356a-4ebb-9a1f-b152419351ad
1622b1ff-1047-453f-9bc7-f6246761b069	2025-12-04 03:02:25.710348+00	u_+85589504405	2025-12-11 03:02:25.705073+00	local-login	016ab7e7f4574a9b867a8b9a766366a8b959550be5f1906eb5b4aa38daf451df	f642b7b6-356a-4ebb-9a1f-b152419351ad
88550025-1db6-45ad-8475-b8d9e869201b	2025-12-04 04:51:26.497618+00	u_+85589504405	2025-12-11 04:51:26.487234+00	local-login	68b67e820c9285f107d059c7ecd1ca95faa76fe20781696959c3ac14c23b1417	f642b7b6-356a-4ebb-9a1f-b152419351ad
17b09c25-ede2-4b19-a2a3-1c2c6c78b2ea	2025-12-04 08:41:14.325974+00	u_+85589504405	2025-12-11 08:41:14.260081+00	local-login	72398fca98a484ffa9842a4afe70b28cffdc5f24cb1b8302458cd939a5b5c210	f642b7b6-356a-4ebb-9a1f-b152419351ad
\.


--
-- Data for Name: token_blacklist; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.token_blacklist (id, created_at, expires_at, reason, token_hash, user_id) FROM stdin;
\.


--
-- Data for Name: user_audits; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_audits (id, created_at, field_name, new_value, old_value, source, user_id) FROM stdin;
0a31148d-78dd-4934-ad37-b77601dbaf98	2025-12-09 11:08:58.047226+00	userType	COMPANY	\N	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
9c13d43e-9f2f-4141-8a6d-aee29e73b79b	2025-12-09 11:08:58.050778+00	firstName	សុវណ្ណមុណីយ	\N	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
612a1667-c1d4-46a8-ab9a-e8020ec08b76	2025-12-09 11:08:58.051995+00	lastName	សុខ	\N	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
cf1a422d-82a1-40e2-8385-6331e4c8c653	2025-12-09 11:13:08.959703+00	userType	ADMIN	COMPANY	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
c94b2a18-07b4-445c-a4d2-e78541cdfce0	2025-12-09 11:13:08.960861+00	firstName	Sovanmoney	សុវណ្ណមុណីយ	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
a252d31a-9f1b-40b6-88b4-3be310e3ad45	2025-12-09 11:13:08.963925+00	lastName	SOK	សុខ	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
830de4b5-552f-4102-be65-85357d58ee1f	2025-12-09 11:13:08.964521+00	displayName	Bling Fiary	\N	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
e02f94aa-a256-4863-9855-d44d8fda72b2	2025-12-09 11:13:08.964978+00	companyName	Bling Jewelry	\N	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
20ac4d48-d7d1-4e0e-ad44-859da14ce841	2025-12-09 11:13:08.965553+00	avatarUrl	/uploads/images/b3be9b4c-b779-4004-9477-2dc1f6118137.jpg	\N	PROFILE_UPDATE	2a8338ab-95e3-4b76-a357-784c4f5c0345
\.


--
-- Data for Name: user_phones; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_phones (id, created_at, phone_e164, is_primary, updated_at, verified_at, user_id) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, created_at, display_name, email, email_verified_at, phone_e164, phone_verified_at, updated_at, active, avatar_url, first_name, last_login_at, last_name, username, company_name, user_type, company_id, user_role, is_incomplete, address, district, province, default_address, default_province_id, default_district_id) FROM stdin;
44346f44-1dd1-4360-bd8e-7b6c92bf8025	2025-12-08 08:06:47.861848+00	System Administrator	\N	\N	\N	\N	2025-12-08 08:06:47.861864+00	t	\N	System	2025-12-08 08:06:47.791775+00	Administrator	admin	\N	ADMIN	6675c21f-5cc2-44fa-8799-9787c58b2d7e	SYSTEM_ADMINISTRATOR	f	\N	\N	\N	\N	\N	\N
6ea4a674-52f7-460e-9989-ec58d54b70da	2025-12-08 09:54:06.665071+00	Test User	\N	\N	\N	\N	2025-12-08 09:54:06.665094+00	t	\N	\N	2025-12-08 09:54:06.635678+00	\N	testuser-00000000	\N	\N	43a072a6-90cc-422e-a47a-fe9ff64c9b3d	\N	f	\N	\N	\N	\N	\N	\N
2a8338ab-95e3-4b76-a357-784c4f5c0345	2025-12-08 10:38:11.557086+00	Bling Fiary	\N	\N	+85589504405	\N	2025-12-09 11:13:08.96614+00	t	/uploads/images/b3be9b4c-b779-4004-9477-2dc1f6118137.jpg	Sovanmoney	\N	SOK	u_+85589504405	\N	ADMIN	ed76b4f6-16d2-45f0-96cb-d1b90a6b6f93	OWNER	f	\N	\N	\N	\N	\N	\N
\.


--
-- Name: company_invitations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.company_invitations_id_seq', 5, true);


--
-- Name: auth_identities auth_identities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auth_identities
    ADD CONSTRAINT auth_identities_pkey PRIMARY KEY (id);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: company_categories company_categories_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_categories
    ADD CONSTRAINT company_categories_code_key UNIQUE (code);


--
-- Name: company_categories company_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_categories
    ADD CONSTRAINT company_categories_pkey PRIMARY KEY (id);


--
-- Name: company_invitations company_invitations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_invitations
    ADD CONSTRAINT company_invitations_pkey PRIMARY KEY (id);


--
-- Name: delivery_items delivery_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_items
    ADD CONSTRAINT delivery_items_pkey PRIMARY KEY (id);


--
-- Name: delivery_packages delivery_packages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_packages
    ADD CONSTRAINT delivery_packages_pkey PRIMARY KEY (id);


--
-- Name: delivery_photos delivery_photos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_photos
    ADD CONSTRAINT delivery_photos_pkey PRIMARY KEY (id);


--
-- Name: delivery_pricing_rules delivery_pricing_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_pricing_rules
    ADD CONSTRAINT delivery_pricing_rules_pkey PRIMARY KEY (id);


--
-- Name: delivery_tracking delivery_tracking_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_tracking
    ADD CONSTRAINT delivery_tracking_pkey PRIMARY KEY (id);


--
-- Name: districts districts_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT districts_code_key UNIQUE (code);


--
-- Name: districts districts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT districts_pkey PRIMARY KEY (id);


--
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);


--
-- Name: exchange_rates exchange_rates_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exchange_rates
    ADD CONSTRAINT exchange_rates_pkey PRIMARY KEY (id);


--
-- Name: otp_attempts idx_otp_link_code; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.otp_attempts
    ADD CONSTRAINT idx_otp_link_code UNIQUE (link_code);


--
-- Name: pending_employees idx_pending_phone_company; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pending_employees
    ADD CONSTRAINT idx_pending_phone_company UNIQUE (phonee164, company_id);


--
-- Name: product_categories idx_product_categories_code; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT idx_product_categories_code UNIQUE (code);


--
-- Name: images images_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT images_pkey PRIMARY KEY (id);


--
-- Name: otp_attempts otp_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.otp_attempts
    ADD CONSTRAINT otp_attempts_pkey PRIMARY KEY (id);


--
-- Name: pending_employees pending_employees_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pending_employees
    ADD CONSTRAINT pending_employees_pkey PRIMARY KEY (id);


--
-- Name: product_categories product_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT product_categories_pkey PRIMARY KEY (id);


--
-- Name: product_images product_images_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT product_images_pkey PRIMARY KEY (id);


--
-- Name: product_photos product_photos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_photos
    ADD CONSTRAINT product_photos_pkey PRIMARY KEY (product_id, photo_index);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: provinces provinces_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_code_key UNIQUE (code);


--
-- Name: provinces provinces_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: token_blacklist token_blacklist_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.token_blacklist
    ADD CONSTRAINT token_blacklist_pkey PRIMARY KEY (id);


--
-- Name: companies uk_companies_name_created_by; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT uk_companies_name_created_by UNIQUE (name, created_by_company_id);


--
-- Name: company_invitations uk_dtgllognvk2yjweca974oyp1c; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_invitations
    ADD CONSTRAINT uk_dtgllognvk2yjweca974oyp1c UNIQUE (invitation_code);


--
-- Name: employees uk_employees_user_company; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT uk_employees_user_company UNIQUE (user_id, company_id);


--
-- Name: users uk_mwbivwxwi6dylel3gd5vg1q8; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_mwbivwxwi6dylel3gd5vg1q8 UNIQUE (phone_e164);


--
-- Name: auth_identities uk_provider_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auth_identities
    ADD CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id);


--
-- Name: otp_attempts uk_s925q1uxy4s8gky9hgoekrhhu; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.otp_attempts
    ADD CONSTRAINT uk_s925q1uxy4s8gky9hgoekrhhu UNIQUE (link_code);


--
-- Name: user_phones uk_user_phones_phone; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_phones
    ADD CONSTRAINT uk_user_phones_phone UNIQUE (phone_e164);


--
-- Name: user_phones uk_user_phones_user_phone; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_phones
    ADD CONSTRAINT uk_user_phones_user_phone UNIQUE (user_id, phone_e164);


--
-- Name: users uk_users_email; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_email UNIQUE (email);


--
-- Name: users uk_users_phone_e164; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_phone_e164 UNIQUE (phone_e164);


--
-- Name: users uk_users_username; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_username UNIQUE (username);


--
-- Name: refresh_tokens uko2mlirhldriil2y7krapq4frt; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uko2mlirhldriil2y7krapq4frt UNIQUE (token_hash);


--
-- Name: user_audits user_audits_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_audits
    ADD CONSTRAINT user_audits_pkey PRIMARY KEY (id);


--
-- Name: user_phones user_phones_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_phones
    ADD CONSTRAINT user_phones_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_companies_category_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_companies_category_id ON public.companies USING btree (category_id);


--
-- Name: idx_companies_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_companies_created_at ON public.companies USING btree (created_at);


--
-- Name: idx_companies_created_by_company_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_companies_created_by_company_id ON public.companies USING btree (created_by_company_id);


--
-- Name: idx_companies_created_by_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_companies_created_by_user_id ON public.companies USING btree (created_by_user_id);


--
-- Name: idx_companies_name_created_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_companies_name_created_by ON public.companies USING btree (name, created_by_company_id);


--
-- Name: idx_companies_province_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_companies_province_id ON public.companies USING btree (province_id);


--
-- Name: idx_companies_updated_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_companies_updated_at ON public.companies USING btree (updated_at);


--
-- Name: idx_company_categories_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_company_categories_code ON public.company_categories USING btree (code);


--
-- Name: idx_delivery_items_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_created_at ON public.delivery_items USING btree (created_at);


--
-- Name: idx_delivery_items_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_deleted_at ON public.delivery_items USING btree (deleted_at);


--
-- Name: idx_delivery_items_delivery_company_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_delivery_company_id ON public.delivery_items USING btree (delivery_company_id);


--
-- Name: idx_delivery_items_delivery_driver_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_delivery_driver_id ON public.delivery_items USING btree (delivery_driver_id);


--
-- Name: idx_delivery_items_receiver_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_receiver_id ON public.delivery_items USING btree (receiver_id);


--
-- Name: idx_delivery_items_sender_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_sender_id ON public.delivery_items USING btree (sender_id);


--
-- Name: idx_delivery_items_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_status ON public.delivery_items USING btree (status);


--
-- Name: idx_delivery_items_updated_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_items_updated_at ON public.delivery_items USING btree (updated_at);


--
-- Name: idx_delivery_photos_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_photos_created_at ON public.delivery_photos USING btree (created_at);


--
-- Name: idx_delivery_photos_delivery_item_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_photos_delivery_item_id ON public.delivery_photos USING btree (delivery_item_id);


--
-- Name: idx_delivery_photos_sequence_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_photos_sequence_order ON public.delivery_photos USING btree (sequence_order);


--
-- Name: idx_delivery_photos_uploaded_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_photos_uploaded_at ON public.delivery_photos USING btree (uploaded_at);


--
-- Name: idx_delivery_tracking_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_tracking_created_at ON public.delivery_tracking USING btree (created_at);


--
-- Name: idx_delivery_tracking_delivery_item_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_tracking_delivery_item_id ON public.delivery_tracking USING btree (delivery_item_id);


--
-- Name: idx_delivery_tracking_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_tracking_status ON public.delivery_tracking USING btree (status);


--
-- Name: idx_delivery_tracking_timestamp; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_tracking_timestamp ON public.delivery_tracking USING btree ("timestamp");


--
-- Name: idx_delivery_tracking_updated_by; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_delivery_tracking_updated_by ON public.delivery_tracking USING btree (updated_by);


--
-- Name: idx_districts_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_districts_active ON public.districts USING btree (is_active) WHERE (is_active = true);


--
-- Name: idx_districts_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_districts_code ON public.districts USING btree (code);


--
-- Name: idx_districts_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_districts_name ON public.districts USING btree (name);


--
-- Name: idx_districts_province_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_districts_province_id ON public.districts USING btree (province_id);


--
-- Name: idx_districts_province_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_districts_province_name ON public.districts USING btree (province_id, name);


--
-- Name: idx_employees_company; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_employees_company ON public.employees USING btree (company_id);


--
-- Name: idx_employees_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_employees_created_at ON public.employees USING btree (created_at);


--
-- Name: idx_employees_updated_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_employees_updated_at ON public.employees USING btree (updated_at);


--
-- Name: idx_employees_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_employees_user ON public.employees USING btree (user_id);


--
-- Name: idx_exchange_rates_company_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exchange_rates_company_id ON public.exchange_rates USING btree (company_id);


--
-- Name: idx_exchange_rates_currencies; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exchange_rates_currencies ON public.exchange_rates USING btree (from_currency, to_currency);


--
-- Name: idx_exchange_rates_effective_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exchange_rates_effective_date ON public.exchange_rates USING btree (effective_date);


--
-- Name: idx_exchange_rates_from_to; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_exchange_rates_from_to ON public.exchange_rates USING btree (from_currency, to_currency);


--
-- Name: idx_expires_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_expires_at ON public.token_blacklist USING btree (expires_at);


--
-- Name: idx_otp_phone; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_otp_phone ON public.otp_attempts USING btree (phonee164);


--
-- Name: idx_pending_company; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pending_company ON public.pending_employees USING btree (company_id);


--
-- Name: idx_pending_expires; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pending_expires ON public.pending_employees USING btree (expires_at);


--
-- Name: idx_pricing_rules_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pricing_rules_active ON public.delivery_pricing_rules USING btree (is_active);


--
-- Name: idx_pricing_rules_company_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pricing_rules_company_id ON public.delivery_pricing_rules USING btree (company_id);


--
-- Name: idx_pricing_rules_district; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pricing_rules_district ON public.delivery_pricing_rules USING btree (district);


--
-- Name: idx_pricing_rules_province; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pricing_rules_province ON public.delivery_pricing_rules USING btree (province);


--
-- Name: idx_product_categories_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_product_categories_active ON public.product_categories USING btree (is_active);


--
-- Name: idx_product_categories_sort_order; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_product_categories_sort_order ON public.product_categories USING btree (sort_order);


--
-- Name: idx_products_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_active ON public.products USING btree (is_active);


--
-- Name: idx_products_category; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_category ON public.products USING btree (category_id);


--
-- Name: idx_products_company_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_company_id ON public.products USING btree (company_id);


--
-- Name: idx_products_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_created_at ON public.products USING btree (created_at);


--
-- Name: idx_provinces_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_provinces_active ON public.provinces USING btree (is_active) WHERE (is_active = true);


--
-- Name: idx_provinces_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_provinces_code ON public.provinces USING btree (code);


--
-- Name: idx_provinces_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_provinces_name ON public.provinces USING btree (name);


--
-- Name: idx_provinces_name_kh; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_provinces_name_kh ON public.provinces USING btree (name_kh);


--
-- Name: idx_refresh_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_refresh_token ON public.refresh_tokens USING btree (token_hash);


--
-- Name: idx_token_hash; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_token_hash ON public.token_blacklist USING btree (token_hash);


--
-- Name: idx_user_audits_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_audits_created_at ON public.user_audits USING btree (created_at);


--
-- Name: idx_user_audits_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_audits_user_id ON public.user_audits USING btree (user_id);


--
-- Name: idx_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_user_phones_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_phones_created_at ON public.user_phones USING btree (created_at);


--
-- Name: idx_user_phones_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_phones_user ON public.user_phones USING btree (user_id);


--
-- Name: idx_users_company_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_company_id ON public.users USING btree (company_id) WHERE ((user_type)::text = 'CUSTOMER'::text);


--
-- Name: idx_users_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_created_at ON public.users USING btree (created_at);


--
-- Name: idx_users_phone_company; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_phone_company ON public.users USING btree (phone_e164, company_id) WHERE ((user_type)::text = 'CUSTOMER'::text);


--
-- Name: idx_users_updated_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_updated_at ON public.users USING btree (updated_at);


--
-- Name: uk_exchange_rates_company_currencies_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uk_exchange_rates_company_currencies_active ON public.exchange_rates USING btree (company_id, from_currency, to_currency, is_active) WHERE ((is_active = true) AND (company_id IS NOT NULL));


--
-- Name: uk_users_phone_company; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uk_users_phone_company ON public.users USING btree (phone_e164, company_id) WHERE (((user_type)::text = 'CUSTOMER'::text) AND (company_id IS NOT NULL));


--
-- Name: districts districts_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT districts_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: districts districts_province_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT districts_province_id_fkey FOREIGN KEY (province_id) REFERENCES public.provinces(id) ON DELETE CASCADE;


--
-- Name: districts districts_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.districts
    ADD CONSTRAINT districts_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);


--
-- Name: employees fk1ekpcbo0lmdx6ou8e3fh9j4lq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT fk1ekpcbo0lmdx6ou8e3fh9j4lq FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: product_images fk1j9bvqvvdudsd1ydm4fr0y3bk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT fk1j9bvqvvdudsd1ydm4fr0y3bk FOREIGN KEY (image_id) REFERENCES public.images(id);


--
-- Name: delivery_photos fk54ceekwcmlts5gyh23sfforjr; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_photos
    ADD CONSTRAINT fk54ceekwcmlts5gyh23sfforjr FOREIGN KEY (delivery_item_id) REFERENCES public.delivery_items(id);


--
-- Name: employees fk69x3vjuy1t5p18a5llb8h2fjx; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT fk69x3vjuy1t5p18a5llb8h2fjx FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: delivery_items fk6dtpnk4q74a7st0tnhoiemwep; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_items
    ADD CONSTRAINT fk6dtpnk4q74a7st0tnhoiemwep FOREIGN KEY (receiver_id) REFERENCES public.users(id);


--
-- Name: products fk6t5dtw6tyo83ywljwohuc6g7k; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk6t5dtw6tyo83ywljwohuc6g7k FOREIGN KEY (category_id) REFERENCES public.product_categories(id);


--
-- Name: delivery_items fk9i2o03qjlo90u1igj1iha7bu7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_items
    ADD CONSTRAINT fk9i2o03qjlo90u1igj1iha7bu7 FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- Name: companies fk_companies_category; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk_companies_category FOREIGN KEY (category_id) REFERENCES public.company_categories(id);


--
-- Name: companies fk_companies_created_by_company; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk_companies_created_by_company FOREIGN KEY (created_by_company_id) REFERENCES public.companies(id);


--
-- Name: companies fk_companies_created_by_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk_companies_created_by_user FOREIGN KEY (created_by_user_id) REFERENCES public.users(id);


--
-- Name: companies fk_companies_province; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk_companies_province FOREIGN KEY (province_id) REFERENCES public.provinces(id);


--
-- Name: companies fk_companies_updated_by_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk_companies_updated_by_user FOREIGN KEY (updated_by_user_id) REFERENCES public.users(id);


--
-- Name: exchange_rates fk_exchange_rates_company; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.exchange_rates
    ADD CONSTRAINT fk_exchange_rates_company FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: images fk_images_company; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT fk_images_company FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: images fk_images_uploader; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.images
    ADD CONSTRAINT fk_images_uploader FOREIGN KEY (uploader_id) REFERENCES public.users(id);


--
-- Name: users fk_users_company; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_company FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: users fk_users_default_district; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_default_district FOREIGN KEY (default_district_id) REFERENCES public.districts(id);


--
-- Name: users fk_users_default_province; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_default_province FOREIGN KEY (default_province_id) REFERENCES public.provinces(id);


--
-- Name: delivery_items fkca2icl8eopjov1yxorugsr7pd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_items
    ADD CONSTRAINT fkca2icl8eopjov1yxorugsr7pd FOREIGN KEY (delivery_driver_id) REFERENCES public.users(id);


--
-- Name: delivery_items fkg7ux2x7j7tupm925smv3pq5p2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_items
    ADD CONSTRAINT fkg7ux2x7j7tupm925smv3pq5p2 FOREIGN KEY (delivery_company_id) REFERENCES public.companies(id);


--
-- Name: auth_identities fkh7r51yumnvaq9p7ay7oimaird; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.auth_identities
    ADD CONSTRAINT fkh7r51yumnvaq9p7ay7oimaird FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: users fkin8gn4o1hpiwe6qe4ey7ykwq7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkin8gn4o1hpiwe6qe4ey7ykwq7 FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: companies fkjd6bgkx2v2lt2p25cc7ol46h3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fkjd6bgkx2v2lt2p25cc7ol46h3 FOREIGN KEY (district_id) REFERENCES public.districts(id);


--
-- Name: product_product_photos fkjusv79jrkdishf3t5ayj7lo0y; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_product_photos
    ADD CONSTRAINT fkjusv79jrkdishf3t5ayj7lo0y FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: product_photos fkk6euo1c1uosxm44vy24qbw05j; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_photos
    ADD CONSTRAINT fkk6euo1c1uosxm44vy24qbw05j FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: delivery_tracking fklv8ypiru2igbvovp4glugiy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_tracking
    ADD CONSTRAINT fklv8ypiru2igbvovp4glugiy FOREIGN KEY (delivery_item_id) REFERENCES public.delivery_items(id);


--
-- Name: delivery_packages fklvo1b5ked2s50sa0w0dp5n0ps; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_packages
    ADD CONSTRAINT fklvo1b5ked2s50sa0w0dp5n0ps FOREIGN KEY (sender_id) REFERENCES public.users(id);


--
-- Name: user_phones fknup2o0u3x7dudj4ky81oiio13; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_phones
    ADD CONSTRAINT fknup2o0u3x7dudj4ky81oiio13 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: delivery_pricing_rules fkp2jq8uagc751tgt2ahgbjt24m; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_pricing_rules
    ADD CONSTRAINT fkp2jq8uagc751tgt2ahgbjt24m FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: company_invitations fkpvcecknxr1udxf7avngt1g26h; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company_invitations
    ADD CONSTRAINT fkpvcecknxr1udxf7avngt1g26h FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: product_images fkqnq71xsohugpqwf3c9gxmsuy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT fkqnq71xsohugpqwf3c9gxmsuy FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: delivery_items fkqvm3lh0lexb88fc5k46pfc62v; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_items
    ADD CONSTRAINT fkqvm3lh0lexb88fc5k46pfc62v FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: products fkr67nkbovcmogr3o5xkmfepgl1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkr67nkbovcmogr3o5xkmfepgl1 FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: pending_employees fkscbbdxfll29kro2f6ju2l6dvk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pending_employees
    ADD CONSTRAINT fkscbbdxfll29kro2f6ju2l6dvk FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: delivery_tracking fksn71fe617xpob27gm5d1iajc1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.delivery_tracking
    ADD CONSTRAINT fksn71fe617xpob27gm5d1iajc1 FOREIGN KEY (status_updated_by) REFERENCES public.users(id);


--
-- Name: provinces provinces_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id);


--
-- Name: provinces provinces_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.provinces
    ADD CONSTRAINT provinces_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict TuP8y0wkJnTBeQvJvK8A6aaNS4dxBNi0hZdSb0dXgsZLYE7qXeFV0rI6bRdBhgL

