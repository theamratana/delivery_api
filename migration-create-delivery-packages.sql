-- Create delivery_packages table
CREATE TABLE IF NOT EXISTS delivery_packages (
  id uuid PRIMARY KEY,
  sender_id uuid,
  delivery_fee numeric(10,2),
  status varchar(50),
  notes text,
  created_at timestamptz,
  updated_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_delivery_packages_sender_id ON delivery_packages(sender_id);
