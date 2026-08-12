CREATE TABLE locations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL UNIQUE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE posts
ADD COLUMN location_id BIGINT;

ALTER TABLE posts
ADD CONSTRAINT fk_posts_location
FOREIGN KEY (location_id) REFERENCES locations(id);

CREATE INDEX idx_posts_location_id ON posts(location_id);

INSERT INTO locations (name, slug, latitude, longitude)
VALUES
('Hunting Island', 'hunting-island', 32.3738, -80.4512),
('Downtown Beaufort', 'downtown-beaufort', 32.4316, -80.6698),
('Waterfront Park', 'waterfront-park', 32.4310, -80.6690),
('Spanish Moss Trail', 'spanish-moss-trail', 32.4490, -80.6760);
