ALTER TABLE collections
ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE collections
ADD CONSTRAINT chk_collections_visibility
CHECK (visibility IN ('PUBLIC', 'PRIVATE'));
