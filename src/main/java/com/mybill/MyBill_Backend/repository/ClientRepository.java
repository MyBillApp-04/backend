package com.mybill.MyBill_Backend.repository;

import com.mybill.MyBill_Backend.dto.ClientProjection;
import com.mybill.MyBill_Backend.entity.Client;
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

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<Client> findByIdAndUserId(UUID id, Long userId);

    Optional<Client> findByIdAndUserIdAndIsDeletedFalse(UUID clientId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Client c
           SET c.name = :name,
               c.phone = :phone,
               c.email = :email,
               c.address = :address,
               c.deviceId = :deviceId,
               c.updatedAt = :updatedAt,
               c.deletedAt = :deletedAt,
               c.isDeleted = :isDeleted,
               c.version = :version
           WHERE c.id = :id
             AND c.user.id = :userId
           """)
    int updateClientFromSync(
            @Param("id") UUID id,
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("address") String address,
            @Param("deviceId") String deviceId,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("isDeleted") Boolean isDeleted,
            @Param("version") Integer version
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Client c
           SET c.isDeleted = true,
               c.deletedAt = :deletedAt,
               c.updatedAt = :deletedAt,
               c.deviceId = :deviceId,
               c.version = :version
           WHERE c.id = :id
             AND c.user.id = :userId
           """)
    int markClientDeletedFromSync(
            @Param("id") UUID id,
            @Param("userId") Long userId,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deviceId") String deviceId,
            @Param("version") Integer version
    );

    @Query("""
           SELECT c FROM Client c
           WHERE c.user.id = :userId
             AND c.isDeleted = false
             AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))
             )
           """)
    List<Client> searchByUserIdAndQuery(
            @Param("userId") Long userId,
            @Param("query") String query
    );

    long countByUserIdAndIsDeletedFalse(Long userId);

    List<Client> findByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime since);

    List<Client> findByUserId(Long userId);

    Page<Client> findByUserId(Long userId, Pageable pageable);

    Page<Client> findByUserIdAndUpdatedAtAfter(
            Long userId,
            LocalDateTime updatedAt,
            Pageable pageable
    );

    // NEW: Projection based pagination queries for optimal load
    Page<ClientProjection> findProjectedByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    @Query("""
           SELECT c FROM Client c
           WHERE c.user.id = :userId
             AND c.isDeleted = false
             AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%'))
             )
           """)
    Page<ClientProjection> searchProjectedByUserIdAndQuery(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );
}
