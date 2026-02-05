-- =============================================
-- Inventory Service Database Schema
-- PostgreSQL 16
-- =============================================

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    price DECIMAL(15,2) NOT NULL,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT chk_quantity_positive CHECK (quantity >= 0),
    CONSTRAINT chk_reserved_valid CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity),
    CONSTRAINT chk_price_positive CHECK (price >= 0)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_products_updated_at ON products;
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Sample data for testing
INSERT INTO products (sku, name, description, quantity, price) VALUES
    ('LAPTOP-001', 'MacBook Pro 14"', 'Apple MacBook Pro with M3 chip', 50, 1999.99),
    ('PHONE-001', 'iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB', 100, 1199.99),
    ('TABLET-001', 'iPad Pro 12.9"', 'Apple iPad Pro with M2 chip', 75, 1099.99),
    ('WATCH-001', 'Apple Watch Ultra 2', 'Apple Watch Ultra 2 GPS + Cellular', 30, 799.99),
    ('HEADPHONE-001', 'AirPods Pro 2', 'Apple AirPods Pro 2nd Generation', 200, 249.99)
ON CONFLICT (sku) DO NOTHING;
