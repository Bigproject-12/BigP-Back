package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RepoFav;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepoFavRepository extends JpaRepository<RepoFav, Integer> {
    @Modifying
    @Query(value = "DELETE FROM REPO_FAV WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Integer userId);
}
