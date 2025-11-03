package com.example.social.service;

import com.example.social.domain.Role;
import com.example.social.domain.entity.Comment;
import com.example.social.domain.entity.Like;
import com.example.social.domain.entity.Post;
import com.example.social.domain.entity.User;
import com.example.social.domain.repository.CommentRepository;
import com.example.social.domain.repository.LikeRepository;
import com.example.social.domain.repository.PostRepository;
import com.example.social.domain.repository.UserRepository;
import com.example.social.security.CurrentUser;
import com.example.social.web.exception.ForbiddenException;
import com.example.social.web.exception.NotFoundException;
import com.example.social.web.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final AuthService authService;
    private final FileStorageService storage;
    private final UserRepository users;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final LikeRepository likes;

    private User requireUser(Long id){
        return users.findById(id).orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    @Transactional
    public Post createPost(MultipartFile image, String description){
        CurrentUser currentUser = authService.requireCurrent();

        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Description too long (max 2000 chars)");
        }

        String imageUrl = storage.saveImage(image);
        Post post = Post.builder()
                .authorId(currentUser.id())
                .imagePath(imageUrl)
                .description(description)
                .likeCount(0)
                .viewCount(0)
                .createdAt(Instant.now())
                .deleted(false)
                .build();

        return posts.save(post);
    }

    @Transactional(readOnly = true)
    public Post getPostOr404(Long id){
        return posts.findByIdAndDeletedFalse(id).orElseThrow(()-> new NotFoundException("Post not found"));
    }

    @Transactional(readOnly = true)
    public List<Post> listActive(){
        return posts.findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    @Transactional
    public void incrementView(Long id){
        Post post = getPostOr404(id);
        post.setViewCount(post.getViewCount()+1);
        posts.save(post);
    }

    @Transactional
    public Post updatePost(Long id, MultipartFile image, String description){
        CurrentUser currentUser = authService.requireCurrent();
        Post post = getPostOr404(id);

        if (!currentUser.role().equals(Role.ADMIN) && !post.getAuthorId().equals(currentUser.id())){
            throw new ForbiddenException("Not allowed");
        }

        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("Description too long (max 2000 chars)");
        }

        boolean updated = false;

        if (image != null && !image.isEmpty()) {
            String newImageUrl = storage.saveImage(image);
            post.setImagePath(newImageUrl);
            updated = true;
        }

        if (description != null) {
            post.setDescription(description);
            updated = true;
        }

        if(updated){
            post.setUpdatedAt(Instant.now());
            return posts.save(post);
        }

        return post;
    }

    @Transactional
    public void deletePost(Long id){
        CurrentUser currentUser = authService.requireCurrent();
        Post post = getPostOr404(id);

        if (!currentUser.role().equals(Role.ADMIN) && !post.getAuthorId().equals(currentUser.id())){
            throw new ForbiddenException("Not allowed");
        }

        post.setDeleted(true);
        posts.save(post);
    }

    @Transactional
    public void likePost(Long id){
        CurrentUser currentUser = authService.requireCurrent();
        Post post = getPostOr404(id);

        if(likes.existsByUserIdAndPostId(currentUser.id(), id)){
            return;
        }

        likes.save(Like.builder()
                .userId(currentUser.id())
                .postId(id)
                .createdAt(Instant.now())
                .build());

        post.setLikeCount(post.getLikeCount() + 1);
        posts.save(post);
    }

    @Transactional
    public void unlikePost(Long id){
        CurrentUser currentUser = authService.requireCurrent();
        Post post = getPostOr404(id);
        long deleted = likes.deleteByUserIdAndPostId(currentUser.id(), id);
        if (deleted > 0 && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount()-1);
            posts.save(post);
        }
    }

    @Transactional
    public Comment addComment(Long postId, String content){
        CurrentUser currentUser = authService.requireCurrent();
        getPostOr404(postId);

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(currentUser.id())
                .content(content)
                .createdAt(Instant.now())
                .deleted(false)
                .build();

        return comments.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> listComments(Long postId){
        getPostOr404(postId);
        return comments.findAllByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId);
    }

    @Transactional
    public void deleteComment(Long commentId){
        CurrentUser currentUser = authService.requireCurrent();
        Comment comment = comments.findByIdAndDeletedFalse(commentId).orElseThrow(()-> new NotFoundException("Comment not found"));

        Post post = getPostOr404(comment.getPostId());

        boolean canDelete = currentUser.role().equals(Role.ADMIN) || comment.getAuthorId().equals(currentUser.id()) || post.getAuthorId().equals(currentUser.id());

        if(!canDelete){
            throw new ForbiddenException("Not allowed");
        }

        comment.setDeleted(true);
        comments.save(comment);
    }
}

















