-- User Profiles Migration
-- Creates the user_profiles table for storing user profile information

create table user_profiles (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique, -- References user_accounts.id in Identity service (no FK constraint - different service)
    full_name varchar(255),
    phone varchar(20), -- E.164 format (e.g., +919876543210)
    avatar_url varchar(2048), -- URL to avatar image (stored in object storage)
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    
    constraint user_profiles_user_id_unique unique (user_id)
);

-- Index for faster lookups by user_id
create index idx_user_profiles_user_id on user_profiles(user_id);

-- Comment for documentation
comment on table user_profiles is 'Stores user profile information (name, phone, avatar) separate from authentication credentials';
comment on column user_profiles.user_id is 'References user_accounts.id in Identity service (no FK - different service/database)';
comment on column user_profiles.full_name is 'Full name of the user (e.g., "John Doe")';
comment on column user_profiles.phone is 'Contact phone number in E.164 format (may differ from auth phone)';
comment on column user_profiles.avatar_url is 'URL to user avatar/profile picture (stored in object storage like S3)';

