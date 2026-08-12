package com.mybeaufortviewproject.mybeaufortview_backend.collection;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.mybeaufortviewproject.mybeaufortview_backend.post.Post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "collection_entries",

//uniqueConstraints to prevent duplicate entries of the same post in a collection
uniqueConstraints = @UniqueConstraint(columnNames = {"collection_id", "post_id"}),
indexes = {
        @Index(name = "idx_collection_id", columnList = "collection_id"),
        @Index(name = "idx_post_id", columnList = "post_id")
    }
)

public class CollectionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected CollectionEntry() {} // for JPA

    public CollectionEntry(Collection collection, Post post) {
        this.collection = collection;
        this.post = post;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

}
