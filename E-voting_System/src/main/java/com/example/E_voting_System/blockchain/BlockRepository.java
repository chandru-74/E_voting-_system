package com.example.E_voting_System.blockchain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ✅ ENHANCED BLOCK REPOSITORY
 *
 * Provides database access for Block entity
 * Includes custom query methods for common operations
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Find the most recent block (highest ID)
     */
    Block findTopByOrderByIdDesc();

    /**
     * Find all blocks ordered by timestamp (oldest first)
     */
    List<Block> findAllByOrderByTimestampAsc();

    /**
     * Find all blocks ordered by timestamp (newest first)
     */
    List<Block> findAllByOrderByTimestampDesc();

    /**
     * Find a block by its hash value
     */
    Optional<Block> findByHash(String hash);

    /**
     * Find a block by its blockId
     */
    Optional<Block> findByBlockId(String blockId);

    /**
     * Check if a block with given hash exists
     */
    boolean existsByHash(String hash);

    /**
     * Check if a block with given blockId exists
     */
    boolean existsByBlockId(String blockId);

    /**
     * Find all blocks created after a specific timestamp
     */
    List<Block> findAllByTimestampAfter(Instant timestamp);

    /**
     * Find all blocks created before a specific timestamp
     */
    List<Block> findAllByTimestampBefore(Instant timestamp);

    /**
     * Find all blocks created within a time range
     */
    List<Block> findAllByTimestampBetween(Instant startTime, Instant endTime);

    /**
     * Count blocks by blockId prefix (useful for finding block-* entries)
     */
    @Query("SELECT COUNT(b) FROM Block b WHERE b.blockId LIKE CONCAT(:prefix, '%')")
    long countByBlockIdPrefix(@Param("prefix") String prefix);

    /**
     * Delete all blocks created before a specific timestamp
     * Useful for archiving old blocks
     */
    long deleteByTimestampBefore(Instant beforeTime);

    /**
     * Get the earliest block (genesis block)
     */
    Optional<Block> findFirstByOrderByTimestampAsc();

    /**
     * Get the latest block
     */
    Optional<Block> findFirstByOrderByIdDesc();

    /**
     * Find blocks with null signature (invalid blocks)
     */
    List<Block> findAllBySignatureIsNull();

    /**
     * Find blocks with null hash (unprocessed blocks)
     */
    List<Block> findAllByHashIsNull();

    /**
     * Find blocks with null merkleRoot
     */
    List<Block> findAllByMerkleRootIsNull();

    /**
     * Custom query to get block count
     */
    @Query("SELECT COUNT(b) FROM Block b")
    long getTotalBlockCount();

    /**
     * Custom query to get blocks in a specific nonce range
     * Used for analyzing mining difficulty
     */
    @Query("SELECT b FROM Block b WHERE b.nonce BETWEEN :minNonce AND :maxNonce ORDER BY b.timestamp ASC")
    List<Block> findBlocksByNonceRange(@Param("minNonce") long minNonce, @Param("maxNonce") long maxNonce);

    /**
     * Custom query to find blocks with specific data prefix
     * Useful for searching votes
     */
    @Query("SELECT b FROM Block b WHERE b.data LIKE CONCAT(:prefix, '%') ORDER BY b.timestamp DESC")
    List<Block> findBlocksByDataPrefix(@Param("prefix") String prefix);
}