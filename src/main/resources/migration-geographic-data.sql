-- Migration Script: Add provinces and districts tables
-- Run this to create geographic data structure for Cambodia

-- Create provinces table
CREATE TABLE IF NOT EXISTS provinces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    name_kh VARCHAR(100),
    code VARCHAR(10) NOT NULL UNIQUE,
    capital VARCHAR(100),
    area_km2 INTEGER,
    population INTEGER,
    districts_krong INTEGER DEFAULT 0,
    districts_srok INTEGER DEFAULT 0,
    districts_khan INTEGER DEFAULT 0,
    communes_commune INTEGER DEFAULT 0,
    communes_sangkat INTEGER DEFAULT 0,
    total_villages INTEGER DEFAULT 0,
    reference_number VARCHAR(50),
    reference_year INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Create districts table
CREATE TABLE IF NOT EXISTS districts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    name_kh VARCHAR(100),
    code VARCHAR(20) NOT NULL UNIQUE,
    province_id UUID NOT NULL REFERENCES provinces(id) ON DELETE CASCADE,
    type VARCHAR(50),
    area_km2 INTEGER,
    population INTEGER,
    postal_code VARCHAR(10),
    communes_commune INTEGER DEFAULT 0,
    communes_sangkat INTEGER DEFAULT 0,
    total_villages INTEGER DEFAULT 0,
    reference_number VARCHAR(50),
    reference_year INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_provinces_name ON provinces(name);
CREATE INDEX IF NOT EXISTS idx_provinces_code ON provinces(code);
CREATE INDEX IF NOT EXISTS idx_provinces_active ON provinces(is_active) WHERE is_active = true;

CREATE INDEX IF NOT EXISTS idx_districts_name ON districts(name);
CREATE INDEX IF NOT EXISTS idx_districts_code ON districts(code);
CREATE INDEX IF NOT EXISTS idx_districts_province_id ON districts(province_id);
CREATE INDEX IF NOT EXISTS idx_districts_province_name ON districts(province_id, name);
CREATE INDEX IF NOT EXISTS idx_districts_active ON districts(is_active) WHERE is_active = true;

-- Add new columns to districts table if they don't exist
ALTER TABLE districts ADD COLUMN IF NOT EXISTS communes_commune INTEGER DEFAULT 0;
ALTER TABLE districts ADD COLUMN IF NOT EXISTS communes_sangkat INTEGER DEFAULT 0;
ALTER TABLE districts ADD COLUMN IF NOT EXISTS total_villages INTEGER DEFAULT 0;
ALTER TABLE districts ADD COLUMN IF NOT EXISTS reference_number VARCHAR(50);
ALTER TABLE districts ADD COLUMN IF NOT EXISTS reference_year INTEGER;

