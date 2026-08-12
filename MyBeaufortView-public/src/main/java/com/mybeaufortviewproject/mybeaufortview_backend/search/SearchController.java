package com.mybeaufortviewproject.mybeaufortview_backend.search;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mybeaufortviewproject.mybeaufortview_backend.post.PostService;
import com.mybeaufortviewproject.mybeaufortview_backend.post.dto.PostResponse;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PostService postService;

    public SearchController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<PostResponse> searchPosts(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "desc") String sort
    ) {
        return postService.searchPostResponses(query, sort);
    }

}
