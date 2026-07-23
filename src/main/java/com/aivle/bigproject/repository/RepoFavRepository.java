package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RepoFav;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.GithubRepo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RepoFavRepository extends JpaRepository<RepoFav, Integer> {
    boolean existsByUserAndGithubRepo(User user, GithubRepo githubRepo);
    List<RepoFav> findAllByUser_Id(Integer userId);
    void deleteByUserAndGithubRepo(User user, GithubRepo githubRepo);

    @Modifying
    @Query(value = "DELETE FROM REPO_FAV WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Integer userId);
}
