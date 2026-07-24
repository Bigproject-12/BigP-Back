package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.UserRepo;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.security.GithubTokenCrypto;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/*
@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock UserRepository userRepository;
    @Mock GithubTokenCrypto githubTokenCrypto;
    @Mock GithubRepoRepository githubRepoRepository;
    @Mock UserRepoRepository userRepoRepository;

    @Test
    void getUserRepoReturnsOnlyConnectedRepository() {
        GithubRepo repo = GithubRepo.builder().id(10).name("BigP").build();
        UserRepo userRepo = UserRepo.builder().githubRepo(repo).build();
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 10))
                .thenReturn(Optional.of(userRepo));
        GithubService service = service();

        var response = service.getUserRepo(1, 10);

        assertEquals(10, response.id());
        assertEquals("BigP", response.name());
    }

    @Test
    void getUserRepoRejectsUnconnectedRepository() {
        when(userRepoRepository.findByUser_IdAndGithubRepo_Id(1, 99))
                .thenReturn(Optional.empty());
        GithubService service = service();

        assertThrows(CustomException.class, () -> service.getUserRepo(1, 99));
    }
 
    private GithubService service() {
        return new GithubService(
                userRepository,
                githubTokenCrypto,
                githubRepoRepository,
                userRepoRepository
        );
    }
        
}
*/