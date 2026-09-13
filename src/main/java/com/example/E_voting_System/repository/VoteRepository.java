package com.example.E_voting_System.repository;

import com.example.E_voting_System.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByVoterId(String voterId);

    @Query("SELECT v.blockHash FROM Vote v ORDER BY v.id DESC LIMIT 1")
    Optional<String> findLatestBlockHash();

    List<Vote> findAllByOrderByIdAsc();
}