-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    order_id UUID,
    payment_method_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    method_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    gateway_provider VARCHAR(50) NOT NULL,
    gateway_transaction_id VARCHAR(200),
    gateway_payment_id VARCHAR(200),
    failure_reason VARCHAR(1000),
    payment_link VARCHAR(500),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for payments table
CREATE INDEX IF NOT EXISTS idx_payment_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_tenant_id ON payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payment_gateway_transaction_id ON payments(gateway_transaction_id);

-- Create payment_methods table
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    token VARCHAR(500) NOT NULL,
    masked_number VARCHAR(50),
    upi_id VARCHAR(100),
    phone_number VARCHAR(20),
    card_type VARCHAR(50),
    expiry_month INTEGER,
    expiry_year INTEGER,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for payment_methods table
CREATE INDEX IF NOT EXISTS idx_payment_method_user_id ON payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_method_tenant_id ON payment_methods(tenant_id);

-- Create payment_refunds table
CREATE TABLE IF NOT EXISTS payment_refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    refund_amount DECIMAL(19, 2) NOT NULL,
    gateway_refund_id VARCHAR(200),
    reason VARCHAR(500),
    failure_reason VARCHAR(1000),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- Create indexes for payment_refunds table
CREATE INDEX IF NOT EXISTS idx_refund_payment_id ON payment_refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_refund_status ON payment_refunds(status);

