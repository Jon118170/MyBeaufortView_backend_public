-- collection_entries: drop and recreate FK with ON DELETE CASCADE
ALTER TABLE collection_entries DROP CONSTRAINT fk_collection_entries_post;
ALTER TABLE collection_entries
    ADD CONSTRAINT fk_collection_entries_post
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- post_tags: drop and recreate FK with ON DELETE CASCADE
ALTER TABLE post_tags DROP CONSTRAINT fk_post_tags_post;
ALTER TABLE post_tags
    ADD CONSTRAINT fk_post_tags_post
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;

-- likes: drop and recreate FK with ON DELETE CASCADE
ALTER TABLE likes DROP CONSTRAINT fk_likes_post;
ALTER TABLE likes
    ADD CONSTRAINT fk_likes_post
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE;
