package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RepoFav;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.GithubRepo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
//사용자의 즐겨찾기 저장소의 데이터 조회 및 관리 Repository
public interface RepoFavRepository extends JpaRepository<RepoFav, Integer> {
    // 사용자가 특정 저장소를 즐겨찾기했는지 확인
    boolean existsByUserAndGithubRepo(User user, GithubRepo githubRepo);
    // 사용자 ID를 기준으로 모든 즐겨찾기 저장소 조회
    List<RepoFav> findAllByUser_Id(Integer userId);
    // 사용자와 저장소를 기준으로 즐겨찾기 삭제
    void deleteByUserAndGithubRepo(User user, GithubRepo githubRepo);
    // 사용자의 모든 즐겨찾기 데이터 삭제 
    @Modifying
    @Query(value = "DELETE FROM REPO_FAV WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Integer userId);
}
