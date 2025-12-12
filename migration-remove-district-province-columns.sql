-- Remove redundant district and province text columns from companies table
-- We already have district_id and province_id which are foreign keys

ALTER TABLE public.companies 
DROP COLUMN IF EXISTS district,
DROP COLUMN IF EXISTS province;
