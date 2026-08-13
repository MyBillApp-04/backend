package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.entity.CatalogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    List<CatalogItem> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<CatalogItem> findByIdAndUserId(UUID id, Long userId);

    Optional<CatalogItem> findByIdAndUserIdAndIsDeletedFalse(UUID id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE CatalogItem c
           SET c.name = :name,
               c.description = :description,
               c.type = :type,
               c.defaultRate = :defaultRate,
               c.defaultTaxRate = :defaultTaxRate,
               c.unit = :unit,
               c.dimension = :dimension,
               c.kgs = :kgs,
               c.isActive = :isActive,
               c.updatedAt = :updatedAt,
               c.deletedAt = :deletedAt,
               c.isDeleted = :isDeleted,
               c.version = :version
           WHERE c.id = :id
             AND c.user.id = :userId
           """)
    int updateCatalogItemFromSync(
            @Param("id") UUID id,
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("type") String type,
            @Param("defaultRate") Double defaultRate,
            @Param("defaultTaxRate") Double defaultTaxRate,
            @Param("unit") String unit,
            @Param("dimension") String dimension,
            @Param("kgs") Double kgs,
            @Param("isActive") Boolean isActive,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("isDeleted") Boolean isDeleted,
            @Param("version") Integer version
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE CatalogItem c
           SET c.isDeleted = true,
               c.deletedAt = :deletedAt,
               c.updatedAt = :deletedAt,
               c.version = :version
           WHERE c.id = :id
             AND c.user.id = :userId
           """)
    int markCatalogItemDeletedFromSync(
            @Param("id") UUID id,
            @Param("userId") Long userId,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("version") Integer version
    );

    @Query("""
           SELECT c FROM CatalogItem c
           WHERE c.user.id = :userId
             AND c.isDeleted = false
             AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))
             )
           """)
    List<CatalogItem> searchByUserIdAndQuery(
            @Param("userId") Long userId,
            @Param("query") String query
    );

    long countByUserIdAndIsDeletedFalse(Long userId);

    List<CatalogItem> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime since);

    List<CatalogItem> findByUserId(Long userId);

    Page<CatalogItem> findByUserId(Long userId, Pageable pageable);

    Page<CatalogItem> findByUserIdAndUpdatedAtAfter(
            Long userId,
            LocalDateTime updatedAt,
            Pageable pageable
    );

    @Query("""
           SELECT c FROM CatalogItem c
           WHERE c.user.id = :userId
             AND c.isDeleted = false
             AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))
             )
           """)
    Page<CatalogItem> searchProjectedByUserIdAndQuery(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
           SELECT c FROM CatalogItem c
           WHERE c.user.id = :userId
             AND (c.updatedAt > :lastTime OR (c.updatedAt = :lastTime AND c.id > :lastId))
           """)
    Page<CatalogItem> findByUserIdWithKeyset(
            @Param("userId") Long userId,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("lastId") UUID lastId,
            Pageable pageable
    );

    @Query("SELECT c FROM CatalogItem c WHERE c.user.id = :userId AND c.updatedAt >= :since")
    Page<CatalogItem> findByUserIdAndUpdatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );
}