-- Insert Cambodian provinces data
INSERT INTO provinces (name, name_kh, code, districts_krong, districts_srok, districts_khan, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Banteay Meanchey Province', 'ខេត្តបន្ទាយមានជ័យ', '01', 2, 7, 0, 55, 12, 666, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Battambang Province', 'ខេត្តបាត់ដំបង', '02', 1, 13, 0, 93, 10, 844, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Kampong Cham Province', 'ខេត្តកំពង់ចាម', '03', 1, 9, 0, 105, 4, 947, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kampong Chhnang Province', 'ខេត្តកំពង់ឆ្នាំង', '04', 1, 7, 0, 67, 4, 569, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kampong Speu Province', 'ខេត្តកំពង់ស្ពឺ', '05', 2, 7, 0, 78, 10, 1365, 'លេខ​៤៩៣​ប្រ,ក', 2008),
('Kampong Thom Province', 'ខេត្តកំពង់ធំ', '06', 1, 8, 0, 73, 8, 765, 'ប្រកាសលេខ ៤៩៣​ ប្រ.ក', 2008),
('Kampot Province', 'ខេត្តកំពត', '07', 2, 7, 0, 85, 8, 491, 'ប្រកាសលេខ ៤៩៣ ​ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Kandal Province', 'ខេត្តកណ្ដាល', '08', 3, 10, 0, 101, 26, 1010, 'ប្រកាសលេខ ៤៩៣​ ប្រ.ក', 2008),
('Koh Kong Province', 'ខេត្តកោះកុង', '09', 1, 6, 0, 26, 3, 119, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Kratie Province', 'ខេត្តក្រចេះ', '10', 1, 6, 0, 43, 5, 327, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Mondul Kiri Province', 'ខេត្តមណ្ឌលគិរី', '11', 1, 4, 0, 17, 4, 92, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Phnom Penh Capital', 'រាជធានីភ្នំពេញ', '12', 0, 0, 14, 0, 105, 953, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Preah Vihear Province', 'ខេត្តព្រះវិហារ', '13', 1, 7, 0, 49, 2, 232, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Prey Veng Province', 'ខេត្តព្រៃវែង', '14', 1, 12, 0, 112, 4, 1168, 'ប្រកាសលេខ ៤៩៣​ ប្រ.ក', 2008),
('Pursat Province', 'ខេត្តពោធិ៍សាត់', '15', 1, 6, 0, 42, 7, 526, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Ratanak Kiri Province', 'ខេត្តរតនគិរី', '16', 1, 8, 0, 46, 4, 243, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Siemreap Province', 'ខេត្តសៀមរាប', '17', 2, 11, 0, 86, 14, 909, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Preah Sihanouk Province', 'ខេត្តព្រះសីហនុ', '18', 3, 3, 0, 18, 11, 111, 'ព្រះរាជក្រឹត្យលេខ នស/រកត/១២០៨/១៣៨៥', 2008),
('Stung Treng Province', 'ខេត្តស្ទឹងត្រែង', '19', 1, 5, 0, 30, 4, 137, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Svay Rieng Province', 'ខេត្តស្វាយរៀង', '20', 2, 6, 0, 68, 12, 690, 'ប្រកាសលេខ ៤៩៣​ ប្រ.ក', 2008),
('Takeo Province', 'ខេត្តតាកែវ', '21', 1, 9, 0, 97, 3, 1121, 'ប្រកាសលេខ ៤៩៣​ ប្រ.ក', 2008),
('Oddar Meanchey Province', 'ខេត្តឧត្ដរមានជ័យ', '22', 1, 4, 0, 19, 5, 308, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kep Province', 'ខេត្តកែប', '23', 1, 1, 0, 2, 3, 18, 'ព្រះរាជក្រឹត្យលេខ នស/រកត/១២០៨/១៣៨៣', 2008),
('Pailin Province', 'ខេត្តប៉ៃលិន', '24', 1, 1, 0, 4, 4, 92, 'នស/រកម/1208/1384​', 2008),
('Tboung Khmum Province', 'ខេត្តត្បូងឃ្មុំ', '25', 1, 6, 0, 62, 2, 875, 'ព្រះរាជក្រឹក្យលេខ នស/រកត/១២១៣/១៤៤៥', 2013)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Banteay Meanchey province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Mongkol Borei District', 'ស្រុកមង្គលបូរី', '0102', (SELECT id FROM provinces WHERE code = '01'), 'district', 13, 0, 159, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008),
('Phnum Srok District', 'ស្រុកភ្នំស្រុក', '0103', (SELECT id FROM provinces WHERE code = '01'), 'district', 6, 0, 60, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008),
('Preah Netr Preah District', 'ស្រុកព្រះនេត្រព្រះ', '0104', (SELECT id FROM provinces WHERE code = '01'), 'district', 9, 0, 118, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008),
('Ou Chrov District', 'ស្រុកអូរជ្រៅ', '0105', (SELECT id FROM provinces WHERE code = '01'), 'district', 7, 0, 56, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Serei Saophoan Municipality', 'ក្រុងសិរីសោភ័ណ', '0106', (SELECT id FROM provinces WHERE code = '01'), 'municipality', 0, 7, 46, 'អនុក្រឹត្យលេខ ១៦អនក្រ.បក', 2008),
('Thma Puok District', 'ស្រុកថ្មពួក', '0107', (SELECT id FROM provinces WHERE code = '01'), 'district', 6, 0, 67, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008),
('Svay Chek District', 'ស្រុកស្វាយចេក', '0108', (SELECT id FROM provinces WHERE code = '01'), 'district', 8, 0, 73, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008),
('Malai District', 'ស្រុកម៉ាឡៃ', '0109', (SELECT id FROM provinces WHERE code = '01'), 'district', 6, 0, 49, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008),
('Paoy Paet Municipality', 'ក្រុងប៉ោយប៉ែត', '0110', (SELECT id FROM provinces WHERE code = '01'), 'municipality', 0, 5, 38, 'អនុក្រឹត្យលេខ ២៣២អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Battambang province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Banan District', 'ស្រុកបាណន់', '0201', (SELECT id FROM provinces WHERE code = '02'), 'district', 8, 0, 77, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Thma Koul District', 'ស្រុកថ្មគោល', '0202', (SELECT id FROM provinces WHERE code = '02'), 'district', 10, 0, 71, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Battambang Municipality', 'ក្រុងបាត់ដំបង', '0203', (SELECT id FROM provinces WHERE code = '02'), 'municipality', 0, 10, 62, '២២៣​អនក្រុ,បក', 2008),
('Bavel District', 'ស្រុកបវេល', '0204', (SELECT id FROM provinces WHERE code = '02'), 'district', 9, 0, 103, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Aek Phnum District', 'ស្រុកឯកភ្នំ', '0205', (SELECT id FROM provinces WHERE code = '02'), 'district', 7, 0, 45, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Moung Ruessei District', 'ស្រុកមោងឫស្សី', '0206', (SELECT id FROM provinces WHERE code = '02'), 'district', 9, 0, 93, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Rotonak Mondol District', 'ស្រុករតនមណ្ឌល', '0207', (SELECT id FROM provinces WHERE code = '02'), 'district', 5, 0, 38, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Sangkae District', 'ស្រុកសង្កែ', '0208', (SELECT id FROM provinces WHERE code = '02'), 'district', 10, 0, 64, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Samlout District', 'ស្រុកសំឡូត', '0209', (SELECT id FROM provinces WHERE code = '02'), 'district', 7, 0, 59, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Sampov Lun District', 'ស្រុកសំពៅលូន', '0210', (SELECT id FROM provinces WHERE code = '02'), 'district', 6, 0, 42, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Phnum Proek District', 'ស្រុកភ្នំព្រឹក', '0211', (SELECT id FROM provinces WHERE code = '02'), 'district', 5, 0, 45, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Kamrieng District', 'ស្រុកកំរៀង', '0212', (SELECT id FROM provinces WHERE code = '02'), 'district', 6, 0, 49, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Koas Krala District', 'ស្រុកគាស់ក្រឡ', '0213', (SELECT id FROM provinces WHERE code = '02'), 'district', 6, 0, 51, 'លេខ​៤៩៣ប្រ.ក', 2008),
('Rukh Kiri District', 'ស្រុករុក្ខគិរី', '0214', (SELECT id FROM provinces WHERE code = '02'), 'district', 5, 0, 45, 'លេខ​04អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kampong Cham province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Batheay District', 'ស្រុកបាធាយ', '0301', (SELECT id FROM provinces WHERE code = '03'), 'district', 10, 0, 89, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Chamkar Leu District', 'ស្រុកចំការលើ', '0302', (SELECT id FROM provinces WHERE code = '03'), 'district', 9, 0, 75, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Cheung Prey District', 'ស្រុកជើងព្រៃ', '0303', (SELECT id FROM provinces WHERE code = '03'), 'district', 12, 0, 102, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Chhuk District', 'ស្រុកឈូក', '0304', (SELECT id FROM provinces WHERE code = '03'), 'district', 8, 0, 67, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Chum Kiri District', 'ស្រុកជុំគិរី', '0305', (SELECT id FROM provinces WHERE code = '03'), 'district', 8, 0, 71, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Dambae District', 'ស្រុកដំបែ', '0306', (SELECT id FROM provinces WHERE code = '03'), 'district', 9, 0, 78, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kang Meas District', 'ស្រុកកងមាស', '0307', (SELECT id FROM provinces WHERE code = '03'), 'district', 11, 0, 94, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kaoh Soutin District', 'ស្រុកកោះសូទិន', '0308', (SELECT id FROM provinces WHERE code = '03'), 'district', 10, 0, 85, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kampong Cham Municipality', 'ក្រុងកំពង់ចាម', '0309', (SELECT id FROM provinces WHERE code = '03'), 'municipality', 0, 4, 26, 'អនុក្រឹត្យលេខ ២៣០ អនក្រ.បក', 2008),
('Kampong Siem District', 'ស្រុកកំពង់សៀម', '0310', (SELECT id FROM provinces WHERE code = '03'), 'district', 13, 0, 111, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kang Meas District', 'ស្រុកកងមាស', '0311', (SELECT id FROM provinces WHERE code = '03'), 'district', 11, 0, 94, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Koh Kong District', 'ស្រុកកោះកុង', '0312', (SELECT id FROM provinces WHERE code = '03'), 'district', 4, 0, 35, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Ou Reang Ov District', 'ស្រុកអូររាំងឪ', '0313', (SELECT id FROM provinces WHERE code = '03'), 'district', 7, 0, 60, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Ponhea Kraek District', 'ស្រុកពញាក្រែក', '0314', (SELECT id FROM provinces WHERE code = '03'), 'district', 8, 0, 70, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Prey Chhor District', 'ស្រុកព្រៃឈរ', '0315', (SELECT id FROM provinces WHERE code = '03'), 'district', 11, 0, 94, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Srei Santhor District', 'ស្រុកស្រីសន្ធរ', '0316', (SELECT id FROM provinces WHERE code = '03'), 'district', 9, 0, 76, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Stueng Trang District', 'ស្រុកស្ទឹងត្រែង', '0317', (SELECT id FROM provinces WHERE code = '03'), 'district', 8, 0, 69, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Tboung Khmum District', 'ស្រុកត្បូងឃ្មុំ', '0318', (SELECT id FROM provinces WHERE code = '03'), 'district', 14, 0, 125, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kampong Chhnang province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Baribour District', 'ស្រុកបរិបូណ៌', '0401', (SELECT id FROM provinces WHERE code = '04'), 'district', 11, 0, 64, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Chol Kiri District', 'ស្រុកជលគីរី', '0402', (SELECT id FROM provinces WHERE code = '04'), 'district', 5, 0, 29, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Kampong Chhnang Municipality', 'ក្រុងកំពង់ឆ្នាំង', '0403', (SELECT id FROM provinces WHERE code = '04'), 'municipality', 0, 4, 26, 'អនុក្រឹត្យលេខ២៣១ អនក្រ.បក', 2008),
('Kampong Leaeng District', 'ស្រុកកំពង់លែង', '0404', (SELECT id FROM provinces WHERE code = '04'), 'district', 9, 0, 44, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Kampong Tralach District', 'ស្រុកកំពង់ត្រឡាច', '0405', (SELECT id FROM provinces WHERE code = '04'), 'district', 10, 0, 103, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Rolea B''ier District', 'ស្រុករលាប្អៀរ', '0406', (SELECT id FROM provinces WHERE code = '04'), 'district', 14, 0, 135, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Sameakki Mean Chey District', 'ស្រុកសាមគ្គីមានជ័យ', '0407', (SELECT id FROM provinces WHERE code = '04'), 'district', 9, 0, 90, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Tuek Phos District', 'ស្រុកទឹកផុស', '0408', (SELECT id FROM provinces WHERE code = '04'), 'district', 9, 0, 78, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kampong Speu province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Basedth District', 'ស្រុកបរសេដ្ឋ', '0501', (SELECT id FROM provinces WHERE code = '05'), 'district', 15, 0, 218, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Chbar Mon Municipality', 'ក្រុងច្បារមន', '0502', (SELECT id FROM provinces WHERE code = '05'), 'municipality', 0, 5, 56, 'អនុក្រឹត្យលេខ​ ២២៩ អនក្រ.បក', 2008),
('Kong Pisei District', 'ស្រុកគងពិសី', '0503', (SELECT id FROM provinces WHERE code = '05'), 'district', 13, 0, 250, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Aoral District', 'ស្រុកឱរ៉ាល់', '0504', (SELECT id FROM provinces WHERE code = '05'), 'district', 5, 0, 67, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Phnum Sruoch District', 'ស្រុកភ្នំស្រួច', '0506', (SELECT id FROM provinces WHERE code = '05'), 'district', 13, 0, 149, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Samraong Tong District', 'ស្រុកសំរោងទង', '0507', (SELECT id FROM provinces WHERE code = '05'), 'district', 15, 0, 290, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Thpong District', 'ស្រុកថ្ពង', '0508', (SELECT id FROM provinces WHERE code = '05'), 'district', 7, 0, 84, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Odongk Maechay Municipality', 'ក្រុងឧដុង្គម៉ែជ័យ', '0509', (SELECT id FROM provinces WHERE code = '05'), 'municipality', 0, 5, 91, 'អនុក្រឹត្យលេខ២៧១ អនក្រ.បក ២៣ ធ្នូ ២០២២', 2022),
('Samkkei Munichay District', 'ស្រុកសាមគ្គីមុនីជ័យ', '0510', (SELECT id FROM provinces WHERE code = '05'), 'district', 10, 0, 160, 'អនុក្រឹត្យលេខ២៧១ អនក្រ.បក ២៣ ធ្នូ ២០២២', 2022)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kampong Thom province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Baray District', 'ស្រុកបារាយណ៍', '0601', (SELECT id FROM provinces WHERE code = '06'), 'district', 10, 0, 97, 'ប្រកាសលេខ ៤៩៣ប្រ.ក', 2008),
('Kampong Svay District', 'ស្រុកកំពង់ស្វាយ', '0602', (SELECT id FROM provinces WHERE code = '06'), 'district', 11, 0, 97, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Stueng Saen Municipality', 'ក្រុងស្ទឹងសែន', '0603', (SELECT id FROM provinces WHERE code = '06'), 'municipality', 0, 8, 39, 'អនុក្រឹតលេខ ១៥ អនក្រ.បក', 2008),
('Prasat Ballangk District', 'ស្រុកប្រាសាទបល្ល័ង្គ', '0604', (SELECT id FROM provinces WHERE code = '06'), 'district', 7, 0, 64, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Prasat Sambour District', 'ស្រុកប្រាសាទសំបូរ', '0605', (SELECT id FROM provinces WHERE code = '06'), 'district', 5, 0, 66, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Sandan District', 'ស្រុកសណ្ដាន់', '0606', (SELECT id FROM provinces WHERE code = '06'), 'district', 9, 0, 84, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Santuk District', 'ស្រុកសន្ទុក', '0607', (SELECT id FROM provinces WHERE code = '06'), 'district', 10, 0, 92, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Stoung District', 'ស្រុកស្ទោង', '0608', (SELECT id FROM provinces WHERE code = '06'), 'district', 13, 0, 135, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Taing Kouk District', 'ស្រុកតាំងគោក', '0609', (SELECT id FROM provinces WHERE code = '06'), 'district', 8, 0, 91, 'អនុក្រឹតលេខ០៥ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kampot province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Angkor Chey District', 'ស្រុកអង្គរជ័យ', '0701', (SELECT id FROM provinces WHERE code = '07'), 'district', 11, 0, 79, 'ប្រកាសលេខ៤៩៣​ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Banteay Meas District', 'ស្រុកបន្ទាយមាស', '0702', (SELECT id FROM provinces WHERE code = '07'), 'district', 15, 0, 88, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Chhuk District', 'ស្រុកឈូក', '0703', (SELECT id FROM provinces WHERE code = '07'), 'district', 15, 0, 80, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Chum Kiri District', 'ស្រុកជុំគិរី', '0704', (SELECT id FROM provinces WHERE code = '07'), 'district', 7, 0, 39, 'ប្រកាសលេខ​ ៤៩៣​ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Dang Tong District', 'ស្រុកដងទង់', '0705', (SELECT id FROM provinces WHERE code = '07'), 'district', 10, 0, 54, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Kampong Trach District', 'ស្រុកកំពង់ត្រាច', '0706', (SELECT id FROM provinces WHERE code = '07'), 'district', 14, 0, 70, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Tuek Chhou District', 'ស្រុកទឹកឈូ', '0707', (SELECT id FROM provinces WHERE code = '07'), 'district', 13, 0, 55, 'អនុក្រឹត្យលេខ​ ២២១ អនក្រ.បក', 2008),
('Kampot Municipality', 'ក្រុងកំពត', '0708', (SELECT id FROM provinces WHERE code = '07'), 'municipality', 0, 5, 15, 'អនុក្រឹត្យលេខ​ ២២១ អនក្រ.បក', 2008),
('Bokor Municipality', 'ក្រុងបូកគោ', '0709', (SELECT id FROM provinces WHERE code = '07'), 'municipality', 0, 3, 11, 'អនុក្រឹត្យលេខ​ ៣៨ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kandal province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Kandal Stueng District', 'ស្រុកកណ្ដាលស្ទឹង', '0801', (SELECT id FROM provinces WHERE code = '08'), 'district', 18, 0, 127, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Kien Svay District', 'ស្រុកកៀនស្វាយ', '0802', (SELECT id FROM provinces WHERE code = '08'), 'district', 8, 0, 67, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Khsach Kandal District', 'ស្រុកខ្សាច់កណ្ដាល', '0803', (SELECT id FROM provinces WHERE code = '08'), 'district', 12, 0, 67, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Kaoh Thum District', 'ស្រុកកោះធំ', '0804', (SELECT id FROM provinces WHERE code = '08'), 'district', 6, 0, 60, 'តាមប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Leuk Daek District', 'ស្រុកលើកដែក', '0805', (SELECT id FROM provinces WHERE code = '08'), 'district', 7, 0, 25, 'លេខ​ ៤៩៣ ប្រ.ក', 2008),
('Lvea Aem District', 'ស្រុកល្វាឯម', '0806', (SELECT id FROM provinces WHERE code = '08'), 'district', 10, 0, 27, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Mukh Kampul District', 'ស្រុកមុខកំពូល', '0807', (SELECT id FROM provinces WHERE code = '08'), 'district', 7, 0, 39, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Angk Snuol District', 'ស្រុកអង្គស្នួល', '0808', (SELECT id FROM provinces WHERE code = '08'), 'district', 10, 0, 200, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Ponhea Lueu District', 'ស្រុកពញាឮ', '0809', (SELECT id FROM provinces WHERE code = '08'), 'district', 11, 0, 124, 'លេខ ៤៩​៣ ប្រ.ក', 2008),
('S''ang District', 'ស្រុកស្អាង', '0810', (SELECT id FROM provinces WHERE code = '08'), 'district', 12, 0, 120, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Ta Khmau Municipality', 'ក្រុងតាខ្មៅ', '0811', (SELECT id FROM provinces WHERE code = '08'), 'municipality', 0, 10, 59, '២២៨ អនក្រ.បក', 2008),
('Sampeou Poun Municipality', 'ក្រុងសំពៅពូន', '0812', (SELECT id FROM provinces WHERE code = '08'), 'municipality', 0, 5, 53, 'អនុក្រឹត្យលេខ២៦៩ អនក្រ.បក ២៣ ធ្នូ ២០២២', 2022),
('Akreiy Ksatr Municipality', 'ក្រុងអរិយក្សត្រ', '0813', (SELECT id FROM provinces WHERE code = '08'), 'municipality', 0, 11, 42, 'អនុក្រឹត្យលេខ២៧២ អនក្រ.បក ២៣ ធ្នូ ២០២២', 2022)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Koh Kong province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Botum Sakor District', 'ស្រុកបុទុមសាគរ', '0901', (SELECT id FROM provinces WHERE code = '09'), 'district', 4, 0, 21, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Kiri Sakor District', 'ស្រុកគិរីសាគរ', '0902', (SELECT id FROM provinces WHERE code = '09'), 'district', 3, 0, 9, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Kaoh Kong District', 'ស្រុកកោះកុង', '0903', (SELECT id FROM provinces WHERE code = '09'), 'district', 4, 0, 11, 'ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Khemara Phoumin Municipality', 'ក្រុងខេមរភូមិន្ទ', '0904', (SELECT id FROM provinces WHERE code = '09'), 'municipality', 0, 3, 11, 'អនុក្រឹត្យលេខ ២២២​ អនុក្រ.បក', 2008),
('Mondol Seima District', 'ស្រុកមណ្ឌលសីមា', '0905', (SELECT id FROM provinces WHERE code = '09'), 'district', 3, 0, 13, 'ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Srae Ambel District', 'ស្រុកស្រែ អំបិល', '0906', (SELECT id FROM provinces WHERE code = '09'), 'district', 6, 0, 37, 'ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008),
('Thma Bang District', 'ស្រុកថ្មបាំង', '0907', (SELECT id FROM provinces WHERE code = '09'), 'district', 6, 0, 17, 'ប្រកាសលេខ​ ៤៩៣ ប្រ.ក របស់ក្រសួងមហាផ្ទៃ', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kratie province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Chhloung District', 'ស្រុកឆ្លូង', '1001', (SELECT id FROM provinces WHERE code = '10'), 'district', 8, 0, 50, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kracheh Municipality', 'ក្រុងក្រចេះ', '1002', (SELECT id FROM provinces WHERE code = '10'), 'municipality', 0, 5, 19, 'អនុក្រឹតលេខ ១០ អនក្រ.បក', 2008),
('Prek Prasab District', 'ស្រុកព្រែកប្រសព្វ', '1003', (SELECT id FROM provinces WHERE code = '10'), 'district', 8, 0, 61, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Sambour District', 'ស្រុកសំបូរ', '1004', (SELECT id FROM provinces WHERE code = '10'), 'district', 6, 0, 34, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Snuol District', 'ស្រុកស្នួល', '1005', (SELECT id FROM provinces WHERE code = '10'), 'district', 6, 0, 69, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Chetr Borei District', 'ស្រុកចិត្របុរី', '1006', (SELECT id FROM provinces WHERE code = '10'), 'district', 10, 0, 69, 'អនុក្រឹតលេខ ១០ អនក្រ.បក', 2008),
('Ou Krieng Saenchey District', 'ស្រុកអូរគ្រៀងសែនជ័យ', '1007', (SELECT id FROM provinces WHERE code = '10'), 'district', 5, 0, 25, 'អនុក្រឹត្យលេខ២៧០ អនក្រ.បក ២៣ ធ្នូ ២០២២', 2022)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Mondulkiri province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Kaev Seima District', 'ស្រុកកែវសីមា', '1101', (SELECT id FROM provinces WHERE code = '11'), 'district', 5, 0, 27, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Kaoh Nheaek District', 'ស្រុកកោះញែក', '1102', (SELECT id FROM provinces WHERE code = '11'), 'district', 6, 0, 26, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Ou Reang District', 'ស្រុកអូររាំង', '1103', (SELECT id FROM provinces WHERE code = '11'), 'district', 2, 0, 7, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Pech Chreada District', 'ស្រុកពេជ្រាដា', '1104', (SELECT id FROM provinces WHERE code = '11'), 'district', 4, 0, 18, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Saen Monourom Municipality', 'ក្រុងសែនមនោរម្យ', '1105', (SELECT id FROM provinces WHERE code = '11'), 'municipality', 0, 4, 14, 'អនុក្រឹតលេខ ២២៥ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Phnom Penh (khans)
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Chamkar Mon Khan', 'ខណ្ឌចំការមន', '1201', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 5, 40, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Doun Penh Khan', 'ខណ្ឌដូនពេញ', '1202', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 11, 134, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Prampir Meakkakra Khan', 'ខណ្ឌ៧មករា', '1203', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 8, 66, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Tuol Kouk Khan', 'ខណ្ឌទួលគោក', '1204', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 10, 143, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Dangkao Khan', 'ខណ្ឌដង្កោ', '1205', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 12, 81, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Mean Chey Khan', 'ខណ្ឌមានជ័យ', '1206', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 7, 59, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Russey Keo Khan', 'ខណ្ឌឫស្សីកែវ', '1207', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 7, 30, 'ប្រកាសលេខ៤៩៣ ប្រ.ក', 2008),
('Saensokh Khan', 'ខណ្ឌសែនសុខ', '1208', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 6, 47, 'អនុក្រឹត្យលេខ០៣ អនក្រ.បក', 2008),
('Pur SenChey Khan', 'ខណ្ឌពោធិ៍សែនជ័យ', '1209', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 7, 75, 'អនុក្រិតលេខ៨០', 2008),
('Chrouy Changvar Khan', 'ខណ្ឌជ្រោយចង្វារ', '1210', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 5, 22, 'អនុក្រឹត្យលេខ៥៧៧ អនក្រ.បក', 2008),
('Praek Pnov Khan', 'ខណ្ឌព្រែកព្នៅ', '1211', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 5, 59, 'អនុក្រឹត្យលេខ៥៧៨ អនក្រ.បក', 2008),
('Chbar Ampov Khan', 'ខណ្ឌច្បារអំពៅ', '1212', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 8, 49, 'អនុក្រឹត្យលេខ៥៧៩ អនក្រ.បក', 2008),
('Boeng Keng Kang Khan', 'ខណ្ឌបឹងកេងកង', '1213', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 7, 55, 'អនុក្រឹត្យលេខ០៣ អនក្រ.បក', 2008),
('Kamboul Khan', 'ខណ្ឌកំបូល', '1214', (SELECT id FROM provinces WHERE code = '12'), 'khan', 0, 7, 93, 'អនុក្រឹត្យលេខ០៤ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Preah Vihear province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Chey Saen District', 'ស្រុកជ័យសែន', '1301', (SELECT id FROM provinces WHERE code = '13'), 'district', 6, 0, 21, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Chhaeb District', 'ស្រុកឆែប', '1302', (SELECT id FROM provinces WHERE code = '13'), 'district', 8, 0, 26, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Choam Ksant District', 'ស្រុកជាំក្សាន្ដ', '1303', (SELECT id FROM provinces WHERE code = '13'), 'district', 8, 0, 49, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Kuleaen District', 'ស្រុកក្រឡាញ់', '1304', (SELECT id FROM provinces WHERE code = '13'), 'district', 6, 0, 23, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Rovieng District', 'ស្រុករវៀង', '1305', (SELECT id FROM provinces WHERE code = '13'), 'district', 12, 0, 57, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Sangkum Thmei District', 'ស្រុកសង្គមថ្មី', '1306', (SELECT id FROM provinces WHERE code = '13'), 'district', 5, 0, 24, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Tbaeng Mean Chey District', 'ស្រុកត្បែងមានជ័យ', '1307', (SELECT id FROM provinces WHERE code = '13'), 'district', 4, 0, 12, 'លេខប្រកាស ៤៩៣ ប្រ.ក', 2008),
('Preah Vihear Municipality', 'ក្រុងព្រះវិហារ', '1308', (SELECT id FROM provinces WHERE code = '13'), 'municipality', 0, 2, 20, 'អនុក្រឹតលេខ ១១ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Prey Veng province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Ba Phnum District', 'ស្រុកបាភ្នំ', '1401', (SELECT id FROM provinces WHERE code = '14'), 'district', 9, 0, 108, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Kamchay Mear District', 'ស្រុកកំចាយមារ', '1402', (SELECT id FROM provinces WHERE code = '14'), 'district', 8, 0, 129, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Kampong Trabaek District', 'ស្រុកកំពង់ត្របែក', '1403', (SELECT id FROM provinces WHERE code = '14'), 'district', 13, 0, 122, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Kanhchriech District', 'ស្រុកកញ្ជ្រៀច', '1404', (SELECT id FROM provinces WHERE code = '14'), 'district', 8, 0, 99, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Me Sang District', 'ស្រុកមេសាង', '1405', (SELECT id FROM provinces WHERE code = '14'), 'district', 8, 0, 118, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Peam Chor District', 'ស្រុកពាមជរ', '1406', (SELECT id FROM provinces WHERE code = '14'), 'district', 10, 0, 50, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Peam Ro District', 'ស្រុកពាមរក៍', '1407', (SELECT id FROM provinces WHERE code = '14'), 'district', 8, 0, 44, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Pea Reang District', 'ស្រុកពារាំង', '1408', (SELECT id FROM provinces WHERE code = '14'), 'district', 9, 0, 93, 'តាមប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Preah Sdach District', 'ស្រុកព្រះស្ដេច', '1409', (SELECT id FROM provinces WHERE code = '14'), 'district', 11, 0, 145, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Prey Veng Municipality', 'ក្រុងព្រៃវែង', '1410', (SELECT id FROM provinces WHERE code = '14'), 'municipality', 0, 4, 23, '០៩ អនក្រ.បក', 2008),
('Pur Rieng District', 'ស្រុកពោធិ៍រៀង', '1411', (SELECT id FROM provinces WHERE code = '14'), 'district', 6, 0, 36, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Sithor Kandal District', 'ស្រុកស៊ីធរកណ្ដាល', '1412', (SELECT id FROM provinces WHERE code = '14'), 'district', 11, 0, 63, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Svay Antor District', 'ស្រុកស្វាយអន្ទរ', '1413', (SELECT id FROM provinces WHERE code = '14'), 'district', 11, 0, 138, '០៩ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Pursat province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Bakan District', 'ស្រុកបាកាន', '1501', (SELECT id FROM provinces WHERE code = '15'), 'district', 9, 0, 132, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Kandieng District', 'ស្រុកកណ្ដៀង', '1502', (SELECT id FROM provinces WHERE code = '15'), 'district', 9, 0, 112, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Krakor District', 'ស្រុកក្រគរ', '1503', (SELECT id FROM provinces WHERE code = '15'), 'district', 11, 0, 107, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Phnum Kravanh District', 'ស្រុកភ្នំក្រវ៉ាញ', '1504', (SELECT id FROM provinces WHERE code = '15'), 'district', 6, 0, 47, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Pursat Municipality', 'ក្រុងពោធិ៍សាត់', '1505', (SELECT id FROM provinces WHERE code = '15'), 'municipality', 0, 7, 73, 'លេខ​ ១៤​ អនក្រ.បក', 2008),
('Veal Veaeng District', 'ស្រុកវាលវែង', '1506', (SELECT id FROM provinces WHERE code = '15'), 'district', 5, 0, 20, 'លេខ​៤៩៣​ប្រ.ក', 2008),
('Ta Lou Senchey District', 'ស្រុកតាលោសែនជ័យ', '1507', (SELECT id FROM provinces WHERE code = '15'), 'district', 2, 0, 35, 'អនុក្រឹត្យលេខ ០៧ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Ratanakiri province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Andoung Meas District', 'ស្រុកអន្លង់វែង', '1601', (SELECT id FROM provinces WHERE code = '16'), 'district', 3, 0, 21, NULL, NULL),
('Ban Lung Municipality', 'ក្រុងបានលុង', '1602', (SELECT id FROM provinces WHERE code = '16'), 'municipality', 0, 4, 19, 'អនុក្រឹត្យលេខ ២២៧ អនក្រ.បក', 2008),
('Bar Kaev District', 'ស្រុកបរកែវ', '1603', (SELECT id FROM provinces WHERE code = '16'), 'district', 6, 0, 34, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Koun Mom District', 'ស្រុកកូនមុំ', '1604', (SELECT id FROM provinces WHERE code = '16'), 'district', 6, 0, 23, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Lumphat District', 'ស្រុកលំផាត់', '1605', (SELECT id FROM provinces WHERE code = '16'), 'district', 6, 0, 26, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Ou Chum District', 'ស្រុកអូរជុំ', '1606', (SELECT id FROM provinces WHERE code = '16'), 'district', 7, 0, 37, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Ou Ya Dav District', 'ស្រុកអូរយ៉ាដាវ', '1607', (SELECT id FROM provinces WHERE code = '16'), 'district', 7, 0, 29, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Ta Veaeng District', 'ស្រុកតាវែង', '1608', (SELECT id FROM provinces WHERE code = '16'), 'district', 2, 0, 20, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Veun Sai District', 'ស្រុកវើនសៃ', '1609', (SELECT id FROM provinces WHERE code = '16'), 'district', 9, 0, 34, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Siem Reap province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Angkor Chum District', 'ស្រុកអង្គរជុំ', '1701', (SELECT id FROM provinces WHERE code = '17'), 'district', 7, 0, 86, NULL, NULL),
('Angkor Thum District', 'ស្រុកអង្គរធំ', '1702', (SELECT id FROM provinces WHERE code = '17'), 'district', 4, 0, 25, NULL, NULL),
('Banteay Srei District', 'ស្រុកបន្ទាយស្រី', '1703', (SELECT id FROM provinces WHERE code = '17'), 'district', 5, 0, 29, NULL, NULL),
('Chi Kraeng District', 'ស្រុកជីក្រែង', '1704', (SELECT id FROM provinces WHERE code = '17'), 'district', 12, 0, 155, NULL, NULL),
('Kralanh District', 'ស្រុកក្រឡាញ់', '1706', (SELECT id FROM provinces WHERE code = '17'), 'district', 10, 0, 98, NULL, NULL),
('Puok District', 'ស្រុកពួក', '1707', (SELECT id FROM provinces WHERE code = '17'), 'district', 14, 0, 132, NULL, NULL),
('Prasat Bakong District', 'ស្រុកប្រាសាទបាគង', '1710', (SELECT id FROM provinces WHERE code = '17'), 'district', 8, 0, 59, NULL, NULL),
('Siem Reap Municipality', 'ក្រុងសៀមរាប', '1711', (SELECT id FROM provinces WHERE code = '17'), 'municipality', 0, 12, 99, 'អនុក្រឹតលេខ ១៣ អនក្រ.បក', 2008),
('Soutr Nikom District', 'ស្រុកសូទ្រនិគម', '1712', (SELECT id FROM provinces WHERE code = '17'), 'district', 10, 0, 113, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Srei Snam District', 'ស្រុកស្រីស្នំ', '1713', (SELECT id FROM provinces WHERE code = '17'), 'district', 6, 0, 39, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Svay Leu District', 'ស្រុកស្វាយលើ', '1714', (SELECT id FROM provinces WHERE code = '17'), 'district', 5, 0, 34, NULL, NULL),
('Varin District', 'ស្រុកវ៉ារិន', '1715', (SELECT id FROM provinces WHERE code = '17'), 'district', 5, 0, 25, NULL, NULL),
('Run Ta Aek Techo Sen Municipality', 'ក្រុងរុនតាឯកតេជោសែន', '1716', (SELECT id FROM provinces WHERE code = '17'), 'municipality', 0, 2, 15, 'អនុក្រឹត្យលេខ០៧ ១១មករា២០២៤', 2024)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Preah Sihanouk province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Preah Sihanouk Municipality', 'ក្រុងព្រះសីហនុ', '1801', (SELECT id FROM provinces WHERE code = '18'), 'municipality', 0, 4, 15, 'អនុក្រឹត្យលេខ ០៧ អនក្រ.បក', 2008),
('Prey Nob District', 'ស្រុកព្រៃនប់', '1802', (SELECT id FROM provinces WHERE code = '18'), 'district', 10, 0, 42, 'អនុក្រឹត្យលេខ ០៧ អនក្រ.បក', 2008),
('Stueng Hav District', 'ស្រុកស្ទឹងហាវ', '1803', (SELECT id FROM provinces WHERE code = '18'), 'district', 4, 0, 13, 'អនុក្រឹត្យលេខ ០៧ អនក្រ.បក', 2008),
('Kampong Seila District', 'ស្រុកកំពង់សីលា', '1804', (SELECT id FROM provinces WHERE code = '18'), 'district', 4, 0, 14, 'ព្រះរាជក្រឹត្យលេខ នស/រកត/១២០៨/១៣៨៥', 2008),
('Kaoh Rung Municipality', 'ក្រុងកោះរ៉ុង', '1805', (SELECT id FROM provinces WHERE code = '18'), 'municipality', 0, 2, 4, 'អនុក្រឹត្យលេខ០២អនក្រ.បក', 2008),
('Kampong Soam Municipality', 'ក្រុងកំពង់សោម', '1806', (SELECT id FROM provinces WHERE code = '18'), 'municipality', 0, 5, 23, 'អនុក្រឹត្យលេខ ២០១ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Stung Treng province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Sesan District', 'ស្រុកសេសាន', '1901', (SELECT id FROM provinces WHERE code = '19'), 'district', 7, 0, 22, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Siem Bouk District', 'ស្រុកសៀមបូក', '1902', (SELECT id FROM provinces WHERE code = '19'), 'district', 7, 0, 17, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Siem Pang District', 'ស្រុកសៀមប៉ាង', '1903', (SELECT id FROM provinces WHERE code = '19'), 'district', 5, 0, 27, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Stueng Traeng Municipality', 'ក្រុងស្ទឹងត្រែង', '1904', (SELECT id FROM provinces WHERE code = '19'), 'municipality', 0, 4, 22, 'អនុក្រឹត្យលេខ២២៤ អនក្រ.បក', 2008),
('Thala Barivat District', 'ស្រុកថាឡាបរិវ៉ាត់', '1905', (SELECT id FROM provinces WHERE code = '19'), 'district', 8, 0, 31, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Borei Ou Svay Senchey District', 'ស្រុកបុរីអូរស្វាយសែនជ័យ', '1906', (SELECT id FROM provinces WHERE code = '19'), 'district', 3, 0, 18, 'អនុក្រឹត្យលេខ០៦ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Svay Rieng province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Chantrea District', 'ស្រុកចន្ទ្រា', '2001', (SELECT id FROM provinces WHERE code = '20'), 'district', 6, 0, 29, '៤៩៣ ប្រ.ក', 2008),
('Kampong Rou District', 'ស្រុកកំពង់រោទិ៍', '2002', (SELECT id FROM provinces WHERE code = '20'), 'district', 11, 0, 80, '៤៩៣ ប្រ.ក', 2008),
('Rumduol District', 'ស្រុករមាសហែក', '2003', (SELECT id FROM provinces WHERE code = '20'), 'district', 10, 0, 78, '៤៩៣ ប្រ.ក', 2008),
('Romeas Haek District', 'ស្រុករមាសហែក', '2004', (SELECT id FROM provinces WHERE code = '20'), 'district', 16, 0, 204, '៤៩៣ ប្រ.ក', 2008),
('Svay Chrum District', 'ស្រុកស្វាយជ្រំ', '2005', (SELECT id FROM provinces WHERE code = '20'), 'district', 16, 0, 158, 'តាមប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Svay Rieng Municipality', 'ក្រុងស្វាយរៀង', '2006', (SELECT id FROM provinces WHERE code = '20'), 'municipality', 0, 7, 43, '១២ អនក្រ.បក', 2008),
('Svay Teab District', 'ស្រុកស្វាយទាប', '2007', (SELECT id FROM provinces WHERE code = '20'), 'district', 9, 0, 63, 'ប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Bavet Municipality', 'ក្រុងបាវិត', '2008', (SELECT id FROM provinces WHERE code = '20'), 'municipality', 0, 5, 35, '២២៨ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Takeo province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Angkor Borei District', 'ស្រុកអង្គរបូរី', '2101', (SELECT id FROM provinces WHERE code = '21'), 'district', 6, 0, 34, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Bati District', 'ស្រុកបាទី', '2102', (SELECT id FROM provinces WHERE code = '21'), 'district', 15, 0, 168, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Borei Cholsar District', 'ស្រុកបូរីជលសារ', '2103', (SELECT id FROM provinces WHERE code = '21'), 'district', 5, 0, 39, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Kiri Vong District', 'ស្រុកគីរីវង់', '2104', (SELECT id FROM provinces WHERE code = '21'), 'district', 12, 0, 115, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Kaoh Andaet District', 'ស្រុកកោះអណ្ដែត', '2105', (SELECT id FROM provinces WHERE code = '21'), 'district', 6, 0, 68, '៤៩៣ ប្រ.ក', 2008),
('Prey Kabbas District', 'ស្រុកព្រៃកប្បាស', '2106', (SELECT id FROM provinces WHERE code = '21'), 'district', 13, 0, 112, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Samraong District', 'ស្រុកសំរោង', '2107', (SELECT id FROM provinces WHERE code = '21'), 'district', 11, 0, 147, 'លេខ ៤៩៣ ប្រ.ក', 2008),
('Doun Kaev Municipality', 'ក្រុងដូនកែវ', '2108', (SELECT id FROM provinces WHERE code = '21'), 'municipality', 0, 3, 40, '២២៦ អនក្រ.បក', 2008),
('Tram Kak District', 'ស្រុកត្រាំកក់', '2109', (SELECT id FROM provinces WHERE code = '21'), 'district', 15, 0, 244, 'តាមប្រកាសលេខ ៤៩៣ ប្រ.ក', 2008),
('Treang District', 'ស្រុកទ្រាំង', '2110', (SELECT id FROM provinces WHERE code = '21'), 'district', 14, 0, 154, 'លេខ ៤៩៣ ប្រ.ក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Oddar Meanchey province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Anlong Veaeng District', 'ស្រុកអន្លង់វែង', '2201', (SELECT id FROM provinces WHERE code = '22'), 'district', 5, 0, 58, NULL, NULL),
('Banteay Ampil District', 'ស្រុកបន្ទាយអំពិល', '2202', (SELECT id FROM provinces WHERE code = '22'), 'district', 4, 0, 86, NULL, NULL),
('Chong Kal District', 'ស្រុកចុងកាល់', '2203', (SELECT id FROM provinces WHERE code = '22'), 'district', 4, 0, 35, NULL, NULL),
('Samraong Municipality', 'ក្រុងសំរោង', '2204', (SELECT id FROM provinces WHERE code = '22'), 'municipality', 0, 5, 76, NULL, NULL),
('Trapeang Prasat District', 'ស្រុកត្រពាំងប្រាសាទ', '2205', (SELECT id FROM provinces WHERE code = '22'), 'district', 6, 0, 53, NULL, NULL)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Kep province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Damnak Chang''aeur District', 'ស្រុកដំណាក់ចង្អើរ', '2301', (SELECT id FROM provinces WHERE code = '23'), 'district', 2, 0, 11, 'អនុក្រឹត្យលេខ ០៦ អនក្រ.បក', 2008),
('Kaeb Municipality', 'ក្រុងកែប', '2302', (SELECT id FROM provinces WHERE code = '23'), 'municipality', 0, 3, 7, 'អនុក្រឹត្យលេខ ០៦​ អនក្រ.បក', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Pailin province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Pailin Municipality', 'ក្រុងប៉ៃលិន', '2401', (SELECT id FROM provinces WHERE code = '24'), 'municipality', 0, 4, 41, 'លេខ 05​អនក្រុ.បក', 2008),
('Sala Krau District', 'ស្រុកសាលាក្រៅ', '2402', (SELECT id FROM provinces WHERE code = '24'), 'district', 4, 0, 51, '០៥​អនក្រ.បក​', 2008)
ON CONFLICT (code) DO NOTHING;

-- Insert districts for Tboung Khmum province
INSERT INTO districts (name, name_kh, code, province_id, type, communes_commune, communes_sangkat, total_villages, reference_number, reference_year) VALUES
('Dambae District', 'ស្រុកដំណាក់ចង្អើរ', '2501', (SELECT id FROM provinces WHERE code = '25'), 'district', 7, 0, 83, 'Royal Degree 1445', 2013),
('Krouch Chhmar District', 'ស្រុកក្រូចឆ្មារ', '2502', (SELECT id FROM provinces WHERE code = '25'), 'district', 12, 0, 77, 'Royal Degree 1445', 2013),
('Memot District', 'ស្រុកមេមត់', '2503', (SELECT id FROM provinces WHERE code = '25'), 'district', 14, 0, 182, 'Royal Degree 1445', 2013),
('Ou Reang Ov District', 'ស្រុកអូររាំងឪ', '2504', (SELECT id FROM provinces WHERE code = '25'), 'district', 7, 0, 142, 'Royal Degree 1445', 2013),
('Ponhea Kraek District', 'ស្រុកពញាក្រែក', '2505', (SELECT id FROM provinces WHERE code = '25'), 'district', 8, 0, 150, 'Royal Degree 1445', 2013),
('Suong Municipality', 'ក្រុងសួង', '2506', (SELECT id FROM provinces WHERE code = '25'), 'municipality', 0, 2, 30, 'ព្រះរាជក្រិត្យលេខ ១៤៤៥', 2013),
('Tboung Khmum District', 'ស្រុកត្បូងឃ្មុំ', '2507', (SELECT id FROM provinces WHERE code = '25'), 'district', 14, 0, 211, 'Royal Degree 1445', 2013)
ON CONFLICT (code) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE provinces IS 'Cambodian provinces with geographic, demographic, and administrative division data';
COMMENT ON TABLE districts IS 'Cambodian districts/khans within provinces';
COMMENT ON COLUMN provinces.name_kh IS 'Province name in Khmer script';
COMMENT ON COLUMN provinces.code IS 'Numeric province code (01-25) as per Royal Gazette';
COMMENT ON COLUMN provinces.districts_krong IS 'Number of Krong (municipal) districts';
COMMENT ON COLUMN provinces.districts_srok IS 'Number of Srok (rural) districts';
COMMENT ON COLUMN provinces.districts_khan IS 'Number of Khan (urban) districts in Phnom Penh';
COMMENT ON COLUMN provinces.communes_commune IS 'Number of rural communes';
COMMENT ON COLUMN provinces.communes_sangkat IS 'Number of urban sangkats';
COMMENT ON COLUMN provinces.total_villages IS 'Total number of villages across the province';
COMMENT ON COLUMN provinces.reference_number IS 'Reference to Royal Gazette or official document';
COMMENT ON COLUMN provinces.reference_year IS 'Year of the reference document';
COMMENT ON COLUMN districts.name_kh IS 'District name in Khmer script';
COMMENT ON COLUMN districts.code IS 'Unique district code (province_code + sequential number)';
COMMENT ON COLUMN districts.type IS 'Type of administrative unit (district, municipality, etc.)';
COMMENT ON COLUMN districts.communes_commune IS 'Number of rural communes in the district';
COMMENT ON COLUMN districts.communes_sangkat IS 'Number of urban sangkats in the district';
COMMENT ON COLUMN districts.total_villages IS 'Total number of villages in the district';
COMMENT ON COLUMN districts.reference_number IS 'Reference to Royal Gazette or official document';
COMMENT ON COLUMN districts.reference_year IS 'Year of the reference document';

-- Verify the data was inserted
SELECT
    'provinces' as table_name,
    COUNT(*) as record_count
FROM provinces
UNION ALL
SELECT 'districts', COUNT(*) FROM districts
ORDER BY table_name;