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

//사용자의 즐겨찾기 저장소 등록, 삭제, 조회 기능을 처리하는 Service
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

    // 특정 저장소를 사용자의 즐겨찾기에 추가
    @Transactional
    public void addFavorite(Integer userId, Integer repoId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        //GitHub Repository 조회 
        GithubRepo githubRepo = githubRepoRepository.findById(repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));
        //이미 즐겨찾기 되어있을 경우, 중복 저장 X
        if (repoFavRepository.existsByUserAndGithubRepo(user, githubRepo)) {
            return; 
        }
        // 즐겨찾기 정보 생성
        RepoFav repofav = RepoFav.builder()
                .user(user)
                .githubRepo(githubRepo)
                .build();
        repoFavRepository.save(repofav);
    }
    //선택한 저장소를 사용자의 즐겨찾기에서 삭제
    @Transactional
    public void removeFavorite(Integer userId, Integer repoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        GithubRepo githubRepo = githubRepoRepository.findById(repoId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPO_NOT_FOUND));

        repoFavRepository.deleteByUserAndGithubRepo(user, githubRepo);
    }
    //사용자의 즐겨찾기 저장소 목록 조회
    public List<RepoResponse> getFavoriteRepos(Integer userId) {
        // 즐겨찾기 목록을 저장소 응답 DTO로 변환
        return repoFavRepository.findAllByUser_Id(userId).stream()
                .map(fav -> RepoResponse.from(fav.getGithubRepo()))
                .toList();
    }
}
