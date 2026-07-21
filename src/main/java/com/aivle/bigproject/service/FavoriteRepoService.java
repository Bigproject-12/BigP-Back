package com.aivle.bigproject.service;

import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.RepoFav;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.repository.RepoFavRepository;
import com.aivle.bigproject.dto.repo.RepoResponse;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FavoriteRepoService {
    private final UserRepository userRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final RepoFavRepository repoFavRepository;

    public FavoriteRepoService(UserRepository userRepository, 
        GithubRepoRepository githubRepoRepository,
        RepoFavRepository repoFavRepository) {
            this.userRepository = userRepository;
            this.githubRepoRepository = githubRepoRepository;
            this.repoFavRepository = repoFavRepository;
        }


    @Transactional
    public void addFavorite(Integer userId, Integer repoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        GithubRepo githubRepo = githubRepoRepository.findById(repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));

        if (repoFavRepository.existsByUserAndGithubRepo(user, githubRepo)) {
            return; // 이미 즐겨찾기 되어있을 경우, 중복 저장 X
        }
        RepoFav repofav = RepoFav.builder()
                .user(user)
                .githubRepo(githubRepo)
                .build();
        repoFavRepository.save(repofav);
    }
    
    @Transactional
    public void removeFavorite(Integer userId, Integer repoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        GithubRepo githubRepo = githubRepoRepository.findById(repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));

        repoFavRepository.deleteByUserAndGithubRepo(user, githubRepo);
    }

    public List<RepoResponse> getFavoriteRepos(Integer userId) {
        return repoFavRepository.findAllByUser_Id(userId).stream()
                .map(fav -> RepoResponse.from(fav.getGithubRepo()))
                .toList();
    }
}
