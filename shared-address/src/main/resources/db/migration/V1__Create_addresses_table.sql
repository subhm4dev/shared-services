-- Addresses Migration
-- Creates the addresses table for storing user shipping addresses

create table addresses (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null, -- References user_accounts.id in Identity service (no FK constraint - different service)
    tenant_id uuid not null, -- Multi-tenant isolation
    line1 varchar(255) not null,
    line2 varchar(255), -- Optional second line (apartment, suite, etc.)
    city varchar(100) not null,
    state varchar(100), -- Optional state/province
    postcode varchar(20) not null,
    country varchar(2) not null, -- ISO 3166-1 alpha-2 country code (e.g., "US", "IN")
    label varchar(50), -- Optional label (e.g., "Home", "Office", "Warehouse")
    is_default boolean not null default false,
    deleted boolean not null default false, -- Soft delete flag
    deleted_at timestamp, -- Timestamp when address was soft deleted
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

-- Partial unique index to prevent duplicate addresses (only for active addresses)
-- This allows same address to exist once per user, but only if not deleted
create unique index idx_addresses_unique_active 
    on addresses(user_id, tenant_id, line1, city, postcode, country) 
    where deleted = false;

-- Index for faster lookups by user_id
create index idx_addresses_user_id on addresses(user_id);

-- Index for faster lookups by tenant_id
create index idx_addresses_tenant_id on addresses(tenant_id);

-- Index for filtering by deleted status
create index idx_addresses_deleted on addresses(deleted);

-- Composite index for common queries (user + tenant + deleted)
create index idx_addresses_user_tenant_deleted on addresses(user_id, tenant_id, deleted);

-- Comments for documentation
comment on table addresses is 'Stores shipping addresses for users. Supports multi-tenant isolation and soft delete.';
comment on column addresses.user_id is 'References user_accounts.id in Identity service (no FK - different service/database)';
comment on column addresses.tenant_id is 'Tenant ID for multi-tenant data isolation';
comment on column addresses.line1 is 'First line of address (e.g., "123 Main Street")';
comment on column addresses.line2 is 'Second line of address (optional, e.g., "Apartment 4B")';
comment on column addresses.city is 'City name';
comment on column addresses.state is 'State or province (optional)';
comment on column addresses.postcode is 'Postal or ZIP code';
comment on column addresses.country is 'Country code (ISO 3166-1 alpha-2, e.g., "US", "IN")';
comment on column addresses.label is 'Address label for identification (e.g., "Home", "Office", "Warehouse")';
comment on column addresses.is_default is 'Whether this is the default address for the user';
comment on column addresses.deleted is 'Soft delete flag - when true, address is considered deleted but retained for audit';
comment on column addresses.deleted_at is 'Timestamp when address was soft deleted (null if active)';

